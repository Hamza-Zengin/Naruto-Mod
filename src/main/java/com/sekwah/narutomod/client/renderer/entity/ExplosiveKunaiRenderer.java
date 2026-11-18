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

public class ExplosiveKunaiRenderer extends EntityRenderer<KunaiEntity, ExplosiveKunaiRenderer.ExplosiveKunaiRenderState> {
   public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/explosive_kunai.png");

   private final ItemModelResolver itemModelResolver;
   private ItemStack explosiveKunaiStack;

   public static class ExplosiveKunaiRenderState extends EntityRenderState {
      public final ItemStackRenderState item = new ItemStackRenderState();
      public float yRot;
      public float xRot;
   }

   public ExplosiveKunaiRenderer(EntityRendererProvider.Context manager) {
      super(manager);
      this.itemModelResolver = manager.getItemModelResolver();
   }

   @Override
   public ExplosiveKunaiRenderState createRenderState() {
      return new ExplosiveKunaiRenderState();
   }

   @Override
   public void extractRenderState(KunaiEntity entity, ExplosiveKunaiRenderState state, float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.yRot = entity.getYRot(partialTicks);
      state.xRot = entity.getXRot(partialTicks);
      if (this.explosiveKunaiStack == null) {
         this.explosiveKunaiStack = new ItemStack(NarutoItems.EXPLOSIVE_KUNAI.get());
      }
      this.itemModelResolver.updateForNonLiving(state.item, this.explosiveKunaiStack, ItemDisplayContext.NONE, entity);
   }

   @Override
   public void submit(ExplosiveKunaiRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      poseStack.pushPose();
      poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
      poseStack.rotateDegrees(Axis.ZP, state.xRot - 90.0F);
      poseStack.scale(0.85F, 0.85F, 0.85F);
      state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
   }
}
