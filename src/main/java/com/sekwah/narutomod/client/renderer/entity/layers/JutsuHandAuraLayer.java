package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChakraScalpelModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChidoriBurstModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EightGatesAuraModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.sekwah.sekclib.util.ColorUtil;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders 3D Jutsu Models directly on the player's hand and torso:
 * - 3D Crackling Chidori Lightning Blade on the right hand during Chidori dash/charge
 * - 3D Swirling Chakra / Rasengan Sphere on the right hand during Rasengan
 * - 3D Teal Medical Chakra Scalpel blade on the right hand during Chakura no Mesu
 * - 3D Eight Gates (Hachimon Tonkō) Crimson/Blue Steam Vortex Rings around the body
 */
public class JutsuHandAuraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final RasenganSphereModel rasenganHandModel;
    private final ChidoriBurstModel chidoriHandModel;
    private final ChakraScalpelModel scalpelHandModel;
    private final EightGatesAuraModel gatesAuraModel;

    public JutsuHandAuraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.rasenganHandModel = new RasenganSphereModel(modelSet.bakeLayer(NarutoRenderEvents.RASENGAN_SPHERE_LAYER));
        this.chidoriHandModel = new ChidoriBurstModel(modelSet.bakeLayer(NarutoRenderEvents.CHIDORI_BURST_LAYER));
        this.scalpelHandModel = new ChakraScalpelModel(modelSet.bakeLayer(NarutoRenderEvents.CHAKRA_SCALPEL_LAYER));
        this.gatesAuraModel = new EightGatesAuraModel(modelSet.bakeLayer(NarutoRenderEvents.EIGHT_GATES_AURA_LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        PlayerModel parentModel = this.getParentModel();
        if (state == null || state.isInvisible || poseStack == null || submitNodeCollector == null || parentModel == null) {
            return;
        }
        NarutoRenderEvents.NinjaRenderStateData data = state.getRenderData(NarutoRenderEvents.NINJA_RENDER_DATA_KEY);
        if (data == null) {
            return;
        }

        String activeHand = data.activeHandJutsu();
        boolean hasChidori = "chidori".equalsIgnoreCase(activeHand);
        boolean hasRasengan = "rasengan".equalsIgnoreCase(activeHand);
        boolean hasScalpel = "chakra_scalpel".equalsIgnoreCase(activeHand);
        boolean hasEightGatesDeath = data.hasEightGatesDeath();
        boolean hasEightGates = data.hasEightGates();
        boolean hasByakugan = data.hasByakugan();
        boolean hasWaterWalk = data.hasWaterWalk();

        // Early Exit: skip all matrix transformations and collector submissions if no aura is active
        if (!hasChidori && !hasRasengan && !hasScalpel && !hasEightGates && !hasByakugan && !hasWaterWalk) {
            return;
        }

        int fullBright = RenderMathUtil.FULL_BRIGHT;
        float ageInTicks = state.ageInTicks;

        // 1. Chidori 3D Hand Lightning Blade (floating in front of palm)
        if (hasChidori) {
            poseStack.pushPose();
            this.getParentModel().rightArm.translateAndRotate(poseStack);
            poseStack.translate(-0.06D, 0.74D, -0.25D);
            RenderMathUtil.scaleUniform(poseStack, 0.32F);
            VisualState chidoriState = new VisualState(ageInTicks, 0.0F);
            submitNodeCollector.submitModel(
                    this.chidoriHandModel,
                    chidoriState,
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHIDORI),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null,
                    state.outlineColor
            );
            submitNodeCollector.submitModel(
                    this.chidoriHandModel,
                    chidoriState,
                    poseStack,
                    RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_CHIDORI),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    ColorUtil.argb(0.55F, 0.45F, 0.85F, 1.0F),
                    null,
                    state.outlineColor
            );
            poseStack.popPose();
        }

        // 2. 3D Swirling Rasengan in hand
        if (hasRasengan) {
            poseStack.pushPose();
            this.getParentModel().rightArm.translateAndRotate(poseStack);
            poseStack.translate(-0.06D, 0.74D, -0.25D);
            RenderMathUtil.scaleUniform(poseStack, 0.28F);
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN),
                    (pose, vc) -> this.rasenganHandModel.renderProcedural(
                            pose, vc, ageInTicks, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.85F
                    )
            );
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_RASENGAN),
                    (pose, vc) -> this.rasenganHandModel.renderProcedural(
                            pose, vc, ageInTicks, fullBright, OverlayTexture.NO_OVERLAY, 0.45F, 0.85F, 1.0F, 0.50F
                    )
            );
            poseStack.popPose();
        }

        // 2b. 3D Surgical Chakra Scalpel on right forearm/hand
        if (hasScalpel) {
            poseStack.pushPose();
            this.getParentModel().rightArm.translateAndRotate(poseStack);
            poseStack.translate(-0.06D, 0.64D, -0.10D);
            RenderMathUtil.scaleUniform(poseStack, 0.52F);
            int scalpelColor = ColorUtil.argb(0.92F, 1.0F, 1.0F, 1.0F);
            VisualState scalpelState = new VisualState(ageInTicks, 0.0F);
            submitNodeCollector.submitModel(
                    this.scalpelHandModel,
                    scalpelState,
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHAKRA_SCALPEL),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    scalpelColor,
                    null,
                    state.outlineColor
            );
            submitNodeCollector.submitModel(
                    this.scalpelHandModel,
                    scalpelState,
                    poseStack,
                    RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_CHAKRA_SCALPEL),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    ColorUtil.argb(0.55F, 0.25F, 0.95F, 0.85F),
                    null,
                    state.outlineColor
            );
            poseStack.popPose();
        }

        // 3. Eight Gates (Hachimon Tonkō) Horizontal Circular Vapor Rings at ground level
        if (hasEightGates) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.48D, 0.0D);
            RenderMathUtil.scaleUniform(poseStack, 0.55F);
            int gatesColor = hasEightGatesDeath
                    ? ColorUtil.argb(0.85F, 0.95F, 0.10F, 0.15F)
                    : ColorUtil.argb(0.80F, 0.35F, 0.80F, 1.0F);
            int gatesGlowColor = hasEightGatesDeath
                    ? ColorUtil.argb(0.55F, 1.0F, 0.18F, 0.20F)
                    : ColorUtil.argb(0.45F, 0.35F, 0.85F, 1.0F);
            VisualState gatesState = new VisualState(ageInTicks, 0.0F);
            submitNodeCollector.submitModel(
                    this.gatesAuraModel,
                    gatesState,
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    gatesColor,
                    null,
                    state.outlineColor
            );
            submitNodeCollector.submitModel(
                    this.gatesAuraModel,
                    gatesState,
                    poseStack,
                    RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_WIND),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    gatesGlowColor,
                    null,
                    state.outlineColor
            );
            poseStack.popPose();
        }

        // 4. Water Walk circular chakra discs on both foot soles
        if (hasWaterWalk) {
            int waterColor = ColorUtil.argb(0.75F, 0.10F, 0.55F, 1.0F);

            // Left foot sole
            poseStack.pushPose();
            this.getParentModel().leftLeg.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 0.76D, 0.0D);
            RenderMathUtil.scaleUniform(poseStack, 0.20F);
            submitNodeCollector.submitModel(
                    this.gatesAuraModel,
                    new VisualState(ageInTicks * 1.5F, 0.0F),
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    waterColor,
                    null,
                    state.outlineColor
            );
            poseStack.popPose();

            // Right foot sole
            poseStack.pushPose();
            this.getParentModel().rightLeg.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 0.76D, 0.0D);
            RenderMathUtil.scaleUniform(poseStack, 0.20F);
            submitNodeCollector.submitModel(
                    this.gatesAuraModel,
                    new VisualState(-ageInTicks * 1.5F, 0.0F),
                    poseStack,
                    RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND),
                    fullBright,
                    OverlayTexture.NO_OVERLAY,
                    waterColor,
                    null,
                    state.outlineColor
            );
            poseStack.popPose();
        }
    }
}
