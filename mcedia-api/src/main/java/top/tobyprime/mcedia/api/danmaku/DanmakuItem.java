package top.tobyprime.mcedia.api.danmaku;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DanmakuItem {
    private final long timeUs;
    private final @NotNull String text;
    private final int argb;
    private final @NotNull DanmakuType type;

    public DanmakuItem(long timeUs, @NotNull String text, int argb, @NotNull DanmakuType type) {
        if (timeUs < 0) {
            throw new IllegalArgumentException("timeUs must not be negative");
        }
        this.timeUs = timeUs;
        this.text = Objects.requireNonNull(text, "text");
        this.argb = argb;
        this.type = Objects.requireNonNull(type, "type");
    }

    public long getTimeUs() {
        return timeUs;
    }

    public @NotNull String getText() {
        return text;
    }

    public int getArgb() {
        return argb;
    }

    public @NotNull DanmakuType getType() {
        return type;
    }
}
