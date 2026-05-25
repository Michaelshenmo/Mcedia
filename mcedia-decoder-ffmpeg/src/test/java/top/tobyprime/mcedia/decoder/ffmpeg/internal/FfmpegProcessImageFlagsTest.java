package top.tobyprime.mcedia.decoder.ffmpeg.internal;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegProcessImageFlagsTest {
    @Test
    void setAndRemoveFlagUsesExpectedDefaults() {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("test");
        try {
            assertTrue(FfmpegProcessImageFlags.isEnableProcessImage(grabber));

            FfmpegProcessImageFlags.setProcessImage(grabber, false);
            assertFalse(FfmpegProcessImageFlags.isEnableProcessImage(grabber));

            FfmpegProcessImageFlags.remove(grabber);
            assertTrue(FfmpegProcessImageFlags.isEnableProcessImage(grabber));
        } finally {
            FfmpegProcessImageFlags.remove(grabber);
        }
    }
}
