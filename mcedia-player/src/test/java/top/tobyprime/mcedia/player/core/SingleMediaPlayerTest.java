package top.tobyprime.mcedia.player.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia.player.internal.MediaPlayImpl;
import top.tobyprime.mcedia.player.internal.processors.AudioProcessor;
import top.tobyprime.mcedia.player.internal.processors.VideoProcessor;
import top.tobyprime.mcedia.player.runtime.McediaExecutors;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Timeout(value = 30, unit = TimeUnit.SECONDS) // 整体测试类超时兜底
class SingleMediaPlayerTest {
    @Test
    void seekAsyncReturnsBeforeSeekFinishes() throws Exception {
        var player = new SingleMediaPlayer();
        var enterLatch = new CountDownLatch(1);
        var releaseLatch = new CountDownLatch(1);
        var mediaPlay = new BlockingMediaPlay(enterLatch, releaseLatch);
        setField(player, "mediaPlay", mediaPlay);

        var start = System.nanoTime();
        var future = player.seekAsync(1_000L);
        var elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis < 100);
        assertTrue(enterLatch.await(1, TimeUnit.SECONDS));
        assertFalse(future.isDone());

        releaseLatch.countDown();
        future.get(1, TimeUnit.SECONDS);
        assertEquals(1_000L, mediaPlay.getLastSeek());
    }

    @Test
    void tickMethodsDoNotWaitWhileSeekIsRunning() throws Exception {
        var player = new SingleMediaPlayer();
        var enterLatch = new CountDownLatch(1);
        var releaseLatch = new CountDownLatch(1);
        var mediaPlay = new BlockingMediaPlay(enterLatch, releaseLatch);
        setField(player, "mediaPlay", mediaPlay);

        var future = player.seekAsync(1_000L);
        assertTrue(enterLatch.await(1, TimeUnit.SECONDS));

        var start = System.nanoTime();
        player.tickAudio();
        player.tickVideo();
        var elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis < 100);

        releaseLatch.countDown();
        future.get(1, TimeUnit.SECONDS);
    }

    @Test
    void seekAsyncSkipsQueuedStaleSeekRequests() throws Exception {
        var player = new SingleMediaPlayer();
        var mediaPlay = new RecordingMediaPlay();
        setField(player, "mediaPlay", mediaPlay);

        var started = new CountDownLatch(4);
        var release = new CountDownLatch(1);
        for (int i = 0; i < 4; i++) {
            McediaExecutors.ioExecutor.execute(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(started.await(1, TimeUnit.SECONDS));

        var first = player.seekAsync(1_000L);
        var second = player.seekAsync(2_000L);

        release.countDown();
        second.get(2, TimeUnit.SECONDS);
        first.get(2, TimeUnit.SECONDS);

        assertEquals(1, mediaPlay.seekCount.get());
        assertEquals(2_000L, mediaPlay.lastSeek.get());
    }

    @Test
    void seekAsyncWaitsForLoadToFinish() throws Exception {
        var player = new SingleMediaPlayer();
        var mediaPlay = new RecordingMediaPlay();
        var pendingLoad = new CompletableFuture<top.tobyprime.mcedia.api.player.MediaPlay>();
        setField(player, "mediaPlay", mediaPlay);
        setAtomicReferenceField(player, "loadFuture", pendingLoad);

        var future = player.seekAsync(3_000L);
        assertFalse(future.isDone());
        assertEquals(0, mediaPlay.seekCount.get());

        pendingLoad.complete(mediaPlay);
        future.get(1, TimeUnit.SECONDS);

        assertEquals(1, mediaPlay.seekCount.get());
        assertEquals(3_000L, mediaPlay.lastSeek.get());
    }

    /// 核心测试: 验证 close() 在加载过程中被调用时能正确取消 pending load
    ///
    /// 场景复现: IO 线程正在执行 mediaSupplier.get() (模拟 Bilibili API 请求),
    /// close() 被调用但未取消 pending load → supplier 完成后 thenApply 回调
    /// 创建孤儿 MediaPlayImpl 并调用 play.open() → decoder 尝试实际 I/O 操作 → 渲染线程卡死
    ///
    /// 测试通过条件: close() 后 pending load 被取消，playAsync 返回的 future
    /// 在 2 秒内完成（被取消或异常完成），不触发实际的 decoder 打开。
    /// 若 future 未在超时内完成，说明 decoder 的 I/O 阻塞了 IO 线程 → 复现了管线卡死。
    @Test
    void closeDuringLoadPreventsOrphanedMediaPlay() throws Exception {
        var player = new SingleMediaPlayer();
        var supplierEntered = new CountDownLatch(1);
        var releaseSupplier = new CountDownLatch(1);

        var future = player.playAsync(() -> {
            supplierEntered.countDown();
            try {
                releaseSupplier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new TestMedia();
        });

        // 等待 supplier 启动（加载进行中）
        assertTrue(supplierEntered.await(5, TimeUnit.SECONDS));
        assertEquals(PlaybackState.LOADING, player.getPlaybackState());

        // 在加载进行中调用 close()（模拟卸载 host）
        player.close();

        // 释放 supplier
        releaseSupplier.countDown();

        // 验证: 在超时内 future 应完成，而非卡死在 decoder.open()
        try {
            future.get(5, TimeUnit.SECONDS);
            // future 正常完成 → 可能没有 decoder 或解析器，close 后不应正常完成
            fail("playAsync future should not complete normally after close()");
        } catch (java.util.concurrent.TimeoutException e) {
            // future 未在超时内完成 → decoder.open() 阻塞了 IO 线程
            player.close();
            throw new AssertionError(
                    "BUG REPRODUCED: close() did not prevent pending load from blocking. " +
                    "Orphaned MediaPlayImpl.open() hangs IO executor -> render thread freeze.", e);
        } catch (java.util.concurrent.CancellationException e) {
            // ✅ 正确路径: close() 取消了 pending load，Stale 检查失效了 thenApply
        } catch (java.util.concurrent.ExecutionException e) {
            // ✅ 正确路径: close() 取消了 pending load，thenApply 未执行，future 异常完成
        }

        // 验证: player 回退到 IDLE 状态，无孤儿 play
        assertEquals(PlaybackState.IDLE, player.getPlaybackState(),
                "player should be IDLE after close()");
        assertNull(player.getMedia(),
                "no orphaned MediaPlayImpl should exist after close()");
    }

    /// 验证 close() 后 tickVideo/tickAudio 不因残留 load 而阻塞
    @Test
    void tickMethodsDoNotBlockAfterCloseDuringLoad() throws Exception {
        var player = new SingleMediaPlayer();
        var supplierEntered = new CountDownLatch(1);
        var releaseSupplier = new CountDownLatch(1);

        player.playAsync(() -> {
            supplierEntered.countDown();
            try {
                releaseSupplier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new TestMedia();
        });

        assertTrue(supplierEntered.await(5, TimeUnit.SECONDS));
        player.close();

        // tickVideo 使用 tryLock，不应阻塞
        var start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            player.tickVideo();
            player.tickAudio();
        }
        var elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsed < 1000, "tick methods blocked: " + elapsed + "ms");

        // 清理: 让 supplier 完成，避免影响后续测试
        releaseSupplier.countDown();
    }

    /// 验证连续 close() 后内部状态干净，不残留前一次的 load 上下文
    @Test
    void closeThenPlayAsyncIsNotContaminatedByPreviousStaleLoad() throws Exception {
        var player = new SingleMediaPlayer();
        var enterSupplier1 = new CountDownLatch(1);
        var releaseSupplier1 = new CountDownLatch(1);

        // 第一次 playAsync 被卡住
        var future1 = player.playAsync(() -> {
            enterSupplier1.countDown();
            try {
                releaseSupplier1.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new TestMedia();
        });
        assertTrue(enterSupplier1.await(5, TimeUnit.SECONDS));

        // close() 后释放 supplier
        player.close();
        releaseSupplier1.countDown();

        // 第一次 playAsync 应被取消
        try {
            future1.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            player.close();
            throw new AssertionError(
                    "BUG REPRODUCED: first playAsync blocked after close().", e);
        } catch (java.util.concurrent.CancellationException | java.util.concurrent.ExecutionException ignored) {
            // ✅ 正确: close() 取消了 pending load
        }

        // 验证 player 回到初始状态
        assertEquals(PlaybackState.IDLE, player.getPlaybackState(),
                "player should be IDLE after close()");
        assertNull(player.getMedia(),
                "no orphaned media after close()");

        // 验证 loadRequestId 已递增，不会被前一次 thenApply 残留覆盖
        // 如果 loadRequestId 未递增，外部代码再次调用 playAsync 时
        // 其 thenApply 中的 stale 检查可能使用已被 close 污染的 ID
        // （通过反射检查内部状态保持一致）
        var loadFutureAfterClose = getAtomicReference(player, "loadFuture");
        assertTrue(loadFutureAfterClose == null || loadFutureAfterClose.isDone(),
                "loadFuture should be null or done after close()");
    }
    }

    @Test
    void runtimeStateIsPropagatedToCurrentMediaPlay() throws Exception {
        var player = new SingleMediaPlayer();
        var mediaPlay = new RecordingMediaPlay();
        setField(player, "mediaPlay", mediaPlay);

        player.setRuntimeVideoEnabled(false);
        player.setRuntimeAudioEnabled(false);

        waitUntil(() -> !mediaPlay.runtimeVideoEnabled && !mediaPlay.runtimeAudioEnabled);
        assertFalse(mediaPlay.runtimeVideoEnabled);
        assertFalse(mediaPlay.runtimeAudioEnabled);
    }

    @Test
    void suspendAndResumeDecoderArePropagatedToCurrentMediaPlay() throws Exception {
        var player = new SingleMediaPlayer();
        var mediaPlay = new RecordingMediaPlay();
        setField(player, "mediaPlay", mediaPlay);

        player.suspendDecoder();
        waitUntil(() -> mediaPlay.suspendCount.get() == 1);
        assertTrue(player.isDecoderSuspended());

        player.resumeDecoder();
        waitUntil(() -> mediaPlay.resumeCount.get() == 1);
        assertFalse(player.isDecoderSuspended());
    }

    @Test
    void repeatedSuspendAndResumeOnlyNeedSingleEffectiveTransition() throws Exception {
        var player = new SingleMediaPlayer();
        var mediaPlay = new RecordingMediaPlay();
        setField(player, "mediaPlay", mediaPlay);

        player.suspendDecoder();
        player.suspendDecoder();
        player.suspendDecoder();
        waitUntil(() -> mediaPlay.suspendCount.get() >= 1);
        assertEquals(1, mediaPlay.suspendCount.get());

        player.resumeDecoder();
        player.resumeDecoder();
        waitUntil(() -> mediaPlay.resumeCount.get() >= 1);
        assertEquals(1, mediaPlay.resumeCount.get());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void waitUntil(Check check) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("condition not met before timeout");
    }

    @SuppressWarnings("unchecked")
    private static <T> void setAtomicReferenceField(Object target, String name, T value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicReference<T>) field.get(target)).set(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getAtomicReference(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return ((java.util.concurrent.atomic.AtomicReference<T>) field.get(target)).get();
    }

    private interface Check {
        boolean ok();
    }

    private static class RecordingMediaPlay extends MediaPlayImpl {
        final AtomicInteger seekCount = new AtomicInteger();
        final AtomicLong lastSeek = new AtomicLong(Long.MIN_VALUE);
        final AtomicInteger suspendCount = new AtomicInteger();
        final AtomicInteger resumeCount = new AtomicInteger();
        boolean runtimeVideoEnabled = true;
        boolean runtimeAudioEnabled = true;
        boolean decoderSuspended;

        private RecordingMediaPlay() {
            super(new TestMedia(), new AudioProcessor(), new VideoProcessor());
        }

        @Override
        public void seek(long time) {
            seekCount.incrementAndGet();
            lastSeek.set(time);
        }

        @Override
        public void setRuntimeVideoEnabled(boolean enabled) {
            runtimeVideoEnabled = enabled;
        }

        @Override
        public void setRuntimeAudioEnabled(boolean enabled) {
            runtimeAudioEnabled = enabled;
        }

        @Override
        public void suspendDecoder() {
            suspendCount.incrementAndGet();
            decoderSuspended = true;
        }

        @Override
        public void resumeDecoder() {
            resumeCount.incrementAndGet();
            decoderSuspended = false;
        }

        @Override
        public boolean isDecoderSuspended() {
            return decoderSuspended;
        }

        long getLastSeek() {
            return lastSeek.get();
        }
    }

    private static final class BlockingMediaPlay extends RecordingMediaPlay {
        private final CountDownLatch enterLatch;
        private final CountDownLatch releaseLatch;

        private BlockingMediaPlay(CountDownLatch enterLatch, CountDownLatch releaseLatch) {
            this.enterLatch = enterLatch;
            this.releaseLatch = releaseLatch;
        }

        @Override
        public void seek(long time) {
            enterLatch.countDown();
            try {
                releaseLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            super.seek(time);
        }
    }

    private static final class TestMedia implements Media {
        private final MediaPlayInfo playInfo = new MediaPlayInfo("test");

        @Override
        public MediaPlayInfo getPlayInfo() {
            return playInfo;
        }

        @Override
        public MediaInfo getInfo() {
            return new MediaInfo();
        }
    }
}
