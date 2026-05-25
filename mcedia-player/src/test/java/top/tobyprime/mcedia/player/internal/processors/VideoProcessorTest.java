package top.tobyprime.mcedia.player.internal.processors;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.api.video.MediaTexture;
import top.tobyprime.mcedia.api.video.VideoFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VideoProcessorTest {

    @Test
    void tickClosesFrameWhenTextureUploadThrows() throws Exception {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        FrameStream<VideoFrame> stream = new FrameStream<>(4);
        TrackingVideoFrame frame = new TrackingVideoFrame(0);
        stream.put(frame);

        processor.bindStream(stream);
        processor.bindTexture(new ThrowingMediaTexture());

        assertThrows(RuntimeException.class, () -> processor.tick(1));

        assertEquals(1, frame.closeCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void tickUploadsLatestEligibleFrameAndClosesSkippedFrames() throws Exception {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        FrameStream<VideoFrame> stream = new FrameStream<>(8);
        TrackingVideoFrame first = new TrackingVideoFrame(10);
        TrackingVideoFrame second = new TrackingVideoFrame(20);
        TrackingVideoFrame third = new TrackingVideoFrame(30);
        RecordingMediaTexture texture = new RecordingMediaTexture();
        stream.put(first);
        stream.put(second);
        stream.put(third);

        processor.bindStream(stream);
        processor.bindTexture(texture);
        processor.tick(31);

        assertEquals(1, texture.tickCount.get());
        assertEquals(1, texture.uploadCount.get());
        assertSame(third, texture.lastUploadedFrame);
        assertEquals(1, first.closeCount.get());
        assertEquals(1, second.closeCount.get());
        assertEquals(1, third.closeCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void tickWithoutTextureStillDrainsAndClosesEligibleFrames() throws Exception {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        FrameStream<VideoFrame> stream = new FrameStream<>(8);
        TrackingVideoFrame first = new TrackingVideoFrame(10);
        TrackingVideoFrame second = new TrackingVideoFrame(20);
        TrackingVideoFrame future = new TrackingVideoFrame(100);
        stream.put(first);
        stream.put(second);
        stream.put(future);

        processor.bindStream(stream);
        processor.tick(50);

        assertEquals(1, first.closeCount.get());
        assertEquals(1, second.closeCount.get());
        assertEquals(0, future.closeCount.get());
        assertEquals(1, stream.size());
        assertSame(future, stream.peek());
    }

    @Test
    void tickLeavesFutureFrameQueued() throws Exception {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        FrameStream<VideoFrame> stream = new FrameStream<>(4);
        TrackingVideoFrame future = new TrackingVideoFrame(100);
        RecordingMediaTexture texture = new RecordingMediaTexture();
        stream.put(future);

        processor.bindStream(stream);
        processor.bindTexture(texture);
        processor.tick(50);

        assertEquals(1, texture.tickCount.get());
        assertEquals(0, texture.uploadCount.get());
        assertEquals(0, future.closeCount.get());
        assertEquals(1, stream.size());
        assertSame(future, stream.peek());
    }

    @Test
    void tickInvokesTextureTickBeforeUploadAttempt() throws Exception {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        FrameStream<VideoFrame> stream = new FrameStream<>(4);
        TrackingVideoFrame frame = new TrackingVideoFrame(0);
        RecordingMediaTexture texture = new RecordingMediaTexture();
        stream.put(frame);

        processor.bindStream(stream);
        processor.bindTexture(texture);
        processor.tick(1);

        assertEquals(List.of("tick:1", "upload:0"), texture.events);
        assertEquals(1, frame.closeCount.get());
    }

    @Test
    void tickWithNoStreamOnlyTicksTexture() {
        VideoProcessor processor = new VideoProcessor(Runnable::run);
        RecordingMediaTexture texture = new RecordingMediaTexture();

        processor.bindTexture(texture);
        processor.tick(42);

        assertEquals(1, texture.tickCount.get());
        assertEquals(0, texture.uploadCount.get());
        assertEquals(List.of("tick:42"), texture.events);
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

    private static class RecordingMediaTexture implements MediaTexture {
        private final AtomicInteger tickCount = new AtomicInteger();
        private final AtomicInteger uploadCount = new AtomicInteger();
        private final List<String> events = new ArrayList<>();
        private VideoFrame lastUploadedFrame;

        @Override
        public void upload(VideoFrame frame) {
            uploadCount.incrementAndGet();
            lastUploadedFrame = frame;
            events.add("upload:" + frame.getTime());
        }

        @Override
        public void tick(long time) {
            tickCount.incrementAndGet();
            events.add("tick:" + time);
        }
    }

    private static final class ThrowingMediaTexture extends RecordingMediaTexture {
        @Override
        public void upload(VideoFrame frame) {
            throw new RuntimeException("upload failed");
        }
    }
}
