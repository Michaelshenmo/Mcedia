package top.tobyprime.mcedia_core.client.danmaku.render;

import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.NotNull;

public final class DanmakuWidthMeasurer {
    private DanmakuWidthMeasurer() {
    }

    public static float measureNormalizedWidth(@NotNull Font font, @NotNull String text, float textHeight, float screenWidth) {
        if (textHeight <= 0.0F || screenWidth <= 0.0F || text.isEmpty()) {
            return 0.0F;
        }
        return normalizePixelWidth(font.width(text), textHeight, font.lineHeight, screenWidth);
    }

    static float normalizePixelWidth(float pixelWidth, float textHeight, int lineHeight, float screenWidth) {
        if (screenWidth <= 0.0F) {
            return 0.0F;
        }
        return pixelWidthToWorldWidth(pixelWidth, textHeight, lineHeight) / screenWidth;
    }

    static float pixelWidthToWorldWidth(float pixelWidth, float textHeight, int lineHeight) {
        if (pixelWidth <= 0.0F || textHeight <= 0.0F || lineHeight <= 0) {
            return 0.0F;
        }
        return pixelWidth * textHeight / lineHeight;
    }

    static float worldWidthToPixelWidth(float worldWidth, float textHeight, int lineHeight) {
        if (worldWidth <= 0.0F || textHeight <= 0.0F || lineHeight <= 0) {
            return 0.0F;
        }
        return worldWidth * lineHeight / textHeight;
    }
}
