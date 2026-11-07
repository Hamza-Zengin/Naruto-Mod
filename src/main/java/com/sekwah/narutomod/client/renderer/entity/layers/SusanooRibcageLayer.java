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
            if (ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId())) {
                poseStack.pushPose();
                this.getParentModel().copyPropertiesTo(this.ribcageModel);
                this.ribcageModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(SUSANOO_TEXTURE));
                // Glowing semi-transparent chakra bone with bright luminous emissive lighting
                this.ribcageModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.4F, 0.2F, 0.65F);
                poseStack.popPose();
            }
        });
    }
}
