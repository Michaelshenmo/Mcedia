package top.tobyprime.mcedia_core.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Stub for MC 1.21.9 which lacks the DebugScreenEntries / DebugScreenProfile classes.
 * The debug entry is registered via a different mechanism by DebugEntryMMDecoderMetrics.
 */
@Mixin(net.minecraft.client.Minecraft.class)
public class MixinDebugScreenEntries {
}