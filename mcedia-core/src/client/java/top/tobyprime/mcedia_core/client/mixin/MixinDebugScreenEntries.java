package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia_core.client.debug.DebugEntryMMDecoderMetrics;

import java.util.HashMap;
import java.util.Map;

@Mixin(DebugScreenEntries.class)
public class MixinDebugScreenEntries {
    @Shadow
    @Final
    private static Map<Identifier, DebugScreenEntry> ENTRIES_BY_ID;

    @Shadow
    @Mutable
    @Final
    public static Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void registerMcediaDebugEntry(CallbackInfo ci) {
        ENTRIES_BY_ID.put(DebugEntryMMDecoderMetrics.ENTRY_ID, new DebugEntryMMDecoderMetrics());

        Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> profiles = new HashMap<>(PROFILES);
        profiles.replaceAll((profile, statusMap) -> {
            Map<Identifier, DebugScreenEntryStatus> updated = new HashMap<>(statusMap);
            updated.put(DebugEntryMMDecoderMetrics.ENTRY_ID, DebugScreenEntryStatus.IN_OVERLAY);
            return Map.copyOf(updated);
        });
        PROFILES = Map.copyOf(profiles);
    }
}
