package top.tobyprime.mcedia_core.client.command;

import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia_core.client.audio.SpeakerAudioChannelMode;
import top.tobyprime.mcedia_core.client.player.MediaPlayerHostManager;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral;
import top.tobyprime.mcedia_core.client.player.SpeakerPeripheral;

/**
 * Minimal 26.1-compatible dev bootstrap.
 * Newer client command APIs used by shared sources are not available on this line.
 */
public final class ClientCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommands.class);
    private static final float SCREEN_DIAGONAL_INCHES = 120.0F;
    private static final float SCREEN_ASPECT_WIDTH = 16.0F;
    private static final float SCREEN_ASPECT_HEIGHT = 9.0F;
    private static final float SCREEN_SCALE = (float) (SCREEN_DIAGONAL_INCHES * 0.0254F
            / Math.sqrt(SCREEN_ASPECT_WIDTH * SCREEN_ASPECT_WIDTH + SCREEN_ASPECT_HEIGHT * SCREEN_ASPECT_HEIGHT));
    private static final float SCREEN_WIDTH = SCREEN_SCALE * SCREEN_ASPECT_WIDTH;
    private static final float SCREEN_HEIGHT = SCREEN_SCALE * SCREEN_ASPECT_HEIGHT;

    private ClientCommands() {
    }

    public static void register() {
        LOGGER.info("Skip shared client commands on MC 26.1: required Fabric client command API is unavailable");
    }

    /**
     * Kept for local/manual bootstrap parity if future 26.1 hooks need it.
     */
    public static int createHostWithPeripherals() {
        var client = Minecraft.getInstance();
        var player = client.player;
        var level = client.level;
        if (player == null || level == null) {
            return 0;
        }

        var look = player.getLookAngle();
        var horizontalLook = new Vec3(look.x, 0.0, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-6) {
            var fallback = player.getForward();
            horizontalLook = new Vec3(fallback.x, 0.0, fallback.z);
        }
        horizontalLook = horizontalLook.normalize();
        var right = new Vec3(-horizontalLook.z, 0.0, horizontalLook.x);

        var basePos = player.position().add(0.0, 1.1, 0.0);
        var screenPos = basePos.add(horizontalLook.scale(2.0));

        var screen = new ScreenPeripheral(level);
        screen.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        screen.setPosition(screenPos);

        var speakerOffset = screen.getScreenWidth() * 0.5F + 0.35F;
        var speakerOffsetVector = right.scale(speakerOffset);
        var leftSpeakerPos = screenPos.subtract(speakerOffsetVector);
        var rightSpeakerPos = screenPos.add(speakerOffsetVector);

        var leftSpeaker = new SpeakerPeripheral(level);
        leftSpeaker.setMaxRange(SpeakerPeripheral.DEFAULT_MAX_RANGE);
        leftSpeaker.setAudioChannelMode(SpeakerAudioChannelMode.LEFT);
        leftSpeaker.setPosition(leftSpeakerPos);

        var rightSpeaker = new SpeakerPeripheral(level);
        rightSpeaker.setMaxRange(SpeakerPeripheral.DEFAULT_MAX_RANGE);
        rightSpeaker.setAudioChannelMode(SpeakerAudioChannelMode.RIGHT);
        rightSpeaker.setPosition(rightSpeakerPos);

        var manager = MediaPlayerHostManager.get();
        var decoderH = decoderMaxHeight(Configs.MAX_RESOLUTION_HEIGHT);
        var handle = manager.createHostAndGetId(new DecoderConfiguration.Builder()
                .maxVideoSize(decoderMaxWidth(decoderH), decoderH)
                .build());
        manager.assignPeripheralToHost(handle.hostId(), screen);
        manager.assignPeripheralToHost(handle.hostId(), leftSpeaker);
        manager.assignPeripheralToHost(handle.hostId(), rightSpeaker);

        player.sendSystemMessage(Component.literal(
                "Created host " + handle.hostId() + " with runtime screen and stereo speakers"));
        return Command.SINGLE_SUCCESS;
    }

    private static int decoderMaxHeight(int resolutionHeight) {
        if (resolutionHeight <= 0) {
            return 0;
        }
        return (int) (resolutionHeight * 1.2f);
    }

    private static int decoderMaxWidth(int decoderHeight) {
        if (decoderHeight <= 0) {
            return 0;
        }
        return decoderHeight * 16 / 9;
    }
}
