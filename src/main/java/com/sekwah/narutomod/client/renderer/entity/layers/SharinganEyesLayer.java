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
            ResourceLocation texture = sharingan ? SHARINGAN_EYES_TEXTURE : BYAKUGAN_EYES_TEXTURE;
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(texture));
            this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });
    }
}
