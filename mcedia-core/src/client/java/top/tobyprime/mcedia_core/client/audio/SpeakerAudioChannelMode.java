package top.tobyprime.mcedia_core.client.audio;

public enum SpeakerAudioChannelMode {
    MIX(2),
    LEFT(0),
    RIGHT(1);

    private final int frameChannel;

    SpeakerAudioChannelMode(int frameChannel) {
        this.frameChannel = frameChannel;
    }

    public int frameChannel() {
        return frameChannel;
    }
}
