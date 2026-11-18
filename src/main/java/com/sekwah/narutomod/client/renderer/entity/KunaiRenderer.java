package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KunaiRenderer extends EntityRenderer<KunaiEntity, KunaiRenderer.KunaiRenderState> {
   public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/kunai.png");

   private final ItemModelResolver itemModelResolver;
   private ItemStack kunaiStack;

   public static class KunaiRenderState extends EntityRenderState {
      public final ItemStackRenderState item = new ItemStackRenderState();
      public float yRot;
      public float xRot;
   }

   public KunaiRenderer(EntityRendererProvider.Context manager) {
      super(manager);
      this.itemModelResolver = manager.getItemModelResolver();
   }

   @Override
   public KunaiRenderState createRenderState() {
      return new KunaiRenderState();
   }

   @Override
   public void extractRenderState(KunaiEntity entity, KunaiRenderState state, float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.yRot = entity.getYRot(partialTicks);
      state.xRot = entity.getXRot(partialTicks);
      if (this.kunaiStack == null) {
         this.kunaiStack = new ItemStack(NarutoItems.KUNAI.get());
      }
      this.itemModelResolver.updateForNonLiving(state.item, this.kunaiStack, ItemDisplayContext.NONE, entity);
   }

   @Override
   public void submit(KunaiRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      poseStack.pushPose();
      poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
      poseStack.rotateDegrees(Axis.ZP, state.xRot - 90.0F);
      poseStack.scale(0.85F, 0.85F, 0.85F);
      state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
   }
}
