package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ShurikenRenderer extends EntityRenderer<ShurikenEntity, ShurikenRenderer.ShurikenRenderState> {

   private final ItemModelResolver itemModelResolver;
   private ItemStack renderingItem;

   public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/kunai.png");

   public static class ShurikenRenderState extends EntityRenderState {
      public float yRot;
      public float xRot;
      public float rotOffset;
      public float spinDegrees;
      public final ItemStackRenderState item = new ItemStackRenderState();
   }

   public ShurikenRenderer(EntityRendererProvider.Context manager) {
      super(manager);
      this.itemModelResolver = manager.getItemModelResolver();
   }

   @Override
   public ShurikenRenderState createRenderState() {
      return new ShurikenRenderState();
   }

   @Override
   public void extractRenderState(ShurikenEntity entity, ShurikenRenderState state, float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      float rotateSpeed = -50.0F;
      state.yRot = entity.getYRot(partialTicks);
      state.xRot = entity.getXRot(partialTicks);
      state.rotOffset = entity.getRotOffset();
      state.spinDegrees = Mth.lerp(partialTicks, entity.getPrevRotateTicks() * rotateSpeed, entity.getRotateTicks() * rotateSpeed);
      if (this.renderingItem == null) {
         this.renderingItem = new ItemStack(NarutoItems.SHURIKEN.get());
      }
      this.itemModelResolver.updateForNonLiving(state.item, this.renderingItem, ItemDisplayContext.FIXED, entity);
   }

   @Override
   public void submit(ShurikenRenderState state, PoseStack matrixStackIn, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      matrixStackIn.pushPose();
      matrixStackIn.rotateDegrees(Axis.YP, state.yRot - 90.0F);
      matrixStackIn.rotateDegrees(Axis.ZP, state.xRot);
      matrixStackIn.rotateDegrees(Axis.XP, state.rotOffset);
      matrixStackIn.rotateDegrees(Axis.ZP, state.spinDegrees);
      matrixStackIn.scale(0.5F, 0.5F, 0.5F);
      state.item.submit(matrixStackIn, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      matrixStackIn.popPose();
      super.submit(state, matrixStackIn, submitNodeCollector, cameraRenderState);
   }
}
