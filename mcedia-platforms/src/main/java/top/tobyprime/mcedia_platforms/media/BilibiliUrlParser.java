package top.tobyprime.mcedia_platforms.media;

import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.resolver.MediaResolvers;
import top.tobyprime.mcedia.api.resolver.MediaUrlParser;

import java.util.Optional;
import java.util.regex.Pattern;

public class BilibiliUrlParser implements MediaUrlParser {
    private static final Pattern BILIBILI_PATTERN = Pattern.compile(
            "^(https?://)?([\\w-]+\\.)?(bilibili\\.com|b23\\.tv)/.*"
    );
    private static final Pattern LIVE_ROOM_PATTERN = Pattern.compile(
            "^(https?://)?live\\.bilibili\\.com/\\d+([?/].*)?$"
    );
    private static final Pattern BV_PATTERN = Pattern.compile("^BV[a-zA-Z0-9]+$");

    @Override
    public Optional<Media> tryParse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        // Live room URL takes precedence over generic bilibili pattern
        if (LIVE_ROOM_PATTERN.matcher(input).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("bilibili_live", input));
        }
        if (BILIBILI_PATTERN.matcher(input).matches() || BV_PATTERN.matcher(input).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("bilibili", input));
        }
        return Optional.empty();
    }
}
