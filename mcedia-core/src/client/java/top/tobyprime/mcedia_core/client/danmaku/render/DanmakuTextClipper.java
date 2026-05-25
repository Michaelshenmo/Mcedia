package top.tobyprime.mcedia_core.client.danmaku.render;

import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DanmakuTextClipper {
    private DanmakuTextClipper() {
    }

    public static @Nullable ClippedText clip(
            @NotNull Font font,
            @NotNull String text,
            float textHeight,
            float left,
            float right,
            float leftBound,
            float rightBound
    ) {
        if (text.isEmpty() || textHeight <= 0.0F || font.lineHeight <= 0) {
            return null;
        }
        if (right <= leftBound || left >= rightBound) {
            return null;
        }

        float scale = textHeight / font.lineHeight;
        String remaining = text;

        while (left < leftBound && !remaining.isEmpty()) {
            int cpLen = Character.charCount(remaining.codePointAt(0));
            remaining = remaining.substring(cpLen);
            left = right - font.width(remaining) * scale;
        }
        if (remaining.isEmpty()) {
            return null;
        }

        while (right > rightBound && !remaining.isEmpty()) {
            int cpLen = Character.charCount(remaining.codePointBefore(remaining.length()));
            remaining = remaining.substring(0, remaining.length() - cpLen);
            right = left + font.width(remaining) * scale;
        }
        if (remaining.isEmpty()) {
            return null;
        }

        return new ClippedText(remaining, left);
    }

    public record ClippedText(@NotNull String text, float drawLeft) {
    }
}
