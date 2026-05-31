package top.tobyprime.mcedia_platforms.media;

import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.resolver.MediaResolvers;
import top.tobyprime.mcedia.api.resolver.MediaUrlParser;

import java.util.Optional;
import java.util.regex.Pattern;

public class DouyinUrlParser implements MediaUrlParser {
    private static final Pattern DOUYIN_PATTERN = Pattern.compile(
            "^(https?://)?([\\w-]+\\.)?(douyin\\.com|iesdouyin\\.com)/.*"
    );
    private static final Pattern DOUYIN_URL_IN_TEXT_PATTERN = Pattern.compile(
            "(https?://(?:[\\w-]+\\.)?(?:douyin\\.com|iesdouyin\\.com)/\\S+)"
    );

    @Override
    public Optional<Media> tryParse(String input) {
        var normalizedInput = extractSupportedInput(input);
        if (normalizedInput == null) {
            return Optional.empty();
        }
        if (DOUYIN_PATTERN.matcher(normalizedInput).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("douyin", normalizedInput));
        }
        return Optional.empty();
    }

    private static String extractSupportedInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        var trimmed = input.trim();
        if (DOUYIN_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        var douyinUrlMatcher = DOUYIN_URL_IN_TEXT_PATTERN.matcher(trimmed);
        if (douyinUrlMatcher.find()) {
            return douyinUrlMatcher.group(1);
        }

        return null;
    }
}
