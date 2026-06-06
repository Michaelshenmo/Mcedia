package top.tobyprime.mcedia_platforms.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.21.9 fallback: Fabric client command API v2 unavailable on this line.
 */
public final class CommandBilibili {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBilibili.class);

    private CommandBilibili() {
    }

    public static void register(Object ignoredDispatcher) {
        LOGGER.info("Skip Bilibili client command registration on MC 1.21.9");
    }
}
