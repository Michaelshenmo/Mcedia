package top.tobyprime.mcedia.api.resolver;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.media.Media;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MediaResolvers {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaResolvers.class);

    private static final Map<String, MediaResolver> RESOLVERS = new ConcurrentHashMap<>();
    private static final List<PrioritizedParser> PARSERS = new CopyOnWriteArrayList<>();

    private record PrioritizedParser(MediaUrlParser parser, int priority) {
    }

    private MediaResolvers() {
    }

    // -- platform resolver registry --

    public static void register(@NotNull String platform, @NotNull MediaResolver resolver) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Resolver platform cannot be blank");
        }

        var normalizedPlatform = platform.trim().toLowerCase(Locale.ROOT);
        var previous = RESOLVERS.put(normalizedPlatform, Objects.requireNonNull(resolver, "resolver"));
        if (previous != null) {
            LOGGER.warn("Resolver for platform '{}' is overwritten.", normalizedPlatform);
        }
    }

    /**
     * 按 platform 名称直接解析 target，供 {@link MediaUrlParser} 实现内部委托使用。
     */
    public static @NotNull Media resolveByPlatform(@NotNull String platform, @NotNull String target) {
        var resolver = RESOLVERS.get(platform.toLowerCase(Locale.ROOT));
        if (resolver == null) {
            throw new IllegalArgumentException("No media resolver registered for platform: " + platform);
        }
        return Objects.requireNonNull(resolver.resolve(target), "resolver returned null media");
    }

    // -- non-standard URL parser registry (priority-based) --

    /**
     * 注册非标准播放字符串解析器，使用默认优先级 100。
     */
    public static void registerParser(@NotNull MediaUrlParser parser) {
        registerParser(parser, 100);
    }

    /**
     * 注册非标准播放字符串解析器，指定优先级（值越小优先级越高）。
     */
    public static void registerParser(@NotNull MediaUrlParser parser, int priority) {
        Objects.requireNonNull(parser, "parser");
        PARSERS.add(new PrioritizedParser(parser, priority));
        PARSERS.sort(Comparator.comparingInt(PrioritizedParser::priority));
    }

    // -- test support --

    static void reset() {
        RESOLVERS.clear();
        PARSERS.clear();
    }

    // -- resolution --

    /**
     * 解析播放字符串为 Media。
     * <p>
     * 按优先级依次尝试已注册的 {@link MediaUrlParser}，
     * 首个匹配的解析结果被返回。如无一匹配则抛出异常。
     */
    public static @NotNull Media resolve(@NotNull String url) {
        Objects.requireNonNull(url, "mediaUrl");
        var normalizedUrl = stripWrappingQuotes(url.trim());

        for (var entry : PARSERS) {
            var result = entry.parser().tryParse(normalizedUrl);
            if (result.isPresent()) {
                return result.get();
            }
        }

        throw new IllegalArgumentException("Unsupported media url: " + normalizedUrl);
    }

    private static String stripWrappingQuotes(String input) {
        if (input.length() >= 2) {
            char first = input.charAt(0);
            char last = input.charAt(input.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return input.substring(1, input.length() - 1).trim();
            }
        }
        return input;
    }

}
