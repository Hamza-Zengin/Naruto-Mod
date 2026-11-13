package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders glowing crimson Sharingan eyes on the player whenever Sharingan is active.
 * Uses vanilla Minecraft glowing eye shaders (like Enderman/Spider eyes).
 */
public class SharinganEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation SHARINGAN_EYES_TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/sharingan_eyes.png");
    private static final ResourceLocation BYAKUGAN_EYES_TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/byakugan_eyes.png");

    public SharinganEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean sharingan = ninjaData.isSharinganActive();
            boolean byakugan = ninjaData.isByakuganActive();
            // Early Exit: skip buffer lookup and model render if neither dojutsu eye is active
            if (!sharingan && !byakugan) {
                return;
            }
            ResourceLocation texture;
            if (sharingan) {
                com.sekwah.narutomod.abilities.jutsus.SharinganAbility.SharinganStage stage =
                        com.sekwah.narutomod.abilities.jutsus.SharinganAbility.getStage(ninjaData);
                texture = stage.getTexture();
            } else {
                texture = BYAKUGAN_EYES_TEXTURE;
            }
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);

            org.joml.Matrix4f matrix = poseStack.last().pose();
            org.joml.Matrix3f normal = poseStack.last().normal();

            float z = -4.003F / 16.0F;
            float yTop = -4.0F / 16.0F;
            float yBottom = -3.0F / 16.0F;
            int fullBright = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

            // Right Eye Iris: X = [-2/16 .. -1/16]
            drawEyeQuad(consumer, matrix, normal, -2.0F / 16.0F, -1.0F / 16.0F, yTop, yBottom, z, fullBright);
            // Left Eye Iris: X = [+1/16 .. +2/16]
            drawEyeQuad(consumer, matrix, normal, 1.0F / 16.0F, 2.0F / 16.0F, yTop, yBottom, z, fullBright);

            poseStack.popPose();
        });
    }

    private static void drawEyeQuad(VertexConsumer consumer, org.joml.Matrix4f matrix, org.joml.Matrix3f normal,
                                    float x1, float x2, float y1, float y2, float z, int light) {
        consumer.vertex(matrix, x1, y1, z).color(255, 255, 255, 255).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, x1, y2, z).color(255, 255, 255, 255).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, x2, y2, z).color(255, 255, 255, 255).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, x2, y1, z).color(255, 255, 255, 255).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
    }
}
