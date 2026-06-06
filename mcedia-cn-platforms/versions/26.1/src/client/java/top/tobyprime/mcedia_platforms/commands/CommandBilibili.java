package top.tobyprime.mcedia_platforms.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 26.1 fallback: shared Fabric client command API is not available on this line.
 */
public final class CommandBilibili {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBilibili.class);

    private CommandBilibili() {
    }

    public static void register(Object ignoredDispatcher) {
        LOGGER.info("Skip Bilibili client command registration on MC 26.1");
    }
}
