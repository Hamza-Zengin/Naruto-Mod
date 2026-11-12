package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WaterVortexModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class WaterBulletRenderer extends EntityRenderer<WaterBulletEntity> {

    private final WaterVortexModel waterModel;

    public WaterBulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.waterModel = new WaterVortexModel(ctx.bakeLayer(NarutoRenderEvents.WATER_VORTEX_LAYER));
    }

    @Override
    public void render(WaterBulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        float progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
        float fadeOut = progress > 0.82F ? (1.0F - ((progress - 0.82F) / 0.18F)) : 1.0F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));

        poseStack.pushPose();
        try {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
            poseStack.translate(0.0D, 0.25D, 0.0D);

            // 1. 3D Fluid Water Core Sphere (r = 0.45F)
            com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel.renderSmoothSphere(
                    poseStack, consumer, 0.45F, 12, 16, 15728880, OverlayTexture.NO_OVERLAY,
                    0.08F, 0.50F, 0.95F, 0.90F * fadeOut
            );

            // 2. Surrounding Swirling Water Vortex Rings
            float scale = 0.85F + (progress > 0.7F ? (progress - 0.7F) * 1.5F : 0.0F);
            poseStack.scale(-scale, -scale, scale);
            this.waterModel.setupAnim(age);
            this.waterModel.renderToBuffer(poseStack, consumer, 15728880, OverlayTexture.NO_OVERLAY, 0.20F, 0.65F, 1.0F, 0.80F * fadeOut);
        } finally {
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterBulletEntity entity) {
        return JutsuVisualEffectRenderer.TEX_WATER;
    }
}
