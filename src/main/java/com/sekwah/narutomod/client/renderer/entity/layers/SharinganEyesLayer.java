package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Renders glowing crimson Sharingan and Byakugan eyes on the player whenever active.
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
        if (data == null || (!data.hasSharingan() && !data.hasByakugan())) {
            return;
        }

        Identifier texture = (data.hasSharingan() && data.eyeTexture() != null)
                ? data.eyeTexture()
                : BYAKUGAN_EYES_TEXTURE;
        renderIrisQuads(poseStack, submitNodeCollector, texture);
    }

    private void renderIrisQuads(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Identifier texture) {
        PlayerModel parentModel = this.getParentModel();
        if (parentModel == null || parentModel.head == null || texture == null) {
            return;
        }
        poseStack.pushPose();
        parentModel.head.translateAndRotate(poseStack);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.eyes(texture), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            float z = -4.003F / 16.0F;
            float yTop = -4.0F / 16.0F;
            float yBottom = -3.0F / 16.0F;
            int fullBright = LightCoordsUtil.FULL_BRIGHT;

            // Right Eye Iris: X = [-2/16 .. -1/16]
            drawEyeQuad(consumer, matrix, pose, -2.0F / 16.0F, -1.0F / 16.0F, yTop, yBottom, z, fullBright);
            // Left Eye Iris: X = [+1/16 .. +2/16]
            drawEyeQuad(consumer, matrix, pose, 1.0F / 16.0F, 2.0F / 16.0F, yTop, yBottom, z, fullBright);
        });

        poseStack.popPose();
    }

    private static void drawEyeQuad(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                                    float x1, float x2, float y1, float y2, float z, int light) {
        consumer.addVertex(matrix, x1, y1, z).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x1, y2, z).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x2, y2, z).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(matrix, x2, y1, z).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
