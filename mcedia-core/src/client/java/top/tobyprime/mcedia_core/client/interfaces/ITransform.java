package top.tobyprime.mcedia_core.client.interfaces;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface ITransform {
    Vector3f getWorldPosition();

    Quaternionf getWorldRotation();
}
