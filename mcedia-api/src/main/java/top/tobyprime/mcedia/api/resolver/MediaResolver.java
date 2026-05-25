package top.tobyprime.mcedia.api.resolver;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.media.Media;

public interface MediaResolver {
    @NotNull Media resolve(@NotNull String target);
}
