package top.tobyprime.mcedia.decoder.ffmpeg.frame;

import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.decoder.metrics.DecoderMetrics;
import top.tobyprime.mcedia.decoder.ffmpeg.internal.DirectBufferPool;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class FfmpegAudioFrame implements AudioFrame {

    final Frame frame;

    @Nullable
    private Buffer pcmLeft;
    private int pcmLeftBytes;
    @Nullable
    private Buffer pcmRight;
    private int pcmRightBytes;
    @Nullable
    private Buffer pcmM;
    private int pcmMBytes;
    private boolean closed;

    public FfmpegAudioFrame(Frame frame) {
        this.frame = frame.clone();
        DecoderMetrics.tracker().onAudioFrameCreated();
    }

    private static Buffer toReadOnly(Buffer src) {
        return switch (src) {
            case ByteBuffer byteBuf -> byteBuf.asReadOnlyBuffer();
            case ShortBuffer shortBuf -> shortBuf.asReadOnlyBuffer();
            case IntBuffer intBuf -> intBuf.asReadOnlyBuffer();
            case FloatBuffer floatBuf -> floatBuf.asReadOnlyBuffer();
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + src.getClass());
        };
    }

    private static Buffer mergeToMono(Buffer[] channelsData) {
        if (channelsData == null || channelsData.length == 0) {
            throw new IllegalArgumentException("No input channels");
        }

        Buffer first = channelsData[0];
        int channels = channelsData.length;

        return switch (first) {
            case ByteBuffer a -> mergeByte(channelsData, channels);
            case ShortBuffer a -> mergeShort(channelsData, channels);
            case FloatBuffer a -> mergeFloat(channelsData, channels);
            case IntBuffer a -> mergeInt(channelsData, channels);
            default -> throw new IllegalArgumentException("Unsupported Buffer type: " + first.getClass());
        };
    }

    private static ByteBuffer mergeByte(Buffer[] channels, int count) {
        int frames = channels[0].remaining();
        ByteBuffer dst = DirectBufferPool.allocBytes(frames);

        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int ch = 0; ch < count; ch++) {
                sum += ((ByteBuffer) channels[ch]).get(i);
            }
            dst.put((byte) (sum / count));
        }
        dst.flip();
        return dst;
    }

    private static ShortBuffer mergeShort(Buffer[] channels, int count) {
        int frames = channels[0].remaining();
        ShortBuffer dst = DirectBufferPool.allocShorts(frames);

        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int ch = 0; ch < count; ch++) {
                sum += ((ShortBuffer) channels[ch]).get(i);
            }
            dst.put((short) (sum / count));
        }
        dst.flip();
        return dst;
    }

    private static IntBuffer mergeInt(Buffer[] channels, int count) {
        int frames = channels[0].remaining();
        IntBuffer dst = DirectBufferPool.allocInts(frames);

        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int ch = 0; ch < count; ch++) {
                sum += ((IntBuffer) channels[ch]).get(i);
            }
            dst.put(sum / count);
        }
        dst.flip();
        return dst;
    }

    private static FloatBuffer mergeFloat(Buffer[] channels, int count) {
        int frames = channels[0].remaining();
        FloatBuffer dst = DirectBufferPool.allocFloats(frames);

        for (int i = 0; i < frames; i++) {
            float sum = 0;
            for (int ch = 0; ch < count; ch++) {
                sum += ((FloatBuffer) channels[ch]).get(i);
            }
            dst.put(sum / count);
        }
        dst.flip();
        return dst;
    }

    @Override
    public long getTime() {
        return frame.timestamp;
    }

    @Override
    public int getSampleRate() {
        return frame.sampleRate;
    }

    @Override
    public Buffer getBuffer(int channel) {
        if (channel == 0) {
            return getLeftBuffer();
        }
        if (channel == 1) {
            return getRightBuffer();
        }
        if (pcmM == null) {
            pcmM = isPackedAudio()
                    ? mergePackedToMono(frame.samples[0], channelCount())
                    : mergeToMono(frame.samples);
            pcmMBytes = DirectBufferPool.allocatedBytes(pcmM);
            if (pcmMBytes > 0) {
                DecoderMetrics.tracker().onAudioFrameBytesAllocated(pcmMBytes);
            }
        }
        return pcmM;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        DirectBufferPool.release(pcmLeft);
        if (pcmLeftBytes > 0) {
            DecoderMetrics.tracker().onAudioFrameBytesReleased(pcmLeftBytes);
        }
        DirectBufferPool.release(pcmRight);
        if (pcmRightBytes > 0) {
            DecoderMetrics.tracker().onAudioFrameBytesReleased(pcmRightBytes);
        }
        DirectBufferPool.release(pcmM);
        if (pcmMBytes > 0) {
            DecoderMetrics.tracker().onAudioFrameBytesReleased(pcmMBytes);
        }
        frame.close();
        DecoderMetrics.tracker().onAudioFrameReleased();
    }

    private Buffer getLeftBuffer() {
        if (isPackedAudio()) {
            if (pcmLeft == null) {
                pcmLeft = extractPackedChannel(frame.samples[0], channelCount(), 0);
                pcmLeftBytes = DirectBufferPool.allocatedBytes(pcmLeft);
                if (pcmLeftBytes > 0) {
                    DecoderMetrics.tracker().onAudioFrameBytesAllocated(pcmLeftBytes);
                }
            }
            return pcmLeft;
        }
        return toReadOnly(frame.samples[0]);
    }

    private Buffer getRightBuffer() {
        if (isPackedAudio()) {
            if (pcmRight == null) {
                pcmRight = extractPackedChannel(frame.samples[0], channelCount(), 1);
                pcmRightBytes = DirectBufferPool.allocatedBytes(pcmRight);
                if (pcmRightBytes > 0) {
                    DecoderMetrics.tracker().onAudioFrameBytesAllocated(pcmRightBytes);
                }
            }
            return pcmRight;
        }
        if (frame.samples.length > 1 && frame.samples[1] != null) {
            return toReadOnly(frame.samples[1]);
        }
        return toReadOnly(frame.samples[0]);
    }

    private boolean isPackedAudio() {
        return frame.samples.length == 1 && frame.audioChannels > 1 && frame.samples[0] != null;
    }

    private int channelCount() {
        return frame.audioChannels > 0 ? frame.audioChannels : frame.samples.length;
    }

    private static Buffer mergePackedToMono(Buffer source, int channels) {
        return switch (source) {
            case ByteBuffer byteBuffer -> mergePackedByte(byteBuffer, channels);
            case ShortBuffer shortBuffer -> mergePackedShort(shortBuffer, channels);
            case IntBuffer intBuffer -> mergePackedInt(intBuffer, channels);
            case FloatBuffer floatBuffer -> mergePackedFloat(floatBuffer, channels);
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + source.getClass());
        };
    }

    private static Buffer extractPackedChannel(Buffer source, int channels, int channel) {
        return switch (source) {
            case ByteBuffer byteBuffer -> extractPackedByte(byteBuffer, channels, channel);
            case ShortBuffer shortBuffer -> extractPackedShort(shortBuffer, channels, channel);
            case IntBuffer intBuffer -> extractPackedInt(intBuffer, channels, channel);
            case FloatBuffer floatBuffer -> extractPackedFloat(floatBuffer, channels, channel);
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + source.getClass());
        };
    }

    private static ByteBuffer mergePackedByte(ByteBuffer source, int channels) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocBytes(frames);
        for (int i = 0; i < frames; i++) {
            int base = start + i * channels;
            int sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                sum += input.get(base + ch);
            }
            output.put((byte) (sum / channels));
        }
        output.flip();
        return output;
    }

    private static ShortBuffer mergePackedShort(ShortBuffer source, int channels) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocShorts(frames);
        for (int i = 0; i < frames; i++) {
            int base = start + i * channels;
            int sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                sum += input.get(base + ch);
            }
            output.put((short) (sum / channels));
        }
        output.flip();
        return output;
    }

    private static IntBuffer mergePackedInt(IntBuffer source, int channels) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocInts(frames);
        for (int i = 0; i < frames; i++) {
            int base = start + i * channels;
            long sum = 0L;
            for (int ch = 0; ch < channels; ch++) {
                sum += input.get(base + ch);
            }
            output.put((int) (sum / channels));
        }
        output.flip();
        return output;
    }

    private static FloatBuffer mergePackedFloat(FloatBuffer source, int channels) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocFloats(frames);
        for (int i = 0; i < frames; i++) {
            int base = start + i * channels;
            float sum = 0.0F;
            for (int ch = 0; ch < channels; ch++) {
                sum += input.get(base + ch);
            }
            output.put(sum / channels);
        }
        output.flip();
        return output;
    }

    private static ByteBuffer extractPackedByte(ByteBuffer source, int channels, int channel) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocBytes(frames);
        for (int i = 0; i < frames; i++) {
            output.put(input.get(start + i * channels + channel));
        }
        output.flip();
        return output;
    }

    private static ShortBuffer extractPackedShort(ShortBuffer source, int channels, int channel) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocShorts(frames);
        for (int i = 0; i < frames; i++) {
            output.put(input.get(start + i * channels + channel));
        }
        output.flip();
        return output;
    }

    private static IntBuffer extractPackedInt(IntBuffer source, int channels, int channel) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocInts(frames);
        for (int i = 0; i < frames; i++) {
            output.put(input.get(start + i * channels + channel));
        }
        output.flip();
        return output;
    }

    private static FloatBuffer extractPackedFloat(FloatBuffer source, int channels, int channel) {
        var input = source.duplicate();
        int start = input.position();
        int frames = input.remaining() / channels;
        var output = DirectBufferPool.allocFloats(frames);
        for (int i = 0; i < frames; i++) {
            output.put(input.get(start + i * channels + channel));
        }
        output.flip();
        return output;
    }
}
