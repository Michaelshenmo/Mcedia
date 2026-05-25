package top.tobyprime.mcedia.api.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaResolversTest {

    @BeforeEach
    void setUp() {
        MediaResolvers.reset();
    }

    // -- resolveByPlatform tests --

    @Test
    void resolvesByPlatform() {
        String platform = "by-platform-" + UUID.randomUUID();
        MediaResolvers.register(platform, target -> new TestMedia("result-" + target));

        Media resolved = MediaResolvers.resolveByPlatform(platform, "test-input");

        assertEquals("result-test-input", resolved.getPlayInfo().getUrl());
    }

    @Test
    void resolveByPlatformIsCaseInsensitive() {
        String platform = "case-" + UUID.randomUUID();
        MediaResolvers.register(platform, target -> new TestMedia("case-insensitive"));

        Media resolved = MediaResolvers.resolveByPlatform(platform.toUpperCase(), "x");

        assertEquals("case-insensitive", resolved.getPlayInfo().getUrl());
    }

    @Test
    void resolveByPlatformThrowsOnMissing() {
        String platform = "nonexistent-" + UUID.randomUUID();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MediaResolvers.resolveByPlatform(platform, "anything"));
        assertEquals("No media resolver registered for platform: " + platform, ex.getMessage());
    }

    @Test
    void allowsResolverOverwrite() {
        String platform = "overwrite-" + UUID.randomUUID();
        MediaResolvers.register(platform, target -> new TestMedia("first-" + target));
        MediaResolvers.register(platform, target -> new TestMedia("second-" + target));

        Media resolved = MediaResolvers.resolveByPlatform(platform, "abc");

        assertEquals("second-abc", resolved.getPlayInfo().getUrl());
    }

    // -- resolve() without parsers --

    @Test
    void throwsWhenNoParserMatches() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MediaResolvers.resolve("anything-at-all"));
        assertEquals("Unsupported media url: anything-at-all", ex.getMessage());
    }

    // -- parser-based resolution tests --

    @Test
    void resolvesByParser() {
        MediaResolvers.registerParser(input -> {
            if (input.equals("custom://test")) {
                return Optional.of(new TestMedia("parser-resolved"));
            }
            return Optional.empty();
        });

        Media resolved = MediaResolvers.resolve("custom://test");

        assertEquals("parser-resolved", resolved.getPlayInfo().getUrl());
    }

    @Test
    void parserWithEmptyResultContinuesToNextParser() {
        MediaResolvers.registerParser(input -> Optional.empty());
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("second-wins")), 0);

        Media resolved = MediaResolvers.resolve("anything");

        assertEquals("second-wins", resolved.getPlayInfo().getUrl());
    }

    @Test
    void allParsersReturnEmptyThrows() {
        MediaResolvers.registerParser(input -> Optional.empty());
        MediaResolvers.registerParser(input -> Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MediaResolvers.resolve("foo"));
        assertEquals("Unsupported media url: foo", ex.getMessage());
    }

    @Test
    void parserPriorityRespected() {
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("second")), 20);
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("first")), 0);

        Media resolved = MediaResolvers.resolve("anything");

        assertEquals("first", resolved.getPlayInfo().getUrl());
    }

    @Test
    void stripsWrappingQuotesBeforeParsing() {
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia(input)));

        Media resolved = MediaResolvers.resolve("\"D:\\videos\\test.mp4\"");

        assertEquals("D:\\videos\\test.mp4", resolved.getPlayInfo().getUrl());
    }

    @Test
    void higherPriorityParserWinsForSameInput() {
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("low")), 10);
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("high")), 5);
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("highest")), 0);

        Media resolved = MediaResolvers.resolve("anything");

        assertEquals("highest", resolved.getPlayInfo().getUrl());
    }

    @Test
    void parserAddedAfterAnotherStillRespectsPriority() {
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("b")), 10);
        MediaResolvers.registerParser(input -> Optional.of(new TestMedia("a")), 0);

        Media resolved = MediaResolvers.resolve("anything");

        assertEquals("a", resolved.getPlayInfo().getUrl());
    }

    private static final class TestMedia implements Media {
        private final MediaPlayInfo playInfo;
        private final MediaInfo info;

        private TestMedia(String url) {
            this.playInfo = new MediaPlayInfo(url);
            this.info = new MediaInfo();
        }

        @Override
        public MediaPlayInfo getPlayInfo() {
            return playInfo;
        }

        @Override
        public MediaInfo getInfo() {
            return info;
        }
    }
}
