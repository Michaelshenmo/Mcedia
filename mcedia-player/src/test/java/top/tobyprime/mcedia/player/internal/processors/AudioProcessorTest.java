package top.tobyprime.mcedia.player.internal.processors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.audio.AudioSource;
import top.tobyprime.mcedia.api.stream.FrameStream;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioProcessorTest {

    @Test
    void tickClosesFrameWhenUploadThrows() throws Exception {
        AudioProcessor processor = new AudioProcessor(Runnable::run);
        FrameStream<AudioFrame> stream = new FrameStream<>(4);
        TrackingAudioFrame frame = new TrackingAudioFrame();
        stream.put(frame);

        processor.bindStream(stream);
        processor.bindAudioSources(new ThrowingAudioSource());

        assertThrows(RuntimeException.class, () -> processor.tick(0));

        assertEquals(1, frame.closeCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void tickClosesFrameAfterUploadingToAllSources() throws Exception {
        AudioProcessor processor = new AudioProcessor(Runnable::run);
        FrameStream<AudioFrame> stream = new FrameStream<>(4);
        TrackingAudioFrame frame = new TrackingAudioFrame();
        CountingAudioSource sourceA = new CountingAudioSource();
        CountingAudioSource sourceB = new CountingAudioSource();
        stream.put(frame);

        processor.bindStream(stream);
        processor.bindAudioSources(sourceA);
        processor.bindAudioSources(sourceB);

        processor.tick(0);

        assertEquals(1, sourceA.uploadCount.get());
        assertEquals(1, sourceB.uploadCount.get());
        assertEquals(1, frame.closeCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void getPlaytimeReturnsCachedValueWithoutTouchingAudioSources() throws Exception {
        AudioProcessor processor = new AudioProcessor(Runnable::run);
        FrameStream<AudioFrame> stream = new FrameStream<>(4);
        processor.bindStream(stream);
        processor.bindAudioSources(new ThrowingPlaytimeAudioSource());
        setLong(processor, "playtime", 123_000L);

        assertEquals(123_000L, processor.getPlaytime());
    }

    @Test
    void tickDoesNotConsumeFramesWhilePaused() throws Exception {
        AudioProcessor processor = new AudioProcessor(Runnable::run);
        FrameStream<AudioFrame> stream = new FrameStream<>(4);
        TrackingAudioFrame frame = new TrackingAudioFrame();
        CountingAudioSource source = new CountingAudioSource();
        stream.put(frame);

        processor.bindStream(stream);
        processor.bindAudioSources(source);
        processor.pause();

        processor.tick(0);

        assertEquals(0, source.uploadCount.get());
        assertEquals(0, frame.closeCount.get());
        assertEquals(1, stream.size());
        assertEquals(1, source.pauseCount.get());
    }

    private static final class TrackingAudioFrame implements AudioFrame {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public long getTime() {
            return 0;
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

    private static class CountingAudioSource implements AudioSource {
        private final AtomicInteger uploadCount = new AtomicInteger();
        private final AtomicInteger pauseCount = new AtomicInteger();

        @Override
        public void tick() {
        }

        @Override
        public void upload(@NotNull AudioFrame frame) {
            uploadCount.incrementAndGet();
        }

        @Override
        public boolean getRequiredFrame() {
            return true;
        }

        @Override
        public long getPlaytime() {
            return 0;
        }

        @Override
        public void pause() {
            pauseCount.incrementAndGet();
        }

        @Override
        public void play() {
        }

        @Override
        public void setSpeed(double speed) {
        }

        @Override
        public void clear() {
        }

        @Override
        public boolean isEnded() {
            return false;
        }

        @Override
        public void setVolume(float gain) {
        }

        @Override
        public float getVolume() {
            return 1.0F;
        }
    }

    private static final class ThrowingAudioSource extends CountingAudioSource {
        @Override
        public void upload(@NotNull AudioFrame frame) {
            throw new RuntimeException("upload failed");
        }
    }

    private static final class ThrowingPlaytimeAudioSource extends CountingAudioSource {
        @Override
        public long getPlaytime() {
            throw new RuntimeException("getPlaytime should not be called");
        }
    }

    private static void setLong(Object target, String name, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(target, value);
    }
}
