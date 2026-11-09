package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.RasenshurikenModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class RasenshurikenRenderer extends EntityRenderer<RasenshurikenEntity> {

    public static final ResourceLocation RASENSHURIKEN_TEX =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/projectiles/rasenshuriken.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(RASENSHURIKEN_TEX);
    private final RasenshurikenModel model;

    public RasenshurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RasenshurikenModel(context.bakeLayer(NarutoRenderEvents.RASENSHURIKEN_LAYER));
    }

    @Override
    public void render(RasenshurikenEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.35D, 0.0D);

        float age = entity.time + partialTicks;
        float scale = 1.25F;
        if (entity.isImpacted()) {
            scale = 1.25F + (entity.getVortexTicks() + partialTicks) * 0.12F;
        }
        poseStack.scale(scale, scale, scale);

        this.model.setSpinAngle(age * 0.45F, age * 0.85F);
        this.model.renderToBuffer(poseStack, consumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.88F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected int getBlockLightLevel(RasenshurikenEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(RasenshurikenEntity entity) {
        return RASENSHURIKEN_TEX;
    }
}
