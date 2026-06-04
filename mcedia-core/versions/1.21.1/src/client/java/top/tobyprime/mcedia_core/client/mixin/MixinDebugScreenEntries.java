package top.tobyprime.mcedia_core.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.components.DebugScreenOverlay.class)
public class MixinDebugScreenEntries {
    @Inject(method = "getOverlayDebugLines", at = @At("TAIL"))
    private void mcedia$registerDebugEntry(CallbackInfo ci) {
    }
}