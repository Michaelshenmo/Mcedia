package top.tobyprime.mcedia_core.client.interfaces;

import net.minecraft.world.phys.Vec3;

public interface IAudioSourceHandle {
    void setPos(Vec3 pos);

    Vec3 getPos();

    void setVolume(float volume);

    float getVolume();
}
