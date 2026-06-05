package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Stub mixin for MC 1.21.8 where the shared DebugRenderer#emitGizmos
 * injection target is not available with the expected signature.
 */
@Mixin(DebugRenderer.class)
public class MixinDebugRenderer {
}
