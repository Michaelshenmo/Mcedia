package top.tobyprime.mcedia_core.client.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
