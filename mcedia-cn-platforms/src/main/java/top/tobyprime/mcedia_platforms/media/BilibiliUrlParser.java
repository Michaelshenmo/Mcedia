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
    private static final Pattern BILIBILI_URL_IN_TEXT_PATTERN = Pattern.compile(
            "(https?://(?:[\\w-]+\\.)?(?:bilibili\\.com|b23\\.tv)/\\S+)"
    );
    private static final Pattern LIVE_ROOM_URL_IN_TEXT_PATTERN = Pattern.compile(
            "(https?://live\\.bilibili\\.com/\\d+(?:[?/][^\\s]*)?)"
    );
    private static final Pattern BV_IN_TEXT_PATTERN = Pattern.compile("(BV[a-zA-Z0-9]+)");

    @Override
    public Optional<Media> tryParse(String input) {
        var normalizedInput = extractSupportedInput(input);
        if (normalizedInput == null) {
            return Optional.empty();
        }
        // Live room URL takes precedence over generic bilibili pattern
        if (LIVE_ROOM_PATTERN.matcher(normalizedInput).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("bilibili_live", normalizedInput));
        }
        if (BILIBILI_PATTERN.matcher(normalizedInput).matches() || BV_PATTERN.matcher(normalizedInput).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("bilibili", normalizedInput));
        }
        return Optional.empty();
    }

    private static String extractSupportedInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        var trimmed = input.trim();
        if (LIVE_ROOM_PATTERN.matcher(trimmed).matches()
                || BILIBILI_PATTERN.matcher(trimmed).matches()
                || BV_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        var liveUrlMatcher = LIVE_ROOM_URL_IN_TEXT_PATTERN.matcher(trimmed);
        if (liveUrlMatcher.find()) {
            return liveUrlMatcher.group(1);
        }

        var bilibiliUrlMatcher = BILIBILI_URL_IN_TEXT_PATTERN.matcher(trimmed);
        if (bilibiliUrlMatcher.find()) {
            return bilibiliUrlMatcher.group(1);
        }

        var bvMatcher = BV_IN_TEXT_PATTERN.matcher(trimmed);
        if (bvMatcher.find()) {
            return bvMatcher.group(1);
        }

        return null;
    }
}
