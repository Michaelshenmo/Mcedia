package top.tobyprime.mcedia.decoder.ffmpeg.frame;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FfmpegAudioFrameTest {

    @Test
    void packedStereoFrameShouldExposeDistinctLeftRightAndMixedMonoChannels() {
        Frame frame = new Frame();
        frame.sampleRate = 44_100;
        frame.timestamp = 123_456L;
        frame.audioChannels = 2;
        frame.samples = new Buffer[]{ShortBuffer.wrap(new short[]{10, 100, 20, 200})};

        FfmpegAudioFrame audioFrame = new FfmpegAudioFrame(frame);

        assertArrayEquals(new short[]{10, 20}, toShortArray((ShortBuffer) audioFrame.getBuffer(0)));
        assertArrayEquals(new short[]{100, 200}, toShortArray((ShortBuffer) audioFrame.getBuffer(1)));
        assertArrayEquals(new short[]{55, 110}, toShortArray((ShortBuffer) audioFrame.getBuffer(2)));
        audioFrame.close();
    }

    private static short[] toShortArray(ShortBuffer buffer) {
        ShortBuffer copy = buffer.duplicate();
        short[] values = new short[copy.remaining()];
        copy.get(values);
        return values;
    }
}
