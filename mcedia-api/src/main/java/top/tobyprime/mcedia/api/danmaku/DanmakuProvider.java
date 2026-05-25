package top.tobyprime.mcedia.api.danmaku;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.media.MediaInfo;

import java.util.concurrent.CompletableFuture;

public interface DanmakuProvider {
    @NotNull CompletableFuture<DanmakuDocument> load(@NotNull MediaInfo mediaInfo);
}
