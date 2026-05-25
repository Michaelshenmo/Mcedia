package top.tobyprime.mcedia_core.client.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia_core.client.mixin.accessor.OptionsSubScreenAccessor;

@Mixin(SoundOptionsScreen.class)
public class MixinSoundOptionsScreen {

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void onAddOptions(CallbackInfo ci) {
        OptionInstance<Double> videoVolume = new OptionInstance<>(
                "mcedia_core.options.video_volume",
                OptionInstance.noTooltip(),
                (caption, value) -> value == 0.0
                        ? Component.translatable("options.generic_value", caption, CommonComponents.OPTION_OFF)
                        : Component.translatable("options.percent_value", caption, (int) Math.round(value * 100.0)),
                OptionInstance.UnitDouble.INSTANCE,
                (double) Math.min(Configs.VOLUME_FACTOR, 1.0F),
                value -> Configs.VOLUME_FACTOR = value.floatValue()
        );
        ((OptionsSubScreenAccessor) this).mcedia$getList().addBig(videoVolume);
    }
}
