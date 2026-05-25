package top.tobyprime.mcedia_core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import top.tobyprime.mcedia_core.client.entity.PlayerSpeakerEntity;

public class PlayerSpeakerEntityRenderer extends EntityRenderer<PlayerSpeakerEntity, EntityRenderState> {

    public PlayerSpeakerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
