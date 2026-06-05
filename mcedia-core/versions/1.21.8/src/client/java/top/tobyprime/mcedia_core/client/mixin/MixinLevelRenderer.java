package top.tobyprime.mcedia_core.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Stub mixin for MC 1.21.8 which lack the SubmitNodeCollector / LevelRenderState API.
 * LevelRenderer.submitEntities with the modern renderer API is not available.
 * Screen rendering is handled by the 1.21.9+ version-specific code.
 */
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class MixinLevelRenderer {
}