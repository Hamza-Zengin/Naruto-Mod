package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChidoriBurstModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EightGatesAuraModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Renders 3D Jutsu Models directly on the player's hand and torso:
 * - 3D Crackling Chidori Lightning Blade on the right hand during Chidori dash/charge
 * - 3D Swirling Chakra / Rasengan Sphere on the right hand during Chakra Charge
 * - 3D Eight Gates (Hachimon Tonkō) Crimson Steam Vortex Rings around the body
 */
public class JutsuHandAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final RasenganSphereModel rasenganHandModel;
    private final ChidoriBurstModel chidoriHandModel;
    private final EightGatesAuraModel gatesAuraModel;

    public JutsuHandAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.rasenganHandModel = new RasenganSphereModel(modelSet.bakeLayer(NarutoRenderEvents.RASENGAN_SPHERE_LAYER));
        this.chidoriHandModel = new ChidoriBurstModel(modelSet.bakeLayer(NarutoRenderEvents.CHIDORI_BURST_LAYER));
        this.gatesAuraModel = new EightGatesAuraModel(modelSet.bakeLayer(NarutoRenderEvents.EIGHT_GATES_AURA_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            String activeHand = ninjaData.getActiveHandJutsu();
            boolean hasChidori = "chidori".equalsIgnoreCase(activeHand);
            boolean hasRasengan = "rasengan".equalsIgnoreCase(activeHand);
            boolean hasEightGatesDeath = ninjaData.isEighthGateActive()
                    || (ninjaData.getToggleAbilityData() != null && ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.EIGHT_GATES_DEATH.getId()));
            boolean hasEightGates = ninjaData.isEightGatesActive() || hasEightGatesDeath
                    || (ninjaData.getToggleAbilityData() != null && ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId()));
            boolean hasByakugan = ninjaData.isByakuganActive();
            boolean hasWaterWalk = ninjaData.isWaterWalkActive()
                    || (ninjaData.getToggleAbilityData() != null && ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.WATER_WALK.getId()));

            // Early Exit: skip all matrix transformations and buffer allocations if no aura is active
            if (!hasChidori && !hasRasengan && !hasEightGates && !hasByakugan && !hasWaterWalk) {
                return;
            }

            int fullBright = 15728880;

            // 1. Chidori 3D Hand Lightning Blade (floating in front of palm)
            if (hasChidori) {
                poseStack.pushPose();
                this.getParentModel().rightArm.translateAndRotate(poseStack);
                poseStack.translate(-0.06D, 0.74D, -0.25D);
                poseStack.scale(0.32F, 0.32F, 0.32F);
                this.chidoriHandModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHIDORI));
                this.chidoriHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.90F);
                poseStack.popPose();
            }

            // 2. 3D Swirling Rasengan in hand (floating above palm; Chakra Charge hand model removed)
            if (hasRasengan) {
                poseStack.pushPose();
                this.getParentModel().rightArm.translateAndRotate(poseStack);
                poseStack.translate(-0.06D, 0.74D, -0.25D);
                poseStack.scale(0.28F, 0.28F, 0.28F);
                this.rasenganHandModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN));
                this.rasenganHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.85F);
                poseStack.popPose();
            }

            // 3. Eight Gates (Hachimon Tonkō) Horizontal Circular Vapor Rings at ground level
            if (hasEightGates) {
                poseStack.pushPose();
                poseStack.translate(0.0D, 1.48D, 0.0D);
                poseStack.scale(0.55F, 0.55F, 0.55F);
                this.gatesAuraModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND));
                if (hasEightGatesDeath) {
                    // 8th Gate (Shinomon): Crimson blood steam
                    this.gatesAuraModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.95F, 0.10F, 0.15F, 0.85F);
                } else {
                    // Gates 1-7: Light Blue chakra (#59CCFF)
                    this.gatesAuraModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.35F, 0.80F, 1.0F, 0.80F);
                }
                poseStack.popPose();
            }

            // 4. Water Walk circular chakra discs on both foot soles
            if (hasWaterWalk) {
                // Left foot sole
                poseStack.pushPose();
                this.getParentModel().leftLeg.translateAndRotate(poseStack);
                poseStack.translate(0.0D, 0.76D, 0.0D);
                poseStack.scale(0.20F, 0.20F, 0.20F);
                this.gatesAuraModel.setupAnim(ageInTicks * 1.5F);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND));
                this.gatesAuraModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.10F, 0.55F, 1.0F, 0.75F);
                poseStack.popPose();

                // Right foot sole
                poseStack.pushPose();
                this.getParentModel().rightLeg.translateAndRotate(poseStack);
                poseStack.translate(0.0D, 0.76D, 0.0D);
                poseStack.scale(0.20F, 0.20F, 0.20F);
                this.gatesAuraModel.setupAnim(-ageInTicks * 1.5F);
                this.gatesAuraModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.10F, 0.55F, 1.0F, 0.75F);
                poseStack.popPose();
            }

        });
    }
}
