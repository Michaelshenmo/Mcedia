package top.tobyprime.mcedia.api.media;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class MediaPlayInfo {
    private final @NotNull String url;
    private final @Nullable String audioUrl;
    private final @Nullable Map<String, String> customHeaders;
    private final @Nullable String cookie;

    public MediaPlayInfo(@NotNull String url) {
        this(url, null, null, null);
    }

    public MediaPlayInfo(@NotNull String url, @Nullable String audioUrl, @Nullable Map<String, String> customHeaders, @Nullable String cookie) {
        this.url = Objects.requireNonNull(url, "url");
        if (this.url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        this.audioUrl = audioUrl;
        this.customHeaders = customHeaders == null ? null : Map.copyOf(customHeaders);
        this.cookie = cookie;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public @Nullable String getAudioUrl() {
        return audioUrl;
    }

    public @Nullable Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public @Nullable String getCookie() {
        return cookie;
    }

    public boolean hasAudio() {
        return audioUrl != null && !audioUrl.isEmpty();
    }
}
