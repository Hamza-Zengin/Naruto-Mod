package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WoodPrisonModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class WoodPrisonRenderer extends EntityRenderer<WoodPrisonEntity, WoodPrisonRenderer.WoodPrisonRenderState> {

    private final WoodPrisonModel woodModel;

    public static class WoodPrisonRenderState extends EntityRenderState {
        public float cageYaw;
        public float age;
        public float progress;
    }

    public WoodPrisonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.woodModel = new WoodPrisonModel(ctx.bakeLayer(NarutoRenderEvents.WOOD_PRISON_LAYER));
    }

    @Override
    public WoodPrisonRenderState createRenderState() {
        return new WoodPrisonRenderState();
    }

    @Override
    public void extractRenderState(WoodPrisonEntity entity, WoodPrisonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        state.cageYaw = entity.getCageYaw();
        state.age = age;
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
    }

    @Override
    public void submit(WoodPrisonRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        int solidLight = Math.max(state.lightCoords, 12583104);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.cageYaw);
            float scale = 1.25F;
            poseStack.scale(-scale, -scale, scale);
            submitNodeCollector.submitModel(
                    this.woodModel,
                    new VisualState(state.age, state.progress),
                    poseStack,
                    RenderTypes.entityCutout(JutsuVisualEffectRenderer.TEX_WOOD),
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
}
