package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.renderer.McediaRenderer;

@Mixin(DebugRenderer.class)
public class MixinDebugRenderer {
    @Inject(method = "emitGizmos", at = @At("HEAD"))
    private void mcedia$captureCurrentFrustum(Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick, CallbackInfo ci) {
        McediaRenderer.get().setCurrentFrustum(frustum);
    }
}
