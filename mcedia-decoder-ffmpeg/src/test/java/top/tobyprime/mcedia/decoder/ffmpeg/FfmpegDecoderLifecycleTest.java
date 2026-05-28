package top.tobyprime.mcedia.decoder.ffmpeg;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.api.video.VideoFrame;
import top.tobyprime.mcedia.decoder.ffmpeg.internal.FfmpegProcessImageFlags;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegDecoderLifecycleTest {
    @Test
    void closeRemovesProcessImageFlagsForGrabbers() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        FFmpegFrameGrabber master = new NoopFrameGrabber();
        FFmpegFrameGrabber audio = new NoopFrameGrabber();

        setBoolean(decoder, "isOpen", true);
        setField(decoder, "masterGrabber", master);
        setField(decoder, "audioGrabber", audio);

        FfmpegProcessImageFlags.setProcessImage(master, false);
        FfmpegProcessImageFlags.setProcessImage(audio, false);

        decoder.close();

        assertTrue(FfmpegProcessImageFlags.isEnableProcessImage(master));
        assertTrue(FfmpegProcessImageFlags.isEnableProcessImage(audio));
        assertFalse(decoder.isOpen());
        assertTrue(decoder.isEnded());
        assertNull(getField(decoder, "masterGrabber"));
        assertNull(getField(decoder, "audioGrabber"));
    }

    @Test
    void closeClosesQueuedVideoAndAudioFramesWhenDecoderWasOpened() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        TrackingVideoFrame videoFrame = new TrackingVideoFrame(10);
        TrackingAudioFrame audioFrame = new TrackingAudioFrame(20);
        FrameStream<VideoFrame> videoStream = decoder.getVideoStream();
        FrameStream<AudioFrame> audioStream = decoder.getAudioStream();
        videoStream.put(videoFrame);
        audioStream.put(audioFrame);

        setBoolean(decoder, "isOpen", true);
        setField(decoder, "masterGrabber", new NoopFrameGrabber());
        setField(decoder, "audioGrabber", new NoopFrameGrabber());

        decoder.close();

        assertEquals(1, videoFrame.closeCount.get());
        assertEquals(1, audioFrame.closeCount.get());
        assertEquals(0, videoStream.size());
        assertEquals(0, audioStream.size());
        assertTrue(isStreamClosed(videoStream));
        assertTrue(isStreamClosed(audioStream));
    }

    @Test
    void closeWithoutResourcesDoesNotCloseStreams() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());

        decoder.close();

        FrameStream<?> videoStream = (FrameStream<?>) getField(decoder, "videoStream");
        FrameStream<?> audioStream = (FrameStream<?>) getField(decoder, "audioStream");

        assertFalse(isStreamClosed(videoStream));
        assertFalse(isStreamClosed(audioStream));
    }

    @Test
    void audioDecodeLoopReturnsWithoutLeakingReadLockWhenAudioGrabberIsNull() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());

        invokePrivate(decoder, "audioDecodeLoop");

        var lock = (java.util.concurrent.locks.ReentrantReadWriteLock) getField(decoder, "audioGrabberLock");
        boolean acquired = lock.writeLock().tryLock();
        try {
            assertTrue(acquired);
        } finally {
            if (acquired) {
                lock.writeLock().unlock();
            }
        }
    }

    @Test
    void updateEndedStateSetsEndedOnlyWhenAllThreadsStopped() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());

        setBoolean(decoder, "isEnded", false);
        setField(decoder, "masterDecoderThread", new Thread(() -> { }));
        setField(decoder, "audioDecodeThread", null);
        invokePrivate(decoder, "updateEndedStateIfStopped");
        assertFalse(decoder.isEnded());

        setField(decoder, "masterDecoderThread", null);
        setField(decoder, "audioDecodeThread", null);
        invokePrivate(decoder, "updateEndedStateIfStopped");
        assertTrue(decoder.isEnded());
    }

    @Test
    void setLowOverheadFalseClearsQueuedFramesOnTransition() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        TrackingVideoFrame videoFrame = new TrackingVideoFrame(10);
        TrackingAudioFrame audioFrame = new TrackingAudioFrame(20);
        decoder.getVideoStream().put(videoFrame);
        decoder.getAudioStream().put(audioFrame);

        decoder.setLowOverhead(true);
        decoder.setLowOverhead(false);

        assertEquals(1, videoFrame.closeCount.get());
        assertEquals(1, audioFrame.closeCount.get());
        assertEquals(0, decoder.getVideoStream().size());
        assertEquals(0, decoder.getAudioStream().size());
    }

    @Test
    void seekClearsQueuedFramesBeforeRestart() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        TrackingVideoFrame videoFrame = new TrackingVideoFrame(10);
        TrackingAudioFrame audioFrame = new TrackingAudioFrame(20);
        RecordingFrameGrabber master = new RecordingFrameGrabber(1_000L);
        RecordingFrameGrabber audio = new RecordingFrameGrabber(2_000L);
        decoder.getVideoStream().put(videoFrame);
        decoder.getAudioStream().put(audioFrame);

        setField(decoder, "masterGrabber", master);
        setField(decoder, "audioGrabber", audio);
        setField(decoder, "masterDecoderThread", new Thread(() -> { }));
        setField(decoder, "audioDecodeThread", new Thread(() -> { }));
        AtomicLong generation = (AtomicLong) getField(decoder, "decodeGeneration");
        long before = generation.get();

        decoder.seek(1_500L);

        assertEquals(1, videoFrame.closeCount.get());
        assertEquals(1, audioFrame.closeCount.get());
        assertEquals(0, decoder.getVideoStream().size());
        assertEquals(0, decoder.getAudioStream().size());
        assertEquals(1_000L, master.lastTimestamp);
        assertEquals(1_500L, audio.lastTimestamp);
        assertEquals(before + 1, generation.get());
    }

    @Test
    void runtimeDisableClearsQueuesAndUpdatesProcessImageFlag() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        TrackingVideoFrame videoFrame = new TrackingVideoFrame(10);
        TrackingAudioFrame audioFrame = new TrackingAudioFrame(20);
        FFmpegFrameGrabber master = new NoopFrameGrabber();
        decoder.getVideoStream().put(videoFrame);
        decoder.getAudioStream().put(audioFrame);
        setField(decoder, "masterGrabber", master);

        decoder.setRuntimeVideoEnabled(false);
        decoder.setRuntimeAudioEnabled(false);

        assertEquals(1, videoFrame.closeCount.get());
        assertEquals(1, audioFrame.closeCount.get());
        assertEquals(0, decoder.getVideoStream().size());
        assertEquals(0, decoder.getAudioStream().size());
        assertFalse(FfmpegProcessImageFlags.isEnableProcessImage(master));
    }

    @Test
    void closeInterruptsAndWaitsForDecoderThreadsBeforeClosingGrabbers() throws Exception {
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo("https://example.com/video"), new DecoderConfiguration.Builder().build());
        BlockingFrameGrabber master = new BlockingFrameGrabber();
        setBoolean(decoder, "isOpen", true);
        setField(decoder, "masterGrabber", master);

        CountDownLatch threadStarted = new CountDownLatch(1);
        CountDownLatch allowExit = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            threadStarted.countDown();
            try {
                allowExit.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                try {
                    setField(decoder, "masterDecoderThread", null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, "test-master-decoder");
        setField(decoder, "masterDecoderThread", worker);
        worker.start();
        assertTrue(threadStarted.await(1, TimeUnit.SECONDS));

        decoder.close();
        worker.join(1_000L);

        assertTrue(master.closeCalled.get());
        assertFalse(worker.isAlive());
        assertTrue(isStreamClosed(decoder.getVideoStream()));
        assertTrue(isStreamClosed(decoder.getAudioStream()));
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setBoolean(Object target, String name, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        AtomicBoolean flag = (AtomicBoolean) field.get(target);
        flag.set(value);
    }

    private static boolean isStreamClosed(FrameStream<?> stream) throws Exception {
        Field field = FrameStream.class.getDeclaredField("closed");
        field.setAccessible(true);
        AtomicBoolean closed = (AtomicBoolean) field.get(stream);
        return closed.get();
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static final class TrackingVideoFrame implements VideoFrame {
        private final long time;
        private final AtomicInteger closeCount = new AtomicInteger();

        private TrackingVideoFrame(long time) {
            this.time = time;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public int getWidth() {
            return 1;
        }

        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public ByteBuffer getBuffer() {
            return ByteBuffer.allocate(4);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class TrackingAudioFrame implements AudioFrame {
        private final long time;
        private final AtomicInteger closeCount = new AtomicInteger();

        private TrackingAudioFrame(long time) {
            this.time = time;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public int getSampleRate() {
            return 44100;
        }

        @Override
        public Buffer getBuffer(int channel) {
            return ByteBuffer.allocate(0);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static class NoopFrameGrabber extends FFmpegFrameGrabber {
        private NoopFrameGrabber() {
            super("test");
        }

        @Override
        public void close() throws FrameGrabber.Exception {
        }
    }

    private static final class RecordingFrameGrabber extends NoopFrameGrabber {
        private final long lengthInTime;
        private long lastTimestamp = Long.MIN_VALUE;

        private RecordingFrameGrabber(long lengthInTime) {
            this.lengthInTime = lengthInTime;
        }

        @Override
        public long getLengthInTime() {
            return lengthInTime;
        }

        @Override
        public void setTimestamp(long timestamp, boolean checkFrame) {
            lastTimestamp = timestamp;
        }
    }

    private static final class BlockingFrameGrabber extends NoopFrameGrabber {
        private final AtomicBoolean closeCalled = new AtomicBoolean(false);

        @Override
        public void close() throws FrameGrabber.Exception {
            closeCalled.set(true);
        }
    }
}
