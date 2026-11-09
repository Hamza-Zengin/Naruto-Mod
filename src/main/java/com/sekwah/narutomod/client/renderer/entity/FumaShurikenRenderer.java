package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.FumaShurikenModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class FumaShurikenRenderer extends EntityRenderer<FumaShurikenEntity> {

    public static final ResourceLocation FUMA_TEX =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/projectiles/fuma_shuriken.png");
    private final FumaShurikenModel model;

    public FumaShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FumaShurikenModel(context.bakeLayer(NarutoRenderEvents.FUMA_SHURIKEN_LAYER));
    }

    @Override
    public void render(FumaShurikenEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.15D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        float spin = Mth.lerp(partialTicks, entity.getPrevRotateTicks(), entity.getRotateTicks()) * 42.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.95F, 0.95F, 0.95F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(FUMA_TEX));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FumaShurikenEntity entity) {
        return FUMA_TEX;
    }
}
