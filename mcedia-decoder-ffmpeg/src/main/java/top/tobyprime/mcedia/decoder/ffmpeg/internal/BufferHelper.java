package top.tobyprime.mcedia.decoder.ffmpeg.internal;

import java.nio.*;

public class BufferHelper {
    public static Buffer cloneBuffer(Buffer src) {
        switch (src) {
            case ByteBuffer byteBuf -> {
                ByteBuffer dup = byteBuf.duplicate();
                dup.clear();
                ByteBuffer copy = DirectBufferPool.allocBytes(dup.remaining());
                copy.put(dup);
                copy.flip();
                return copy;
            }
            case ShortBuffer shortBuf -> {
                ShortBuffer dup = shortBuf.duplicate();
                dup.clear();
                ShortBuffer copy = DirectBufferPool.allocShorts(dup.remaining());
                copy.put(dup);
                copy.flip();
                return copy;
            }
            case IntBuffer intBuf -> {
                IntBuffer dup = intBuf.duplicate();
                dup.clear();
                IntBuffer copy = DirectBufferPool.allocInts(dup.remaining());
                copy.put(dup);
                copy.flip();
                return copy;
            }
            case FloatBuffer floatBuf -> {
                FloatBuffer dup = floatBuf.duplicate();
                dup.clear();
                FloatBuffer copy = DirectBufferPool.allocFloats(dup.remaining());
                copy.put(dup);
                copy.flip();
                return copy;
            }
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + src.getClass());
        }
    }
}
