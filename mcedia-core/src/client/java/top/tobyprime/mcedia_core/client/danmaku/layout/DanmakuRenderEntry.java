package top.tobyprime.mcedia_core.client.danmaku.layout;

import org.jetbrains.annotations.NotNull;

public record DanmakuRenderEntry(@NotNull String text, int argb, float left, float width, int trackIndex) {
}
