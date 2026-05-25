package top.tobyprime.mcedia_platforms.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformResolverBootstrapTest {
    private static Method selectBestVideoTrack;

    @BeforeAll
    static void setUp() throws Exception {
        selectBestVideoTrack = PlatformResolverBootstrap.class.getDeclaredMethod(
                "selectBestVideoTrack", JsonArray.class, int.class);
        selectBestVideoTrack.setAccessible(true);
    }

    private JsonObject invokeSelect(JsonArray video, int maxHeight) throws Exception {
        return (JsonObject) selectBestVideoTrack.invoke(null, video, maxHeight);
    }

    private static JsonArray videoTracks(JsonObject... tracks) {
        var array = new JsonArray();
        for (var track : tracks) {
            array.add(track);
        }
        return array;
    }

    private static JsonObject track(String id, int width, int height) {
        var object = new JsonObject();
        object.addProperty("id", id);
        object.addProperty("width", width);
        object.addProperty("height", height);
        object.addProperty("bandwidth", width * height * 10);
        object.addProperty("baseUrl", "https://example.com/" + id + ".m4s");
        return object;
    }

    @Test
    void selectsHighestTrackWhenUnlimited() throws Exception {
        var video = videoTracks(
                track("80", 1920, 1080),
                track("64", 1280, 720)
        );

        var result = invokeSelect(video, 0);

        assertEquals("80", result.get("id").getAsString());
    }

    @Test
    void selectsWithinToleranceThreshold() throws Exception {
        var video = videoTracks(
                track("120", 3840, 2160),
                track("80", 1920, 1080),
                track("64", 1280, 720)
        );

        var result = invokeSelect(video, 1080);

        assertEquals("80", result.get("id").getAsString());
    }

    @Test
    void appliesToleranceToSlightlyExceedingTrack() throws Exception {
        var video = videoTracks(
                track("120", 3840, 2160),
                track("90", 1920, 1200),
                track("64", 1280, 720)
        );

        var result = invokeSelect(video, 1080);

        assertEquals("90", result.get("id").getAsString());
    }

    @Test
    void fallsBackToLowestWhenAllExceedWithTolerance() throws Exception {
        var video = videoTracks(
                track("120", 3840, 2160),
                track("80", 1920, 1080)
        );

        var result = invokeSelect(video, 720);

        assertEquals("80", result.get("id").getAsString());
    }

    @Test
    void skipsTracksWithoutHeightInfo() throws Exception {
        var unknown = new JsonObject();
        unknown.addProperty("id", "unknown");
        unknown.addProperty("baseUrl", "https://example.com/unknown.m4s");
        var video = videoTracks(
                unknown,
                track("64", 1280, 720),
                track("32", 640, 360)
        );

        var result = invokeSelect(video, 1080);

        assertEquals("64", result.get("id").getAsString());
    }

    @Test
    void singleTrackWithinTolerance() throws Exception {
        var video = videoTracks(track("64", 1280, 720));

        var result = invokeSelect(video, 1080);

        assertEquals("64", result.get("id").getAsString());
    }

    @Test
    void singleTrackExceedsEvenWithTolerance() throws Exception {
        var video = videoTracks(track("120", 3840, 2160));

        var result = invokeSelect(video, 720);

        assertEquals("120", result.get("id").getAsString());
    }

    @Test
    void higherWithinTolerancePreferred() throws Exception {
        var video = videoTracks(
                track("4k", 3840, 2160),
                track("1080p", 1920, 1080),
                track("720p", 1280, 720)
        );

        var result = invokeSelect(video, 1080);

        assertEquals("1080p", result.get("id").getAsString());
    }
}
