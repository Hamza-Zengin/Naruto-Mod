package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SusanooRibcageLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation SUSANOO_TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/susanoo_ribcage.png");
    private final SusanooRibcageModel<AbstractClientPlayer> ribcageModel;

    public SusanooRibcageLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.ribcageModel = new SusanooRibcageModel<>(modelSet.bakeLayer(NarutoRenderEvents.SUSANOO_RIBCAGE_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Early Exit: if Susanoo is not active, skip all matrix setups and buffer lookups
            if (!ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId())) {
                return;
            }
            poseStack.pushPose();
            this.getParentModel().copyPropertiesTo(this.ribcageModel);
            this.ribcageModel.body.copyFrom(this.getParentModel().body);

            // Imposing +18% scale to give Susanoo a majestic presence enveloping the player
            poseStack.translate(0.0D, -0.06D, 0.0D);
            poseStack.scale(1.18F, 1.18F, 1.18F);

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(SUSANOO_TEXTURE));
            // Luminous ethereal translucent chakra bone (alpha = 0.55F)
            this.ribcageModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.55F, 0.25F, 0.55F);
            poseStack.popPose();
        });
    }
}
