package top.tobyprime.mcedia.api.media;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class MediaInfo {
    private final @NotNull String title;
    private final @NotNull String artist;
    private final @Nullable String coverUrl;
    private final @NotNull String platform;
    private final @NotNull Map<String, String> extraMetadata;

    public MediaInfo() {
        this("Unknown", "Unknown", null, "Unknown", Map.of());
    }

    public MediaInfo(@NotNull String title, @NotNull String artist, @Nullable String coverUrl, @NotNull String platform) {
        this(title, artist, coverUrl, platform, Map.of());
    }

    public MediaInfo(@NotNull String title, @NotNull String artist, @Nullable String coverUrl, @NotNull String platform, @Nullable Map<String, String> extraMetadata) {
        this.title = Objects.requireNonNull(title, "title");
        this.artist = Objects.requireNonNull(artist, "artist");
        this.coverUrl = coverUrl;
        this.platform = Objects.requireNonNull(platform, "platform");
        this.extraMetadata = extraMetadata == null ? Map.of() : Map.copyOf(extraMetadata);
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull String getArtist() {
        return artist;
    }

    public @Nullable String getCoverUrl() {
        return coverUrl;
    }

    public @NotNull String getPlatform() {
        return platform;
    }

    public @NotNull Map<String, String> getExtraMetadata() {
        return extraMetadata;
    }
}
