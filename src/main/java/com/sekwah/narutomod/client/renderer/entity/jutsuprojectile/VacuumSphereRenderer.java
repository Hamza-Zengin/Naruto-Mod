package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WindShockwaveModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.sekclib.util.ColorUtil;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class VacuumSphereRenderer extends EntityRenderer<VacuumSphereEntity, VacuumSphereRenderer.VacuumSphereRenderState> {

    private final WindShockwaveModel windModel;

    public static class VacuumSphereRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float progress;
    }

    public VacuumSphereRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.windModel = new WindShockwaveModel(ctx.bakeLayer(NarutoRenderEvents.WIND_SHOCKWAVE_LAYER));
    }

    @Override
    public VacuumSphereRenderState createRenderState() {
        return new VacuumSphereRenderState();
    }

    @Override
    public void extractRenderState(VacuumSphereEntity entity, VacuumSphereRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.age = age;
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
    }

    @Override
    public void submit(VacuumSphereRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        float fadeOut = RenderMathUtil.fadeOut(state.progress, 0.80F);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
            poseStack.rotateDegrees(Axis.XP, state.xRot);
            poseStack.translate(0.0D, 0.22D, 0.0D);

            // Compressed Vacuum Air Core
            float sphereAlpha = 0.82F * fadeOut;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_WIND),
                    (pose, consumer) -> {
                        PoseStack localStack = new PoseStack();
                        localStack.last().set(pose);
                        RasenganSphereModel.renderSmoothSphere(
                                localStack, consumer, 0.28F, 14, 20, RenderMathUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                0.85F, 0.98F, 0.94F, sphereAlpha
                        );
                    }
            );

            // Compact Spinning Wind Blade Rings
            RenderMathUtil.scaleEntityModel(poseStack, 0.42F);
            int ringColor = ColorUtil.argb(0.76F * fadeOut, 0.78F, 1.0F, 0.92F);
            submitNodeCollector.submitModel(
                    this.windModel,
                    new VisualState(state.age, state.progress),
                    poseStack,
                    RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_WIND),
                    RenderMathUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    ringColor,
                    null,
                    state.outlineColor
            );
        } finally {
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
