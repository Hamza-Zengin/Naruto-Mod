package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class SenbonRenderer extends ArrowRenderer<SenbonEntity, ArrowRenderState> {
   public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/senbon.png");

   public SenbonRenderer(EntityRendererProvider.Context manager) {
      super(manager);
   }

   @Override
   public ArrowRenderState createRenderState() {
      return new ArrowRenderState();
   }

   @Override
   public void submit(ArrowRenderState state, PoseStack matrixStackIn, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      matrixStackIn.pushPose();
      matrixStackIn.rotateDegrees(Axis.YP, state.yRot - 90.0F);
      matrixStackIn.rotateDegrees(Axis.ZP, state.xRot);
      matrixStackIn.scale(0.5F, 0.5F, 0.5F);

      matrixStackIn.rotateDegrees(Axis.XP, 45.0F);
      matrixStackIn.scale(0.05625F, 0.05625F, 0.05625F);
      matrixStackIn.translate(-4.0D, 0.0D, 0.0D);

      int packedLightIn = state.lightCoords;
      submitNodeCollector.submitCustomGeometry(matrixStackIn, RenderTypes.entityCutout(this.getTextureLocation(state)), (basePose, ivertexbuilder) -> {
         PoseStack localStack = new PoseStack();
         localStack.last().set(basePose);
         for (int j = 0; j < 4; ++j) {
            localStack.rotateDegrees(Axis.XP, 90.0F);
            PoseStack.Pose entry = localStack.last();
            Matrix4f matrix4f = entry.pose();
            vertex(matrix4f, entry, ivertexbuilder, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLightIn);
            vertex(matrix4f, entry, ivertexbuilder, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLightIn);
            vertex(matrix4f, entry, ivertexbuilder, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLightIn);
            vertex(matrix4f, entry, ivertexbuilder, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLightIn);
         }
      });

      matrixStackIn.popPose();
   }

   private static void vertex(Matrix4f matrix, PoseStack.Pose pose, VertexConsumer consumer,
                              int x, int y, int z, float u, float v, int nx, int ny, int nz, int light) {
      consumer.addVertex(matrix, (float) x, (float) y, (float) z)
              .setColor(255, 255, 255, 255)
              .setUv(u, v)
              .setOverlay(OverlayTexture.NO_OVERLAY)
              .setLight(light)
              .setNormal(pose, (float) nx, (float) ny, (float) nz);
   }

   @Override
   protected Identifier getTextureLocation(ArrowRenderState state) {
      return RES_ARROW;
   }
}
