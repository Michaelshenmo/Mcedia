package top.tobyprime.mcedia_core.client.audio;

import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia_core.client.player.PlayerHost;

public interface AudioSourceBinding extends AutoCloseable {
    void attach(@Nullable PlayerHost host);

    @Override
    void close();
}
