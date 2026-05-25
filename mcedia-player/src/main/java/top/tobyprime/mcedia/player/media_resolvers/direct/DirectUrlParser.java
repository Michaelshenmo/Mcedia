package top.tobyprime.mcedia.player.media_resolvers.direct;

import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.resolver.MediaResolvers;
import top.tobyprime.mcedia.api.resolver.MediaUrlParser;

import java.util.Optional;

public class DirectUrlParser implements MediaUrlParser {
    @Override
    public Optional<Media> tryParse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        if (isHttpUrl(input) || isLocalFilePath(input)) {
            return Optional.of(MediaResolvers.resolveByPlatform("direct", input));
        }
        return Optional.empty();
    }

    private static boolean isHttpUrl(String input) {
        return input.startsWith("http://") || input.startsWith("https://");
    }

    private static boolean isLocalFilePath(String input) {
        if (input.startsWith("/") || input.startsWith("file://")) {
            return true;
        }
        // Windows absolute path: D:\ 或 D:/
        return input.length() >= 3
                && Character.isLetter(input.charAt(0))
                && input.charAt(1) == ':'
                && (input.charAt(2) == '\\' || input.charAt(2) == '/');
    }
}
