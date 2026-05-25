package top.tobyprime.mcedia_core.client.audio;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.client.sounds.SoundManager;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia_core.client.mixin.accessor.SoundEngineAccessor;
import top.tobyprime.mcedia_core.client.mixin.accessor.SoundManagerAccessor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class MinecraftSoundEngineAdapter {
    private static final String SOUND_THREAD_NAME = "Sound engine";

    public @Nullable ChannelAccess.ChannelHandle createStreamingHandle() {
        var soundEngine = getSoundEngine();
        if (soundEngine == null || !((SoundEngineAccessor) soundEngine).mcedia$isLoaded()) {
            return null;
        }

        var channelAccess = ((SoundEngineAccessor) soundEngine).mcedia$getChannelAccess();
        return channelAccess.createHandle(Library.Pool.STREAMING).join();
    }

    public boolean executeOnAudioThread(Runnable action) {
        var executor = getExecutor();
        if (executor == null) {
            return false;
        }

        if (isAudioThread()) {
            action.run();
        } else {
            executor.execute(action);
        }
        return true;
    }

    public <T> @Nullable T executeBlockingOnAudioThread(Supplier<T> action) {
        var executor = getExecutor();
        if (executor == null) {
            return null;
        }

        if (isAudioThread()) {
            return action.get();
        }

        var future = new CompletableFuture<T>();
        executor.execute(() -> {
            try {
                future.complete(action.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future.join();
    }

    public boolean isLoaded() {
        return getExecutor() != null;
    }

    private static boolean isAudioThread() {
        return SOUND_THREAD_NAME.equals(Thread.currentThread().getName());
    }

    private static @Nullable SoundEngineExecutor getExecutor() {
        var soundEngine = getSoundEngine();
        if (soundEngine == null) {
            return null;
        }

        var accessor = (SoundEngineAccessor) soundEngine;
        if (!accessor.mcedia$isLoaded()) {
            return null;
        }
        return accessor.mcedia$getExecutor();
    }

    private static @Nullable SoundEngine getSoundEngine() {
        var soundManager = Minecraft.getInstance().getSoundManager();
        if (soundManager == null) {
            return null;
        }
        return ((SoundManagerAccessor) soundManager).mcedia$getSoundEngine();
    }
}
