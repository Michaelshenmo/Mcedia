package top.tobyprime.mcedia.player.core;

import org.junit.jupiter.api.Test;
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
        var pendingLoad = new java.util.concurrent.CompletableFuture<top.tobyprime.mcedia.api.player.MediaPlay>();
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

    @Test
    void closeCancelsPendingLoadBeforeMediaPlayIsInstalled() throws Exception {
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

        assertTrue(supplierEntered.await(1, TimeUnit.SECONDS));
        player.close();
        releaseSupplier.countDown();

        waitUntil(future::isDone);
        assertTrue(future.isCompletedExceptionally() || future.isCancelled());
        assertNull(player.getMedia());
        assertEquals(PlaybackState.IDLE, player.getPlaybackState());
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
