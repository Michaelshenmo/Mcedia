package top.tobyprime.mcedia.decoder.ffmpeg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;
import top.tobyprime.mcedia.api.video.VideoFrame;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FfmpegDecoderIntegrationTest {
    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration END_TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    Path tempDir;

    @Test
    void openDecodesAtLeastOneRealVideoFrame() throws Exception {
        Path videoPath = TestMediaFactory.createVideoOnly(tempDir);
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(videoPath.toString()), stableDecoderConfig());

        decoder.open();
        try {
            VideoFrame frame = awaitVideoFrame(decoder, FRAME_TIMEOUT);
            try {
                assertEquals(TestMediaFactory.VIDEO_WIDTH, frame.getWidth());
                assertEquals(TestMediaFactory.VIDEO_HEIGHT, frame.getHeight());
                assertTrue(frame.getBuffer().remaining() >= frame.getWidth() * frame.getHeight() * 4);
            } finally {
                frame.close();
            }
        } finally {
            decoder.close();
        }
    }

    @Test
    void openDecodesAudioFramesWhenMediaHasSeparateAudio() throws Exception {
        TestMediaFactory.MediaFixture media = TestMediaFactory.createVideoWithSeparateAudio(tempDir);
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(media.videoPath().toString(), media.audioPath().toString(), null, null), stableDecoderConfig());

        decoder.open();
        VideoFrame videoFrame = null;
        AudioFrame audioFrame = null;
        try {
            videoFrame = awaitVideoFrame(decoder, FRAME_TIMEOUT);
            audioFrame = awaitAudioFrame(decoder, FRAME_TIMEOUT);

            assertNotNull(videoFrame);
            assertEquals(TestMediaFactory.VIDEO_WIDTH, videoFrame.getWidth());
            assertEquals(TestMediaFactory.VIDEO_HEIGHT, videoFrame.getHeight());
            assertNotNull(audioFrame);
            assertEquals(TestMediaFactory.AUDIO_SAMPLE_RATE, audioFrame.getSampleRate());
            assertTrue(audioFrame.getBuffer(0).remaining() > 0);
        } finally {
            if (videoFrame != null) {
                videoFrame.close();
            }
            if (audioFrame != null) {
                audioFrame.close();
            }
            decoder.close();
        }
    }

    @Test
    void seekAfterDecodingStillProducesFrames() throws Exception {
        Path videoPath = TestMediaFactory.createVideoOnly(tempDir);
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(videoPath.toString()), stableDecoderConfig());

        decoder.open();
        try {
            VideoFrame first = awaitVideoFrame(decoder, FRAME_TIMEOUT);
            long firstTime;
            try {
                firstTime = first.getTime();
            } finally {
                first.close();
            }

            decoder.seek(0);

            VideoFrame second = awaitVideoFrame(decoder, FRAME_TIMEOUT);
            try {
                assertTrue(second.getTime() >= 0);
                assertEquals(TestMediaFactory.VIDEO_WIDTH, second.getWidth());
                assertEquals(TestMediaFactory.VIDEO_HEIGHT, second.getHeight());
            } finally {
                second.close();
            }
        } finally {
            decoder.close();
        }
    }

    @Test
    void decodeReachesEofAndEventuallyEnds() throws Exception {
        Path videoPath = TestMediaFactory.createVideoOnly(tempDir);
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(videoPath.toString()), stableDecoderConfig());
        int decodedFrames = 0;

        decoder.open();
        try {
            long deadline = System.nanoTime() + END_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                VideoFrame frame = decoder.getVideoStream().poll();
                if (frame != null) {
                    decodedFrames++;
                    frame.close();
                }
                if (decoder.isEnded() && decoder.getVideoStream().size() == 0) {
                    break;
                }
                Thread.sleep(10);
            }

            assertTrue(decodedFrames > 0, "Expected at least one decoded video frame before EOF");
            awaitCondition(decoder::isEnded, END_TIMEOUT, "decoder should eventually end after EOF");
        } finally {
            decoder.close();
        }
    }

    @Test
    void closeDuringRealDecodeStopsCleanly() throws Exception {
        TestMediaFactory.MediaFixture media = TestMediaFactory.createVideoWithSeparateAudio(tempDir);
        FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(media.videoPath().toString(), media.audioPath().toString(), null, null), stableDecoderConfig());

        decoder.open();
        VideoFrame frame = null;
        try {
            frame = awaitVideoFrame(decoder, FRAME_TIMEOUT);
        } finally {
            if (frame != null) {
                frame.close();
            }
            decoder.close();
        }

        assertFalse(decoder.isOpen());
        assertTrue(decoder.isEnded());
        decoder.close();
    }

    @Test
    void repeatedOpenDecodeAndCloseStaysStable() throws Exception {
        TestMediaFactory.MediaFixture media = TestMediaFactory.createVideoWithSeparateAudio(tempDir);
        for (int i = 0; i < 5; i++) {
            FfmpegDecoder decoder = new FfmpegDecoder(new MediaPlayInfo(media.videoPath().toString(), media.audioPath().toString(), null, null), stableDecoderConfig());
            decoder.open();
            VideoFrame frame = null;
            try {
                frame = awaitVideoFrame(decoder, FRAME_TIMEOUT);
            } finally {
                if (frame != null) {
                    frame.close();
                }
                decoder.close();
            }
            assertFalse(decoder.isOpen());
            assertTrue(decoder.isEnded());
        }
    }

    private static DecoderConfiguration stableDecoderConfig() {
        return new DecoderConfiguration.Builder()
                .disableHardwareDecoding()
                .build();
    }

    private static VideoFrame awaitVideoFrame(FfmpegDecoder decoder, Duration timeout) throws Exception {
        return awaitFrame(timeout, "video frame", () -> decoder.getVideoStream().poll());
    }

    private static AudioFrame awaitAudioFrame(FfmpegDecoder decoder, Duration timeout) throws Exception {
        return awaitFrame(timeout, "audio frame", () -> decoder.getAudioStream().poll());
    }

    private static <T> T awaitFrame(Duration timeout, String label, CheckedSupplier<T> supplier) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            T frame = supplier.get();
            if (frame != null) {
                return frame;
            }
            Thread.sleep(10);
        }
        fail("Timed out waiting for " + label);
        return null;
    }

    private static void awaitCondition(BooleanSupplier condition, Duration timeout, String message) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail(message);
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
