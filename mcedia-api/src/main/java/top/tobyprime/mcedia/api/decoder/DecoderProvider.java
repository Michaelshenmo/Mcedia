package top.tobyprime.mcedia.api.decoder;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

public interface DecoderProvider {
    boolean supports(@NotNull MediaPlayInfo playInfo, @NotNull DecoderConfiguration configuration);

    @NotNull DecoderFactory getFactory();

    default int getPriority() {
        return 0;
    }
}
