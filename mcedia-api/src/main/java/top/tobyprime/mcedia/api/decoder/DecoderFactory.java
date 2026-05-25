package top.tobyprime.mcedia.api.decoder;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

public interface DecoderFactory {
    @NotNull Decoder create(@NotNull MediaPlayInfo playInfo, @NotNull DecoderConfiguration configuration);
}
