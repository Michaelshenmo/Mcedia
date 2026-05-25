package top.tobyprime.mcedia.player.media_resolvers.direct;

import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.resolver.MediaResolvers;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DirectMediaResolverBootstrap {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private DirectMediaResolverBootstrap() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        MediaResolvers.register("direct", target -> new DirectMedia(
                new MediaPlayInfo(target),
                new MediaInfo("Direct Media", "Unknown", null, "direct")
        ));
        MediaResolvers.registerParser(new DirectUrlParser(), 100);
    }
}
