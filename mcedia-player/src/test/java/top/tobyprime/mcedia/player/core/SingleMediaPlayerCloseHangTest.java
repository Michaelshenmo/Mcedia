package top.tobyprime.mcedia.player.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * close() 卡住问题的复现测试。
 *
 * 与常规测试分开存放，因为这些测试会复现 close() 在 lock.lock() 上的死锁，
 * 常规 JUnit @Timeout 无法中断被锁阻塞的线程，因此独立测试可避免阻塞其他测试套件。
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class SingleMediaPlayerCloseHangTest {

    // ============================================================
    // 场景 1: seek 持有 SingleMediaPlayer.lock 且 target.seek() 阻塞
    // → close() 在 lock.lock() 处卡住
    // ============================================================

    /// BUG 复现测试: close() 在 seek 持有锁并阻塞时卡住调用线程
    ///
    /// IO 线程在 seekCurrentMedia() 中持有 SingleMediaPlayer.lock，
    /// 同时调用 target.seek(time) 并阻塞。调用线程的 close()
    /// 需要获取锁来设置 mediaPlay=null，但 lock.lock() 会无限等待。
    ///
    /// BUG: close() 使用 lock.lock() 阻塞获取锁，但锁被 IO 线程持有且
    /// IO 线程在 decoder.seek() 中阻塞（网络 I/O、文件 I/O 等）。
    /// 这使得 close() 所在调用线程也被阻塞。
    @Test
    void closeReturnsPromptlyWhenTargetSeekBlocks() throws Exception {
        var player = new SingleMediaPlayer();
        var seekEnterLatch = new CountDownLatch(1);
        var seekReleaseLatch = new CountDownLatch(1);
        var mediaPlay = new BlockingSeekMediaPlay(seekEnterLatch, seekReleaseLatch);
        setField(player, "mediaPlay", mediaPlay);

        // 在 IO 线程上发起 seek，seekCurrentMedia 会持有 lock 并阻塞在 target.seek()
        var seekFuture = player.seekAsync(1_000L);
        assertTrue(seekEnterLatch.await(5, TimeUnit.SECONDS),
                "seek should have entered target.seek() and be holding the lock");

        // 调用 close() — 此时 lock 被 seek 持有的 IO 线程占用
        // BUG: 此处 lock.lock() 无限等待，导致 close() 卡住调用线程
        var closeStart = System.nanoTime();
        player.close();
        var closeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - closeStart);

        // 若执行至此，说明 BUG 已修复（close() 不再阻塞等待锁）
        assertTrue(closeElapsed < 1_000,
                "close() blocked for " + closeElapsed + "ms waiting for lock held by seek");

        seekReleaseLatch.countDown();
        seekFuture.get(2, TimeUnit.SECONDS);
    }

    // ============================================================
    // 辅助类
    // ============================================================

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

    private static final class BlockingSeekMediaPlay extends RecordingMediaPlay {
        private final CountDownLatch seekEnterLatch;
        private final CountDownLatch seekReleaseLatch;

        private BlockingSeekMediaPlay(CountDownLatch seekEnterLatch, CountDownLatch seekReleaseLatch) {
            this.seekEnterLatch = seekEnterLatch;
            this.seekReleaseLatch = seekReleaseLatch;
        }

        @Override
        public void seek(long time) {
            seekEnterLatch.countDown();
            try {
                seekReleaseLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            super.seek(time);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}