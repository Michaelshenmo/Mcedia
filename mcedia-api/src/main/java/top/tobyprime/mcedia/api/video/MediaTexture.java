package top.tobyprime.mcedia.api.video;

public interface MediaTexture {
    void upload(VideoFrame frame);

    void tick(long time);
}
