package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WaterVortexModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.sekwah.sekclib.util.ColorUtil;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class WaterBulletRenderer extends EntityRenderer<WaterBulletEntity, WaterBulletRenderer.WaterBulletRenderState> {

    private final WaterVortexModel waterModel;

    public static class WaterBulletRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float progress;
        public boolean isDragon;
    }

    public WaterBulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.waterModel = new WaterVortexModel(ctx.bakeLayer(NarutoRenderEvents.WATER_VORTEX_LAYER));
    }

    @Override
    public WaterBulletRenderState createRenderState() {
        return new WaterBulletRenderState();
    }

    @Override
    public void extractRenderState(WaterBulletEntity entity, WaterBulletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            float targetYaw = WaterBulletEntity.computeYawFromMotion(motion);
            float targetPitch = WaterBulletEntity.computePitchFromMotion(motion);
            if (entity.hasMotionRot()) {
                state.yRot = Mth.rotLerp(partialTicks, entity.getPrevMotionYaw(), targetYaw);
                state.xRot = Mth.lerp(partialTicks, entity.getPrevMotionPitch(), targetPitch);
            } else {
                state.yRot = targetYaw;
                state.xRot = targetPitch;
            }
        } else {
            state.yRot = entity.getYRot(partialTicks);
            state.xRot = entity.getXRot(partialTicks);
        }
        state.age = age;
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
        state.isDragon = entity.isDragon();
    }

    @Override
    public void submit(WaterBulletRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        float fadeOut = RenderMathUtil.fadeOut(state.progress, 0.82F);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
            poseStack.rotateDegrees(Axis.XP, state.xRot);
            poseStack.translate(0.0D, 0.25D, 0.0D);

            // 1. High-resolution Dual-Layer 3D Fluid Water Core Sphere
            float dragonSizeMult = state.isDragon ? 1.65F : 1.0F;
            float sphereAlpha = 0.90F * fadeOut;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_WATER),
                    (pose, consumer) -> {
                        RasenganSphereModel.renderSmoothSphere(
                                pose, consumer, 0.42F * dragonSizeMult, 20, 28, RenderMathUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                0.10F, 0.54F, 0.98F, sphereAlpha
                        );
                        RasenganSphereModel.renderSmoothSphere(
                                pose, consumer, 0.52F * dragonSizeMult, 18, 24, RenderMathUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                0.35F, 0.78F, 1.0F, sphereAlpha * 0.42F
                        );
                    }
            );

            // 2. Surrounding Swirling Water Vortex Rings
            float scale = (state.isDragon ? 1.55F : 0.85F) + (state.progress > 0.7F ? (state.progress - 0.7F) * 1.5F : 0.0F);
            RenderMathUtil.scaleEntityModel(poseStack, scale);
            int vortexColor = state.isDragon
                    ? ColorUtil.argb(0.88F * fadeOut, 0.14F, 0.58F, 1.0F)
                    : ColorUtil.argb(0.80F * fadeOut, 0.20F, 0.65F, 1.0F);
            submitNodeCollector.submitModel(
                    this.waterModel,
                    new VisualState(state.age, state.progress),
                    poseStack,
                    RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_WATER),
                    RenderMathUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    vortexColor,
                    null,
                    state.outlineColor
            );

            if (state.isDragon) {
                poseStack.translate(0.0D, 0.0D, 0.65D);
                submitNodeCollector.submitModel(
                        this.waterModel,
                        new VisualState(state.age + 4.0F, state.progress),
                        poseStack,
                        RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_WATER),
                        RenderMathUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        ColorUtil.argb(0.70F * fadeOut, 0.30F, 0.80F, 1.0F),
                        null,
                        state.outlineColor
                );
            }
        } finally {
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
