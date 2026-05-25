package top.tobyprime.mcedia_core.client.renderer;

import java.nio.ByteBuffer;

public interface TextureUploader {
    void upload(int textureId, int width, int height, ByteBuffer data, int index);

    void close();
}
