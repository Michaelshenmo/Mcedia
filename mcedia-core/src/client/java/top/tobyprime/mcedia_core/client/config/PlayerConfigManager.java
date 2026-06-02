package top.tobyprime.mcedia_core.client.config;

import net.fabricmc.loader.api.FabricLoader;
import top.tobyprime.mcedia.api.resolver.MediaResolverSettings;
import top.tobyprime.mcedia.player.config.Configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PlayerConfigManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mcedia_core.properties");

    private PlayerConfigManager() {
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                var props = new Properties();
                try (var input = Files.newInputStream(CONFIG_PATH)) {
                    props.load(input);
                }
                Configs.fromProperties(props);
            }
        } catch (IOException e) {
            // Config file not readable -- continue with defaults
        }

        MediaResolverSettings.setResolutionLimit(
                Configs.MAX_RESOLUTION_HEIGHT
        );
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            var props = new Properties();
            Configs.writeToProperties(props);
            try (var output = Files.newOutputStream(CONFIG_PATH)) {
                props.store(output, "Mcedia Player configuration");
            }
        } catch (IOException e) {
            // Best-effort save
        }
    }
}
