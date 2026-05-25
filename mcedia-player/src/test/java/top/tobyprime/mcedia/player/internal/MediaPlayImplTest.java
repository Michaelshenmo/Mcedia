package top.tobyprime.mcedia.player.internal;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.player.internal.processors.AudioProcessor;
import top.tobyprime.mcedia.player.internal.processors.VideoProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaPlayImplTest {
    @Test
    void getEstimatedTimeReturnsPlayClockTime() {
        var media = new TestMedia();
        var player = new MediaPlayImpl(media, new AudioProcessor(), new VideoProcessor());

        player.seek(1_250_000L);

        assertEquals(1_250_000L, player.getEstimatedTime());
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
