package top.tobyprime.mcedia_core.client.player;

import net.minecraft.client.renderer.culling.Frustum;
import org.jspecify.annotations.Nullable;

public interface MediaPlayerPeripheral {
    void setPlayerHost(@Nullable PlayerHost host);

    boolean isActive();
    boolean isAlive();

    double getDistance();

    PeripheralType getPeripheralType();

    /** 返回外设是否在视锥体内（可见）。默认返回 true（总是可见）。 */
    default boolean isVisible(@Nullable Frustum frustum) {
        return true;
    }
}
