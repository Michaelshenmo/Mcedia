package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.debug.DebugEntryMMDecoderMetrics;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenEntries {
    @Inject(method = "getOverlayDebugLines", at = @At("TAIL"))
    private void mcedia$registerDebugEntry(CallbackInfo ci) {
    }
}