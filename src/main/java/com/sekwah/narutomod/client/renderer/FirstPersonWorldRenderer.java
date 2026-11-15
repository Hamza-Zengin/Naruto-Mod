package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EightGatesAuraModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.VisualState;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Renders 3D World-Space Jutsu Effects in first-person mode:
 * - 3D Translucent Susanoo Skeletal Ribcage enveloping the player's first-person view
 * - 3D Eight Gates (Hachimon Tonkō) Crimson Steam Vortex Ring on the ground at the player's feet
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class FirstPersonWorldRenderer {

    private static final Identifier SUSANOO_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/susanoo_ribcage.png");

    private static SusanooRibcageModel susanooModel;
    private static EightGatesAuraModel gatesAuraModel;

    public static void resetModels() {
        susanooModel = null;
        gatesAuraModel = null;
    }

    private static void initModelsIfNeeded() {
        if (susanooModel == null || gatesAuraModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            if (modelSet != null) {
                susanooModel = new SusanooRibcageModel(modelSet.bakeLayer(NarutoRenderEvents.SUSANOO_RIBCAGE_LAYER));
                gatesAuraModel = new EightGatesAuraModel(modelSet.bakeLayer(NarutoRenderEvents.EIGHT_GATES_AURA_LAYER));
            }
        }
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        INinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
        boolean hasSusanoo = toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId());
        boolean hasEightGatesDeath = ninjaData.isEighthGateActive()
                || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.EIGHT_GATES_DEATH.getId()));
        boolean hasEightGates = ninjaData.isEightGatesActive() || hasEightGatesDeath
                || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId()));
        boolean hasWaterWalk = ninjaData.isWaterWalkActive()
                || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.WATER_WALK.getId()));

        if (!hasSusanoo && !hasEightGates && !hasWaterWalk) {
            return;
        }

        initModelsIfNeeded();
        if (susanooModel == null || gatesAuraModel == null) {
            return;
        }

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        Vec3 camPos = camera.pos;
        float partialTicks = camera.cameraEntityPartialTicks;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector submitNodeCollector = event.getSubmitNodeCollector();

        double px = Mth.lerp(partialTicks, player.xo, player.getX()) - camPos.x;
        double py = Mth.lerp(partialTicks, player.yo, player.getY()) - camPos.y;
        double pz = Mth.lerp(partialTicks, player.zo, player.getZ()) - camPos.z;

        // 1. Susanoo Ribcage surrounding player body in 1st person
        if (hasSusanoo) {
            poseStack.pushPose();
            try {
                poseStack.translate(px, py, pz);

                float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
                poseStack.rotateDegrees(Axis.YP, 180.0F - bodyYaw);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(0.0D, -1.501D - 0.06D, 0.0D);
                poseStack.scale(1.22F, 1.22F, 1.22F);

                HumanoidRenderState susanooState = new HumanoidRenderState();
                susanooState.isCrouching = player.isCrouching();
                int color = ARGB.colorFromFloat(0.50F, 1.0F, 0.55F, 0.25F);
                submitNodeCollector.submitModel(
                        susanooModel,
                        susanooState,
                        poseStack,
                        RenderTypes.entityTranslucent(SUSANOO_TEXTURE),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        color,
                        null,
                        EntityRenderState.NO_OUTLINE
                );
            } finally {
                poseStack.popPose();
            }
        }

        // 2. Eight Gates (Hachimon Tonkō) Horizontal Circular Vapor Rings at ground level around feet
        if (hasEightGates) {
            poseStack.pushPose();
            try {
                poseStack.translate(px, py + 0.05D, pz);
                poseStack.scale(0.65F, 0.65F, 0.65F);

                int color = hasEightGatesDeath
                        ? ARGB.colorFromFloat(0.85F, 0.95F, 0.10F, 0.15F)
                        : ARGB.colorFromFloat(0.80F, 0.35F, 0.80F, 1.0F);
                submitNodeCollector.submitModel(
                        gatesAuraModel,
                        new VisualState(player.tickCount + partialTicks, 0.0F),
                        poseStack,
                        RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        color,
                        null,
                        EntityRenderState.NO_OUTLINE
                );
            } finally {
                poseStack.popPose();
            }
        }

        // 3. Water Walk circular blue chakra aura at player feet
        if (hasWaterWalk) {
            poseStack.pushPose();
            try {
                poseStack.translate(px, py + 0.02D, pz);
                poseStack.scale(0.35F, 0.35F, 0.35F);

                int color = ARGB.colorFromFloat(0.75F, 0.10F, 0.55F, 1.0F);
                submitNodeCollector.submitModel(
                        gatesAuraModel,
                        new VisualState((player.tickCount + partialTicks) * 1.5F, 0.0F),
                        poseStack,
                        RenderTypes.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        color,
                        null,
                        EntityRenderState.NO_OUTLINE
                );
            } finally {
                poseStack.popPose();
            }
        }
    }
}
