package top.tobyprime.mcedia_core.client.events;


public class MinecraftClientEvents {
    public static Event<Void> onAudioTick;
    public static Event<Void> onPostAudioTick;

    public static void init() {
        onAudioTick = new Event<>("On Audio Tick");
        onPostAudioTick = new Event<>("On Post Audio Tick");
    }
}
