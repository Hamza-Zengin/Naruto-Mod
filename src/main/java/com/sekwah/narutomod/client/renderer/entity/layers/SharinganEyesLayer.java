package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.SageModeHelper;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Renders Sharingan, Byakugan, and Sage Mode eyes + pigment on the player whenever active.
 */
public class SharinganEyesLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier BYAKUGAN_EYES_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/eyes/byakugan_eyes.png");

    public SharinganEyesLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state == null || state.isInvisible || poseStack == null || submitNodeCollector == null) {
            return;
        }
        NarutoRenderEvents.NinjaRenderStateData data = state.getRenderData(NarutoRenderEvents.NINJA_RENDER_DATA_KEY);
        if (data == null || (!data.hasSharingan() && !data.hasByakugan() && !data.hasSageMode())) {
            return;
        }

        if (data.hasSageMode()) {
            renderSagePigmentQuad(poseStack, submitNodeCollector, packedLight);
            Identifier sageEyeTexture = SageModeHelper.getSageEyeTexture(data.sageModeStage());
            renderIrisQuads(poseStack, submitNodeCollector, RenderTypes.entityTranslucent(sageEyeTexture));
            return;
        }

        Identifier texture = (data.hasSharingan() && data.eyeTexture() != null)
                ? data.eyeTexture()
                : BYAKUGAN_EYES_TEXTURE;
        renderIrisQuads(poseStack, submitNodeCollector, RenderTypes.eyes(texture));
    }

    private void renderSagePigmentQuad(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        PlayerModel parentModel = this.getParentModel();
        if (parentModel == null || parentModel.head == null) {
            return;
        }
        poseStack.pushPose();
        parentModel.head.translateAndRotate(poseStack);

        submitNodeCollector.submitCustomGeometry(poseStack,
                RenderTypes.entityTranslucent(SageModeHelper.SAGE_PIGMENT_TEXTURE), (pose, consumer) -> {
                    Matrix4f matrix = pose.pose();
                    float z = -4.0015F / 16.0F;
                    drawQuad(consumer, matrix, pose,
                            -4.0F / 16.0F, 4.0F / 16.0F,
                            -8.0F / 16.0F, 0.0F,
                            z,
                            0.125F, 0.25F, 0.125F, 0.25F,
                            packedLight);
                });

        poseStack.popPose();
    }

    private void renderIrisQuads(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType) {
        PlayerModel parentModel = this.getParentModel();
        if (parentModel == null || parentModel.head == null || renderType == null) {
            return;
        }
        poseStack.pushPose();
        parentModel.head.translateAndRotate(poseStack);

        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            float z = -4.003F / 16.0F;
            float yTop = -4.0F / 16.0F;
            float yBottom = -3.0F / 16.0F;
            int fullBright = LightCoordsUtil.FULL_BRIGHT;

            // Right Eye Iris: X = [-2/16 .. -1/16]
            drawQuad(consumer, matrix, pose, -2.0F / 16.0F, -1.0F / 16.0F, yTop, yBottom, z,
                    0.0F, 1.0F, 0.0F, 1.0F, fullBright);
            // Left Eye Iris: X = [+1/16 .. +2/16]
            drawQuad(consumer, matrix, pose, 1.0F / 16.0F, 2.0F / 16.0F, yTop, yBottom, z,
                    0.0F, 1.0F, 0.0F, 1.0F, fullBright);
        });

        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                                 float x1, float x2, float y1, float y2, float z,
                                 float u1, float u2, float v1, float v2, int light) {
        consumer.addVertex(matrix, x1, y1, z).setColor(255, 255, 255, 255).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x1, y2, z).setColor(255, 255, 255, 255).setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x2, y2, z).setColor(255, 255, 255, 255).setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x2, y1, z).setColor(255, 255, 255, 255).setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
