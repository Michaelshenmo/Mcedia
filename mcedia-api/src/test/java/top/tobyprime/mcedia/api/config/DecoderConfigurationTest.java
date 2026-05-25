package top.tobyprime.mcedia.api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecoderConfigurationTest {
    @Test
    void supportsLongMicrosecondValues() {
        long largeValue = 3_000_000_000L;
        DecoderConfiguration config = new DecoderConfiguration.Builder()
                .cacheDuration(largeValue)
                .timeout(largeValue)
                .build();

        assertEquals(largeValue, config.getCacheDuration());
        assertEquals(largeValue, config.getTimeout());
    }

    @Test
    void usesFasterDefaultProbeSettings() {
        DecoderConfiguration config = new DecoderConfiguration.Builder().build();

        assertEquals(2_000_000L, config.getCacheDuration());
        assertEquals(2_000_000, config.getProbesize());
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().cacheDuration(-1L));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().timeout(-1L));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().audioSampleRate(-1));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().bufferSize(0));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().bufferSize(-1));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().probesize(0));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().probesize(-1));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().maxVideoSize(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DecoderConfiguration.Builder().maxVideoSize(0, -1));
    }

    @Test
    void acceptsBoundaryValuesForValidatedFields() {
        DecoderConfiguration config = new DecoderConfiguration.Builder()
                .audioSampleRate(0)
                .maxVideoSize(1920, 1080)
                .bufferSize(1)
                .probesize(1)
                .build();

        assertEquals(0, config.getAudioSampleRate());
        assertEquals(1920, config.getMaxVideoWidth());
        assertEquals(1080, config.getMaxVideoHeight());
        assertEquals(1, config.getBufferSize());
        assertEquals(1, config.getProbesize());
    }
}
