package top.tobyprime.mcedia_core.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.debug.DebugRendererAudioHitbox;
import top.tobyprime.mcedia_core.client.player.MediaPlayerHostManager;
import top.tobyprime.mcedia_core.client.renderer.McediaRenderer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void mcedia$submitScreens(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        var frustum = McediaRenderer.get().getCurrentFrustum();
        MediaPlayerHostManager.get().tickVideo(frustum);
        McediaRenderer.get().submitScreens(poseStack, levelRenderState, submitNodeCollector);
        DebugRendererAudioHitbox.submit(
                poseStack,
                submitNodeCollector,
                levelRenderState.cameraRenderState,
                frustum
        );
    }
}
