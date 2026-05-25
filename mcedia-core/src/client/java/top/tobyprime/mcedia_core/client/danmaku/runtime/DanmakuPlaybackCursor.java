package top.tobyprime.mcedia_core.client.danmaku.runtime;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.danmaku.DanmakuDocument;
import top.tobyprime.mcedia.api.danmaku.DanmakuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DanmakuPlaybackCursor {
    private final @NotNull DanmakuDocument document;
    private int nextIndex;
    private long lastTimeUs = Long.MIN_VALUE;

    public DanmakuPlaybackCursor(@NotNull DanmakuDocument document) {
        this.document = Objects.requireNonNull(document, "document");
        this.nextIndex = 0;
    }

    public @NotNull List<DanmakuItem> pollNewItems(long currentTimeUs) {
        if (currentTimeUs < 0) {
            currentTimeUs = 0;
        }
        if (currentTimeUs < lastTimeUs) {
            nextIndex = document.findStartIndex(currentTimeUs);
        }
        lastTimeUs = currentTimeUs;

        var items = document.getItems();
        if (nextIndex >= items.size()) {
            return List.of();
        }

        var result = new ArrayList<DanmakuItem>();
        while (nextIndex < items.size()) {
            var item = items.get(nextIndex);
            if (item.getTimeUs() > currentTimeUs) {
                break;
            }
            result.add(item);
            nextIndex++;
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public void reset() {
        nextIndex = 0;
        lastTimeUs = Long.MIN_VALUE;
    }
}
