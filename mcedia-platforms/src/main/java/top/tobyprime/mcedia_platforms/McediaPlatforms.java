package top.tobyprime.mcedia_platforms;

import net.fabricmc.api.ModInitializer;
import top.tobyprime.mcedia_platforms.danmaku.PlatformDanmakuProviders;
import top.tobyprime.mcedia_platforms.media.PlatformResolverBootstrap;

public final class McediaPlatforms implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformResolverBootstrap.init();
        PlatformDanmakuProviders.init();
    }
}
