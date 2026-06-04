package top.tobyprime.mcedia.api.decoder;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.video.VideoFrame;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecoderProvidersTest {
    @Test
    void throwsWhenNoProviderMatches() {
        MediaPlayInfo playInfo = new MediaPlayInfo("https://example.com/video");
        DecoderConfiguration configuration = new DecoderConfiguration.Builder().build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DecoderProviders.find(playInfo, configuration, List.of()));
        assertTrue(ex.getMessage().contains("No decoder provider found"));
    }

    @Test
    void choosesHighestPriorityProvider() {
        MediaPlayInfo playInfo = new MediaPlayInfo("https://example.com/video");
        DecoderConfiguration configuration = new DecoderConfiguration.Builder().build();

        DecoderFactory lowFactory = (info, cfg) -> new NoopDecoder();
        DecoderFactory highFactory = (info, cfg) -> new NoopDecoder();
        DecoderProvider low = provider(true, 1, lowFactory);
        DecoderProvider high = provider(true, 10, highFactory);

        DecoderFactory selected = DecoderProviders.find(playInfo, configuration, List.of(low, high));
        assertSame(highFactory, selected);
    }

    @Test
    void throwsWhenHighestPriorityIsAmbiguous() {
        MediaPlayInfo playInfo = new MediaPlayInfo("https://example.com/video");
        DecoderConfiguration configuration = new DecoderConfiguration.Builder().build();

        DecoderProvider first = provider(true, 5, (info, cfg) -> new NoopDecoder());
        DecoderProvider second = provider(true, 5, (info, cfg) -> new NoopDecoder());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DecoderProviders.find(playInfo, configuration, List.of(first, second)));
        assertTrue(ex.getMessage().contains("Ambiguous decoder providers"));
    }

    private static DecoderProvider provider(boolean supported, int priority, DecoderFactory factory) {
        return new DecoderProvider() {
            @Override
            public boolean supports(MediaPlayInfo playInfo, DecoderConfiguration configuration) {
                return supported;
            }

            @Override
            public DecoderFactory getFactory() {
                return factory;
            }

            @Override
            public int getPriority() {
                return priority;
            }
        };
    }

    private static class NoopDecoder implements Decoder {
        @Override
        public void open() {
        }

        @Override
        public void close() {
        }

        @Override
        public top.tobyprime.mcedia.api.stream.FrameStream<VideoFrame> getVideoStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public top.tobyprime.mcedia.api.stream.FrameStream<AudioFrame> getAudioStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isEnded() {
            return false;
        }

        @Override
        public long getDuration() {
            return 0;
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
        public boolean isLowOverhead() {
            return false;
        }

        @Override
        public void setRuntimeVideoEnabled(boolean enabled) {
        }

        @Override
        public void setRuntimeAudioEnabled(boolean enabled) {
        }
    }
}
