package top.tobyprime.mcedia.player.media_resolvers.direct;

import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

public class DirectMedia implements Media {
    MediaPlayInfo mediaPlayInfo;
    MediaInfo mediaInfo;
    public DirectMedia(MediaPlayInfo mediaPlayInfo, MediaInfo mediaInfo) {
        this.mediaPlayInfo = mediaPlayInfo;
        this.mediaInfo = mediaInfo;
    }

    @Override
    public MediaPlayInfo getPlayInfo() {
        return mediaPlayInfo;
    }

    @Override
    public MediaInfo getInfo() {
        return mediaInfo;
    }
}
