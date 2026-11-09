package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class FireballJutsuRenderer extends EntityRenderer<FireballJutsuEntity> {
   public static final ResourceLocation FIREBALL_TEX = new ResourceLocation("narutomod", "textures/entity/jutsu/projectiles/fireball.png");
   private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(FIREBALL_TEX);
   private final FireballJutsuModel model;

   public FireballJutsuRenderer(EntityRendererProvider.Context manager) {
      super(manager);
      this.model = new FireballJutsuModel(manager.bakeLayer(NarutoRenderEvents.FIREBALL_LAYER));
   }

   @Override
   public void render(FireballJutsuEntity fireballJutsuEntity, float entityYaw, float partial, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
      VertexConsumer vertexconsumer = multiBufferSource.getBuffer(RENDER_TYPE);
      poseStack.pushPose();
      poseStack.translate(0, fireballJutsuEntity.getBbHeight() / 2f, 0);
      float age = fireballJutsuEntity.time + partial;
      float scale = 1.0F;
      if (fireballJutsuEntity.isImpacted()) {
         scale = 1.15F + (fireballJutsuEntity.getImpactTicks() + partial) * 0.14F;
      } else if (age < FireballJutsuEntity.GROW_TIME) {
         scale = FireballJutsuEntity.INITIAL_SCALE
                 + (FireballJutsuEntity.GROW_SCALE * (age / FireballJutsuEntity.GROW_TIME));
      }
      poseStack.scale(scale, scale, scale);
      this.model.setAnimationAngles(age);
      float alpha = fireballJutsuEntity.isImpacted()
              ? Math.max(0.15F, 0.92F - (fireballJutsuEntity.getImpactTicks() + partial) * 0.042F)
              : 0.92F;
      this.model.renderToBuffer(poseStack, vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
      poseStack.popPose();
      super.render(fireballJutsuEntity, entityYaw, partial, poseStack, multiBufferSource, packedLight);
   }

   @Override
   protected int getBlockLightLevel(FireballJutsuEntity entity, BlockPos pos) {
      return 15;
   }

   @Override
   public ResourceLocation getTextureLocation(FireballJutsuEntity entity) {
      return FIREBALL_TEX;
   }
}

