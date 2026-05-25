package top.tobyprime.mcedia_core.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia_core.client.audio.OpenAlAudioSource;
import top.tobyprime.mcedia_core.client.events.MinecraftClientEvents;

import java.lang.reflect.Method;

public final class SoundPhysicsCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundPhysicsCompat.class);
    private static final Identifier SOUND_ID = Identifier.fromNamespaceAndPath("mcedia", "video_sound");

    private static Method processSoundMethod;
    private static boolean active;

    private SoundPhysicsCompat() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("sound_physics_remastered")) {
            return;
        }

        if (!resolveMethod()) {
            LOGGER.warn("SoundPhysics Remastered 存在但无法找到 processSound 方法，兼容将禁用");
            return;
        }

        MinecraftClientEvents.onPostAudioTick.addHandler(ignored -> onAudioTick());
        active = true;
        LOGGER.info("已启用物理声效兼容");
    }

    private static boolean resolveMethod() {
        Class<?> soundPhysicsClass;
        try {
            soundPhysicsClass = Class.forName("com.sonicether.soundphysics.SoundPhysics");
        } catch (ClassNotFoundException e) {
            return false;
        }

        // 优先尝试 Identifier 参数（新版 API）
        try {
            processSoundMethod = soundPhysicsClass.getMethod(
                    "processSound",
                    int.class,
                    double.class,
                    double.class,
                    double.class,
                    SoundSource.class,
                    Identifier.class
            );
            return true;
        } catch (NoSuchMethodException ignored) {
        }

        // 回退到 String 参数（旧版 API）
        try {
            processSoundMethod = soundPhysicsClass.getMethod(
                    "processSound",
                    int.class,
                    double.class,
                    double.class,
                    double.class,
                    SoundSource.class,
                    String.class
            );
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static void onAudioTick() {
        if (!Configs.PHYSICS || !active) {
            return;
        }

        for (var source : OpenAlAudioSource.getActiveSources()) {
            int sourceId = source.getSourceId();
            if (sourceId == 0) continue;

            var pos = source.getPos();
            if (pos == null) continue;

            try {
                processSoundMethod.invoke(null, sourceId, pos.x, pos.y, pos.z, SoundSource.MASTER, SOUND_ID);
            } catch (Exception e) {
                LOGGER.error("处理物理声效异常", e);
                active = false;
                return;
            }
        }
    }
}
