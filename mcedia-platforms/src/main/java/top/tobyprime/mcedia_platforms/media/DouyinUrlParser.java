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

    @Override
    public Optional<Media> tryParse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        if (DOUYIN_PATTERN.matcher(input).matches()) {
            return Optional.of(MediaResolvers.resolveByPlatform("douyin", input));
        }
        return Optional.empty();
    }
}
