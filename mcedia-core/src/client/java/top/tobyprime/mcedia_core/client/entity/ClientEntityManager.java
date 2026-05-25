package top.tobyprime.mcedia_core.client.entity;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import top.tobyprime.mcedia_core.client.renderer.PlayerScreenEntityRenderer;
import top.tobyprime.mcedia_core.client.renderer.PlayerSpeakerEntityRenderer;

public final class ClientEntityManager {

    private static final String MOD_ID = "mcedia_core";

    public static EntityType<PlayerScreenEntity> PLAYER_SCREEN;
    public static EntityType<PlayerSpeakerEntity> PLAYER_SPEAKER;

    private ClientEntityManager() {
    }

    public static void init() {
        if (PLAYER_SCREEN != null) {
            return;
        }

        PLAYER_SCREEN = register(
                "player_screen",
                EntityType.Builder.of(PlayerScreenEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .noSave()
        );

        PLAYER_SPEAKER = register(
                "player_speaker",
                EntityType.Builder.of(PlayerSpeakerEntity::new, MobCategory.MISC)
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .noSave()
        );

        EntityRenderers.register(PLAYER_SCREEN, PlayerScreenEntityRenderer::new);
        EntityRenderers.register(PLAYER_SPEAKER, PlayerSpeakerEntityRenderer::new);
    }

    private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        var id = Identifier.fromNamespaceAndPath(MOD_ID, path);
        var key = ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }
}
