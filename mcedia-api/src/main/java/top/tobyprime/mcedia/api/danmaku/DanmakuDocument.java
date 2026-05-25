package top.tobyprime.mcedia.api.danmaku;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class DanmakuDocument {
    public static final DanmakuDocument EMPTY = new DanmakuDocument(List.of());

    private static final Comparator<DanmakuItem> BY_TIME = Comparator.comparingLong(DanmakuItem::getTimeUs);

    private final @NotNull List<DanmakuItem> items;

    public DanmakuDocument(@NotNull List<DanmakuItem> items) {
        Objects.requireNonNull(items, "items");
        var sorted = new ArrayList<>(items);
        sorted.sort(BY_TIME);
        this.items = List.copyOf(sorted);
    }

    public @NotNull List<DanmakuItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int findStartIndex(long timeUs) {
        int low = 0;
        int high = items.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (items.get(mid).getTimeUs() < timeUs) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
