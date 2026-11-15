package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EarthWallModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class MudWallRenderer extends EntityRenderer<MudWallEntity, MudWallRenderer.MudWallRenderState> {

    private final EarthWallModel earthModel;

    public static class MudWallRenderState extends EntityRenderState {
        public float wallYaw;
        public float progress;
    }

    public MudWallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.earthModel = new EarthWallModel(ctx.bakeLayer(NarutoRenderEvents.EARTH_WALL_LAYER));
    }

    @Override
    public MudWallRenderState createRenderState() {
        return new MudWallRenderState();
    }

    @Override
    public void extractRenderState(MudWallEntity entity, MudWallRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        state.wallYaw = entity.getWallYaw();
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
    }

    @Override
    public void submit(MudWallRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        int solidLight = Math.max(state.lightCoords, 12583104);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.wallYaw);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            submitNodeCollector.submitModel(
                    this.earthModel,
                    new VisualState(state.ageInTicks, state.progress),
                    poseStack,
                    RenderTypes.entityCutout(JutsuVisualEffectRenderer.TEX_EARTH),
                    solidLight,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null,
                    state.outlineColor
            );
        } finally {
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    protected int getBlockLightLevel(MudWallEntity entity, BlockPos pos) {
        return 15;
    }
}
