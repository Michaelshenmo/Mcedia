package top.tobyprime.mcedia_core.client.mixin.accessor;

import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {
    @Accessor("channelAccess")
    ChannelAccess mcedia$getChannelAccess();

    @Accessor("executor")
    SoundEngineExecutor mcedia$getExecutor();

    @Accessor("loaded")
    boolean mcedia$isLoaded();
}
