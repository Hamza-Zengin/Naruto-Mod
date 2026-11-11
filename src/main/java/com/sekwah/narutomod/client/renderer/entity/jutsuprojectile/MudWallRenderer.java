package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EarthWallModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MudWallRenderer extends EntityRenderer<MudWallEntity> {

    private final EarthWallModel earthModel;

    public MudWallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.earthModel = new EarthWallModel(ctx.bakeLayer(NarutoRenderEvents.EARTH_WALL_LAYER));
    }

    @Override
    public void render(MudWallEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        float progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
        int solidLight = Math.max(packedLight, 12583104);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        poseStack.pushPose();
        try {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getWallYaw()));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            this.earthModel.setupAnim(progress);
            this.earthModel.renderToBuffer(poseStack, consumer, solidLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected int getBlockLightLevel(MudWallEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(MudWallEntity entity) {
        return JutsuVisualEffectRenderer.TEX_EARTH;
    }
}
