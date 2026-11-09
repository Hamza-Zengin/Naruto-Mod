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
            int fullBright = 15728880;

            // 1. Chidori 3D Hand Lightning Blade (active while holding Chidori)
            if ("chidori".equalsIgnoreCase(ninjaData.getActiveHandJutsu())) {
                poseStack.pushPose();
                this.getParentModel().rightArm.translateAndRotate(poseStack);
                poseStack.translate(-0.06D, 0.68D, -0.12D);
                poseStack.scale(0.32F, 0.32F, 0.32F);
                this.chidoriHandModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHIDORI));
                this.chidoriHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.90F);
                poseStack.popPose();
            }

            // 2. 3D Swirling Chakra / Rasengan Sphere in hand (active while holding Rasengan or Chakra Charge)
            if ("rasengan".equalsIgnoreCase(ninjaData.getActiveHandJutsu())
                    || (ninjaData.getCurrentlyChanneledAbility() != null
                    && ninjaData.getCurrentlyChanneledAbility().equals(NarutoAbilities.CHAKRA_CHARGE.getId()))) {
                poseStack.pushPose();
                this.getParentModel().rightArm.translateAndRotate(poseStack);
                poseStack.translate(-0.06D, 0.65D, -0.15D);
                poseStack.scale(0.28F, 0.28F, 0.28F);
                this.rasenganHandModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN));
                this.rasenganHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.85F);
                poseStack.popPose();
            }

            // 3. Eight Gates (Hachimon Tonkō) Horizontal Crimson Steam Ring at ground level
            boolean hasEightGates = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId());
            if (hasEightGates) {
                poseStack.pushPose();
                poseStack.translate(0.0D, 1.48D, 0.0D);
                poseStack.scale(0.55F, 0.55F, 0.55F);
                this.gatesAuraModel.setupAnim(ageInTicks);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND));
                this.gatesAuraModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 0.15F, 0.15F, 0.35F);
                poseStack.popPose();
            }

            // 4. Byakugan 3D Gentle Fist (Jūken) Chakra Sphere on both palms
            if (ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.BYAKUGAN.getId())) {
                poseStack.pushPose();
                this.getParentModel().rightArm.translateAndRotate(poseStack);
                poseStack.translate(-0.06D, 0.64D, -0.10D);
                poseStack.scale(0.24F, 0.24F, 0.24F);
                this.rasenganHandModel.setupAnim(ageInTicks * 0.8F);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN));
                this.rasenganHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.85F, 0.95F, 1.0F, 0.65F);
                poseStack.popPose();

                poseStack.pushPose();
                this.getParentModel().leftArm.translateAndRotate(poseStack);
                poseStack.translate(0.06D, 0.64D, -0.10D);
                poseStack.scale(0.24F, 0.24F, 0.24F);
                this.rasenganHandModel.setupAnim(-ageInTicks * 0.8F);
                this.rasenganHandModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0.85F, 0.95F, 1.0F, 0.65F);
                poseStack.popPose();
            }
        });
    }
}
