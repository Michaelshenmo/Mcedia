package top.tobyprime.mcedia.player.internal;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.decoder.Decoder;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.api.video.VideoFrame;
import top.tobyprime.mcedia.player.internal.processors.AudioProcessor;
import top.tobyprime.mcedia.player.internal.processors.VideoProcessor;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaPlayImplTest {
    @Test
    void getEstimatedTimeReturnsPlayClockTime() {
        var media = new TestMedia();
        var player = new MediaPlayImpl(media, new AudioProcessor(), new VideoProcessor());

        player.seek(1_250_000L);

        assertEquals(1_250_000L, player.getEstimatedTime());
    }

    @Test
    void isEndedUsesActualPlaybackTimeInsteadOfEstimatedTime() throws Exception {
        var audioProcessor = new StubAudioProcessor();
        var player = new MediaPlayImpl(new TestMedia(), audioProcessor, new VideoProcessor());
        player.decoder = new StubDecoder();
        setLong(player, "duration", 1_000_000L);
        player.seek(900_000L);
        player.setSpeed(2.0D);
        audioProcessor.playtime = 950_000L;
        audioProcessor.ended = true;

        assertFalse(player.isEnded());
    }

    @Test
    void suspendMarksPlayAsNotEnded() {
        var player = new MediaPlayImpl(new TestMedia(), new AudioProcessor(), new VideoProcessor());
        player.decoder = new StubDecoder();

        player.suspendDecoder();

        assertTrue(player.isDecoderSuspended());
        assertFalse(player.isEnded());
    }

    @Test
    void openRetriesUntilThirdAttemptSucceeds() {
        var player = new RetryableMediaPlayImpl(
                new TestMedia(),
                new AudioProcessor(),
                new VideoProcessor(),
                new FlakyDecoder(),
                new FlakyDecoder(),
                new StubDecoder()
        );

        player.open(new top.tobyprime.mcedia.api.config.DecoderConfiguration.Builder().build());

        assertEquals(3, player.createCount);
        assertFalse(player.isDecoderSuspended());
    }

    @Test
    void openThrowsLastFailureAfterThreeAttempts() {
        var first = new FlakyDecoder();
        var second = new FlakyDecoder();
        var third = new FlakyDecoder();
        var player = new RetryableMediaPlayImpl(
                new TestMedia(),
                new AudioProcessor(),
                new VideoProcessor(),
                first,
                second,
                third
        );

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> player.open(new top.tobyprime.mcedia.api.config.DecoderConfiguration.Builder().build()));

        assertEquals("open failed", error.getMessage());
        assertEquals(3, player.createCount);
        assertTrue(first.closed);
        assertTrue(second.closed);
        assertTrue(third.closed);
    }

    private static void setLong(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(target, value);
    }

    private static final class StubAudioProcessor extends AudioProcessor {
        private long playtime = -1L;
        private boolean ended;

        @Override
        public long getPlaytime() {
            return playtime;
        }

        @Override
        public boolean isEnded() {
            return ended;
        }
    }

    private static final class RetryableMediaPlayImpl extends MediaPlayImpl {
        private final ArrayDeque<Decoder> decoders = new ArrayDeque<>();
        private int createCount;

        private RetryableMediaPlayImpl(Media media, AudioProcessor audioProcessor, VideoProcessor videoProcessor, Decoder... decoders) {
            super(media, audioProcessor, videoProcessor);
            for (var decoder : decoders) {
                this.decoders.addLast(decoder);
            }
        }

        @Override
        protected Decoder createDecoder(top.tobyprime.mcedia.api.config.DecoderConfiguration decoderConfiguration) {
            createCount++;
            Decoder decoder = decoders.pollFirst();
            if (decoder == null) {
                throw new IllegalStateException("missing decoder for test");
            }
            return decoder;
        }
    }

    private static class StubDecoder implements Decoder {
        private final FrameStream<VideoFrame> videoStream = new FrameStream<>(1);
        private final FrameStream<AudioFrame> audioStream = new FrameStream<>(1);
        boolean closed;

        @Override
        public void open() {
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public FrameStream<VideoFrame> getVideoStream() {
            return videoStream;
        }

        @Override
        public FrameStream<AudioFrame> getAudioStream() {
            return audioStream;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isEnded() {
            return true;
        }

        @Override
        public long getDuration() {
            return 1_000_000L;
        }

        @Override
        public long getTime() {
            return 0;
        }

        @Override
        public void seek(long time) {
        }

        @Override
        public void setLowOverhead(boolean lowOverhead) {
        }

        @Override
        public void setRuntimeVideoEnabled(boolean enabled) {
        }

        @Override
        public void setRuntimeAudioEnabled(boolean enabled) {
        }

        @Override
        public boolean isLowOverhead() {
            return false;
        }
    }

    private static final class FlakyDecoder extends StubDecoder {
        @Override
        public void open() {
            throw new RuntimeException("open failed");
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
