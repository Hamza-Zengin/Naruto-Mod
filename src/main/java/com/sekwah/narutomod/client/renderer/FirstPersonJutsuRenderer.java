package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChidoriBurstModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders 3D Jutsu Models directly onto the player's hand in first-person view:
 * - 3D Crackling Chidori Lightning Blade
 * - 3D Swirling Chakra / Rasengan Sphere (during Rasengan or Chakra Charge)
 * - 3D Gentle Fist (Jūken) Chakra Spheres on palms during Byakugan
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirstPersonJutsuRenderer {

    private static RasenganSphereModel rasenganModel;
    private static ChidoriBurstModel chidoriModel;

    public static void resetModels() {
        rasenganModel = null;
        chidoriModel = null;
    }

    private static void initModelsIfNeeded() {
        if (rasenganModel == null || chidoriModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            if (modelSet != null) {
                rasenganModel = new RasenganSphereModel(modelSet.bakeLayer(NarutoRenderEvents.RASENGAN_SPHERE_LAYER));
                chidoriModel = new ChidoriBurstModel(modelSet.bakeLayer(NarutoRenderEvents.CHIDORI_BURST_LAYER));
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

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            initModelsIfNeeded();
            if (rasenganModel == null || chidoriModel == null) {
                return;
            }

            int fullBright = 15728880;
            float partialTicks = event.getPartialTick();
            float ageInTicks = player.tickCount + partialTicks;
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffer = event.getMultiBufferSource();
            InteractionHand hand = event.getHand();

            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

            // 1. Chidori 3D Lightning Blade on main hand (floating in front of palm)
            if (isMainHand && "chidori".equalsIgnoreCase(ninjaData.getActiveHandJutsu())) {
                poseStack.pushPose();
                try {
                    applyFirstPersonArmTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                    poseStack.translate(arm == HumanoidArm.RIGHT ? -0.08D : 0.08D, 0.24D, -0.28D);
                    poseStack.scale(0.34F, 0.34F, 0.34F);
                    chidoriModel.setupAnim(ageInTicks);
                    VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_CHIDORI));
                    chidoriModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                } finally {
                    poseStack.popPose();
                }
            }

            // 2. Swirling Rasengan Sphere on main hand (floating above palm; Chakra Charge hand model removed)
            boolean hasRasengan = "rasengan".equalsIgnoreCase(ninjaData.getActiveHandJutsu());
            if (isMainHand && hasRasengan) {
                poseStack.pushPose();
                try {
                    applyFirstPersonArmTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                    poseStack.translate(arm == HumanoidArm.RIGHT ? -0.08D : 0.08D, 0.24D, -0.28D);
                    poseStack.scale(0.30F, 0.30F, 0.30F);
                    rasenganModel.setupAnim(ageInTicks);
                    VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_RASENGAN));
                    rasenganModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.90F);
                } finally {
                    poseStack.popPose();
                }
            }

        });
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
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));

        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);

        // Move from arm root down to the palm / fist center
        poseStack.translate(f * -0.375D, 0.68D, -0.05D);
    }
}
