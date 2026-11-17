package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.RasenganSphereModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class FireballJutsuRenderer extends EntityRenderer<FireballJutsuEntity, FireballJutsuRenderer.FireballRenderState> {
   public static final Identifier FIREBALL_TEX = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/jutsu/projectiles/fireball.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(FIREBALL_TEX);
   private final FireballJutsuModel model;

   public static class FireballRenderState extends EntityRenderState {
      public float scale = 1.65F;
      public float alpha = 0.92F;
   }

   public FireballJutsuRenderer(EntityRendererProvider.Context manager) {
      super(manager);
      this.model = new FireballJutsuModel(manager.bakeLayer(NarutoRenderEvents.FIREBALL_LAYER));
   }

   @Override
   public FireballRenderState createRenderState() {
      return new FireballRenderState();
   }

   @Override
   public void extractRenderState(FireballJutsuEntity entity, FireballRenderState state, float partial) {
      super.extractRenderState(entity, state, partial);
      float age = entity.time + partial;
      state.ageInTicks = age;
      float scale = 1.0F;
      if (entity.isImpacted()) {
         scale = 1.15F + (entity.getImpactTicks() + partial) * 0.14F;
      } else if (age < FireballJutsuEntity.GROW_TIME) {
         scale = FireballJutsuEntity.INITIAL_SCALE
                 + (FireballJutsuEntity.GROW_SCALE * (age / FireballJutsuEntity.GROW_TIME));
      }
      state.scale = scale * 1.65F;
      state.alpha = entity.isImpacted()
              ? Math.max(0.15F, 0.92F - (entity.getImpactTicks() + partial) * 0.042F)
              : 0.92F;
   }

   @Override
   public void submit(FireballRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      poseStack.pushPose();
      poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
      poseStack.scale(state.scale, state.scale, state.scale);

      // Smooth spherical inner fire glow for silhouette continuity
      submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
         PoseStack local = new PoseStack();
         local.last().set(pose);
         RasenganSphereModel.renderSmoothSphere(
                 local, consumer, 0.42F, 18, 26, RenderMathUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                 1.0F, 0.72F, 0.25F, state.alpha * 0.55F
         );
      });

      int color = ARGB.colorFromFloat(state.alpha, 1.0F, 1.0F, 1.0F);
      submitNodeCollector.submitModel(
              this.model,
              state,
              poseStack,
              RENDER_TYPE,
              15728880,
              OverlayTexture.NO_OVERLAY,
              color,
              null,
              state.outlineColor
      );
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
   }

   @Override
   protected int getBlockLightLevel(FireballJutsuEntity entity, BlockPos pos) {
      return 15;
   }
}
