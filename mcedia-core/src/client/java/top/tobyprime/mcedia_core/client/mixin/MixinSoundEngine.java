package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.events.MinecraftClientEvents;

@Mixin(SoundEngine.class)
public class MixinSoundEngine  {

    @Inject(method = "tick", at=@At("HEAD"))
    public void tick(CallbackInfo info) {
        MinecraftClientEvents.onAudioTick.handle(null);
    }

    @Inject(method = "tick", at=@At("TAIL"))
    public void postTick(CallbackInfo info) {
        MinecraftClientEvents.onPostAudioTick.handle(null);
    }
}
