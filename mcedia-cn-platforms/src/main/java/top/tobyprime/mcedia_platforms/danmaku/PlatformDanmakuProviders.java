package top.tobyprime.mcedia_platforms.danmaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.danmaku.DanmakuProviders;
import top.tobyprime.mcedia_platforms.danmaku.bilibili.BilibiliDanmakuProvider;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PlatformDanmakuProviders {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformDanmakuProviders.class);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private PlatformDanmakuProviders() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        DanmakuProviders.register("bilibili", new BilibiliDanmakuProvider());
        LOGGER.info("Registered platform danmaku providers: bilibili");
    }
}
