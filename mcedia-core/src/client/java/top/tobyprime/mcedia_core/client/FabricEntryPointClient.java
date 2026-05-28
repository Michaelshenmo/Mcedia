package top.tobyprime.mcedia_core.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import top.tobyprime.mcedia.player.media_resolvers.direct.DirectMediaResolverBootstrap;
import top.tobyprime.mcedia_core.client.command.ClientCommands;
import top.tobyprime.mcedia_core.client.compat.SoundPhysicsCompat;
import top.tobyprime.mcedia_core.client.config.PlayerConfigManager;
import top.tobyprime.mcedia_core.client.events.MinecraftClientEvents;
import top.tobyprime.mcedia_core.client.player.MediaPlayerHostManager;


public class FabricEntryPointClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DirectMediaResolverBootstrap.init();
        MinecraftClientEvents.init();
        ClientCommands.register();
        MinecraftClientEvents.onRenderTick.addHandler((ignored) -> MediaPlayerHostManager.get().tickVideo());
        MinecraftClientEvents.onAudioTick.addHandler((ignored) -> MediaPlayerHostManager.get().tickAudio());

        SoundPhysicsCompat.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PlayerConfigManager.load());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlayerConfigManager.save());
    }
}
