package top.tobyprime.mcedia_core.client.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAlAudioSourceTest {
    @Test
    void convertsSecondsToMicroseconds() {
        assertEquals(500_000L, OpenAlAudioSource.secondsToMicros(0.5D));
        assertEquals(1_000_000L, OpenAlAudioSource.secondsToMicros(1.0D));
        assertEquals(2_000_000L, OpenAlAudioSource.secondsToMicros(2.0D));
    }

    @Test
    void clampsMaxDistanceToZeroOrAbove() {
        assertEquals(0.0F, OpenAlAudioSource.sanitizeMaxDistance(-5.0F), 0.0F);
        assertEquals(16.0F, OpenAlAudioSource.sanitizeMaxDistance(16.0F), 0.0F);
    }

    @Test
    void resetAudioStateClearsTrackedState() throws Exception {
        var source = new OpenAlAudioSource(new MinecraftSoundEngineAdapter(), () -> null);
        source.sourceId = 42;
        setField(source, "sampleRate", 48_000);
        setField(source, "cachedPlaytime", 123L);
        source.setChannelMode(SpeakerAudioChannelMode.RIGHT);

        invokeNoArg(source, "resetAudioStateInternal");

        assertEquals(0, source.getSourceId());
        assertEquals(-1L, getField(source, "cachedPlaytime"));
        assertEquals(-1, getField(source, "sampleRate"));
    }

    @Test
    void invalidateAudioStateClearsTrackedState() throws Exception {
        var source = new OpenAlAudioSource(new MinecraftSoundEngineAdapter(), () -> null);
        source.sourceId = 17;
        setField(source, "sampleRate", 48_000);
        setField(source, "cachedPlaytime", 321L);
        setField(source, "bufferIds", new int[]{7, 8});

        invokeNoArg(source, "invalidateAudioState");

        assertEquals(0, source.getSourceId());
        assertEquals(-1L, getField(source, "cachedPlaytime"));
        assertEquals(-1, getField(source, "sampleRate"));
        assertNull(getField(source, "bufferIds"));
    }

    private static void setField(OpenAlAudioSource source, String fieldName, Object value) throws Exception {
        var field = OpenAlAudioSource.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(source, value);
    }

    private static Object getField(OpenAlAudioSource source, String fieldName) throws Exception {
        var field = OpenAlAudioSource.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(source);
    }

    private static void invokeNoArg(OpenAlAudioSource source, String methodName) throws Exception {
        var method = OpenAlAudioSource.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(source);
    }
}
