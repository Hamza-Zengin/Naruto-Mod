package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class EarthBulletRenderer extends EntityRenderer<EarthBulletEntity, EarthBulletRenderer.EarthBulletRenderState> {

    public static class EarthBulletRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float progress;
        public boolean molten;
    }

    public EarthBulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EarthBulletRenderState createRenderState() {
        return new EarthBulletRenderState();
    }

    @Override
    public void extractRenderState(EarthBulletEntity entity, EarthBulletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.age = age;
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
        state.molten = entity.isMolten();
    }

    @Override
    public void submit(EarthBulletRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        float fadeOut = RenderMathUtil.fadeOut(state.progress, 0.85F);
        int solidLight = RenderMathUtil.clampMinLight(state.lightCoords, RenderMathUtil.MIN_AMBIENT_LIGHT);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
            poseStack.rotateDegrees(Axis.XP, state.xRot);
            poseStack.translate(0.0D, 0.25D, 0.0D);

            // Spin like a jagged earthen boulder
            poseStack.rotateDegrees(Axis.ZP, state.age * 14.0F);
            poseStack.rotateDegrees(Axis.XP, state.age * 11.0F);

            final boolean molten = state.molten;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(JutsuVisualEffectRenderer.TEX_EARTH),
                    (pose, consumer) ->
                            // Faceted rocky boulder (low subdivision counts yield angular rock facets)
                            RasenganSphereModel.renderSmoothSphere(
                                    pose, consumer, 0.45F, 7, 10,
                                    molten ? RenderMathUtil.FULL_BRIGHT : solidLight,
                                    OverlayTexture.NO_OVERLAY,
                                    molten ? 0.92F : 0.72F,
                                    molten ? 0.48F : 0.62F,
                                    molten ? 0.22F : 0.50F,
                                    fadeOut
                            )
            );

            if (molten) {
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucentCull(JutsuVisualEffectRenderer.TEX_AMATERASU),
                        (pose, consumer) ->
                                RasenganSphereModel.renderSmoothSphere(
                                        pose, consumer, 0.56F, 10, 14,
                                        RenderMathUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                        1.0F, 0.38F, 0.08F, 0.55F * fadeOut
                                )
                );
            }
        } finally {
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
