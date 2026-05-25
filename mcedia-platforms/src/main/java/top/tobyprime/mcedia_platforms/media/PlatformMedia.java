package top.tobyprime.mcedia_platforms.media;

import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

public final class PlatformMedia implements Media {
    private final MediaPlayInfo playInfo;
    private final MediaInfo info;

    public PlatformMedia(MediaPlayInfo playInfo, MediaInfo info) {
        this.playInfo = playInfo;
        this.info = info;
    }

    @Override
    public MediaPlayInfo getPlayInfo() {
        return playInfo;
    }

    @Override
    public MediaInfo getInfo() {
        return info;
    }
}
