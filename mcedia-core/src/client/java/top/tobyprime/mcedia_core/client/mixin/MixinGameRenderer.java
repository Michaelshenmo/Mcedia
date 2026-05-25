package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.events.MinecraftClientEvents;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void render(DeltaTracker deltaTracker, CallbackInfo ci) {
        MinecraftClientEvents.onRenderTick.handle(null);
    }
}
