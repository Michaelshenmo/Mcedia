package top.tobyprime.mcedia_core.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

public final class GlTextureUploader implements TextureUploader {
    private static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    private static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    private static final int GL_STREAM_DRAW = 0x88E0;

    private final int[] pbo = new int[2];
    private final int[] pboAllocSize = new int[2];
    private boolean closed;
    private boolean initialized;

    public GlTextureUploader() {
        RenderSystem.assertOnRenderThread();
        this.pbo[0] = GlStateManager._glGenBuffers();
        this.pbo[1] = GlStateManager._glGenBuffers();
    }

    @Override
    public void upload(int textureId, int width, int height, ByteBuffer data, int index) {
        RenderSystem.assertOnRenderThread();
        if (closed || !data.hasRemaining()) {
            return;
        }

        int slot = index & 1;
        int writePbo = pbo[slot];
        int readPbo = pbo[1 - slot];
        int requiredBytes = data.remaining();

        if (!initialized) {
            for (int i = 0; i < 2; i++) {
                GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, pbo[i]);
                GlStateManager._glBufferData(GL15.GL_PIXEL_UNPACK_BUFFER, requiredBytes, GL_STREAM_DRAW);
                pboAllocSize[i] = requiredBytes;
            }
            initialized = true;
        }

        if (requiredBytes > pboAllocSize[slot]) {
            GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, writePbo);
            GlStateManager._glBufferData(GL15.GL_PIXEL_UNPACK_BUFFER, requiredBytes, GL_STREAM_DRAW);
            pboAllocSize[slot] = requiredBytes;
        } else {
            GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, writePbo);
        }

        int access = GL15.GL_MAP_WRITE_BIT
                | GL_MAP_INVALIDATE_BUFFER_BIT
                | GL_MAP_UNSYNCHRONIZED_BIT;
        ByteBuffer mapped = GlStateManager._glMapBufferRange(
                GL15.GL_PIXEL_UNPACK_BUFFER, 0, requiredBytes, access);
        if (mapped != null) {
            mapped.put(data);
            GlStateManager._glUnmapBuffer(GL15.GL_PIXEL_UNPACK_BUFFER);
        } else {
            data.rewind();
            GlStateManager._glBufferData(GL15.GL_PIXEL_UNPACK_BUFFER, data, GL_STREAM_DRAW);
        }

        if (requiredBytes > pboAllocSize[1 - slot]) {
            GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, readPbo);
            GlStateManager._glBufferData(GL15.GL_PIXEL_UNPACK_BUFFER, requiredBytes, GL_STREAM_DRAW);
            pboAllocSize[1 - slot] = requiredBytes;
        }

        GlStateManager._bindTexture(textureId);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);

        GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, readPbo);
        GlStateManager._texSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0, 0,
            width, height,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            0L
        );

        GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (!closed) {
            closed = true;
            GlStateManager._glBindBuffer(GL15.GL_PIXEL_UNPACK_BUFFER, 0);
            GlStateManager._glDeleteBuffers(pbo[0]);
            GlStateManager._glDeleteBuffers(pbo[1]);
        }
    }
}