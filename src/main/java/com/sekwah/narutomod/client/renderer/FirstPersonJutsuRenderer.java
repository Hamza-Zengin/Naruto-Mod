package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChakraScalpelModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChidoriBurstModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import com.sekwah.sekclib.util.ColorUtil;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Renders 3D Jutsu Models directly onto the player's hand in first-person view:
 * - 3D Crackling Chidori Lightning Blade
 * - 3D Swirling Chakra / Rasengan Sphere
 * - 3D Teal Medical Chakra Scalpel Blade
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class FirstPersonJutsuRenderer {

    private static RasenganSphereModel rasenganModel;
    private static ChidoriBurstModel chidoriModel;
    private static ChakraScalpelModel scalpelModel;

    public static void resetModels() {
        rasenganModel = null;
        chidoriModel = null;
        scalpelModel = null;
    }

    private static void initModelsIfNeeded() {
        if (rasenganModel == null || chidoriModel == null || scalpelModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            if (modelSet != null) {
                rasenganModel = new RasenganSphereModel(modelSet.bakeLayer(NarutoRenderEvents.RASENGAN_SPHERE_LAYER));
                chidoriModel = new ChidoriBurstModel(modelSet.bakeLayer(NarutoRenderEvents.CHIDORI_BURST_LAYER));
                scalpelModel = new ChakraScalpelModel(modelSet.bakeLayer(NarutoRenderEvents.CHAKRA_SCALPEL_LAYER));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) {
            return;
        }

        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }

        INinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        initModelsIfNeeded();
        if (rasenganModel == null || chidoriModel == null || scalpelModel == null) {
            return;
        }

        int fullBright = RenderMathUtil.FULL_BRIGHT;
        float partialTicks = event.getPartialTick();
        float ageInTicks = player.tickCount + partialTicks;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector submitNodeCollector = event.getSubmitNodeCollector();
        InteractionHand hand = event.getHand();

        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        String activeHandJutsu = ninjaData.getActiveHandJutsu();

        // 1. Chidori 3D Lightning Blade on main hand (floating in front of palm)
        if (isMainHand && "chidori".equalsIgnoreCase(activeHandJutsu)) {
            poseStack.pushPose();
            try {
                applyFirstPersonArmTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                poseStack.translate(arm == HumanoidArm.RIGHT ? -0.08D : 0.08D, 0.24D, -0.28D);
                RenderMathUtil.scaleUniform(poseStack, 0.34F);
                VisualState chidoriState = new VisualState(ageInTicks, 0.0F);
                submitNodeCollector.submitModel(
                        chidoriModel,
                        chidoriState,
                        poseStack,
                        RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHIDORI),
                        fullBright,
                        OverlayTexture.NO_OVERLAY,
                        -1,
                        null,
                        EntityRenderState.NO_OUTLINE
                );
                submitNodeCollector.submitModel(
                        chidoriModel,
                        chidoriState,
                        poseStack,
                        RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_CHIDORI),
                        fullBright,
                        OverlayTexture.NO_OVERLAY,
                        ColorUtil.argb(0.55F, 0.45F, 0.85F, 1.0F),
                        null,
                        EntityRenderState.NO_OUTLINE
                );
            } finally {
                poseStack.popPose();
            }
        }

        // 2. Swirling Rasengan Sphere on main hand (floating above palm)
        boolean hasRasengan = "rasengan".equalsIgnoreCase(activeHandJutsu);
        if (isMainHand && hasRasengan) {
            poseStack.pushPose();
            try {
                applyFirstPersonArmTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                poseStack.translate(arm == HumanoidArm.RIGHT ? -0.08D : 0.08D, 0.24D, -0.28D);
                RenderMathUtil.scaleUniform(poseStack, 0.30F);
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN),
                        (pose, vc) -> rasenganModel.renderProcedural(
                                pose, vc, ageInTicks, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.90F
                        )
                );
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_RASENGAN),
                        (pose, vc) -> rasenganModel.renderProcedural(
                                pose, vc, ageInTicks, fullBright, OverlayTexture.NO_OVERLAY, 0.45F, 0.85F, 1.0F, 0.50F
                        )
                );
            } finally {
                poseStack.popPose();
            }
        }

        // 3. Surgical Chakra Scalpel on main hand
        if (isMainHand && "chakra_scalpel".equalsIgnoreCase(activeHandJutsu)) {
            poseStack.pushPose();
            try {
                applyFirstPersonArmTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                poseStack.translate(arm == HumanoidArm.RIGHT ? -0.06D : 0.06D, 0.16D, -0.14D);
                RenderMathUtil.scaleUniform(poseStack, 0.48F);
                VisualState scalpelState = new VisualState(ageInTicks, 0.0F);
                submitNodeCollector.submitModel(
                        scalpelModel,
                        scalpelState,
                        poseStack,
                        RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHAKRA_SCALPEL),
                        fullBright,
                        OverlayTexture.NO_OVERLAY,
                        ColorUtil.argb(0.92F, 1.0F, 1.0F, 1.0F),
                        null,
                        EntityRenderState.NO_OUTLINE
                );
                submitNodeCollector.submitModel(
                        scalpelModel,
                        scalpelState,
                        poseStack,
                        RenderTypes.eyes(JutsuVisualEffectRenderer.TEX_CHAKRA_SCALPEL),
                        fullBright,
                        OverlayTexture.NO_OVERLAY,
                        ColorUtil.argb(0.55F, 0.25F, 0.95F, 0.85F),
                        null,
                        EntityRenderState.NO_OUTLINE
                );
            } finally {
                poseStack.popPose();
            }
        }
    }

    /**
     * Translates and rotates the PoseStack from the first-person camera space
     * into the local coordinate system of the player's hand / palm.
     */
    public static void applyFirstPersonArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProgress, float swingProgress) {
        boolean isRight = arm == HumanoidArm.RIGHT;
        float f = isRight ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float) Math.PI * 2F));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);

        poseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equipProgress * -0.6F, f4 + -0.71999997F);
        poseStack.rotateDegrees(Axis.YP, f * 45.0F);
        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        poseStack.rotateDegrees(Axis.YP, f * f6 * 70.0F);
        poseStack.rotateDegrees(Axis.ZP, f * f5 * -20.0F);

        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.rotateDegrees(Axis.ZP, f * 120.0F);
        poseStack.rotateDegrees(Axis.XP, 200.0F);
        poseStack.rotateDegrees(Axis.YP, f * -135.0F);
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);

        // Move from arm root down to the palm / fist center
        poseStack.translate(f * -0.375D, 0.68D, -0.05D);
    }
}
