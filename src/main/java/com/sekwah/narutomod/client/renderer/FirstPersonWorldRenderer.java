package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EightGatesAuraModel;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders 3D World-Space Jutsu Effects in first-person mode:
 * - 3D Translucent Susanoo Skeletal Ribcage enveloping the player's first-person view
 * - 3D Eight Gates (Hachimon Tonkō) Crimson Steam Vortex Ring on the ground at the player's feet
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirstPersonWorldRenderer {

    private static final ResourceLocation SUSANOO_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/susanoo_ribcage.png");

    private static SusanooRibcageModel<LocalPlayer> susanooModel;
    private static EightGatesAuraModel gatesAuraModel;

    public static void resetModels() {
        susanooModel = null;
        gatesAuraModel = null;
    }

    private static void initModelsIfNeeded() {
        if (susanooModel == null || gatesAuraModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            if (modelSet != null) {
                susanooModel = new SusanooRibcageModel<>(modelSet.bakeLayer(NarutoRenderEvents.SUSANOO_RIBCAGE_LAYER));
                gatesAuraModel = new EightGatesAuraModel(modelSet.bakeLayer(NarutoRenderEvents.EIGHT_GATES_AURA_LAYER));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean hasSusanoo = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId());
            boolean hasEightGates = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId());

            if (!hasSusanoo && !hasEightGates) {
                return;
            }

            initModelsIfNeeded();
            if (susanooModel == null || gatesAuraModel == null) {
                return;
            }

            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();
            float partialTicks = event.getPartialTick();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            double px = Mth.lerp(partialTicks, player.xo, player.getX()) - camPos.x;
            double py = Mth.lerp(partialTicks, player.yo, player.getY()) - camPos.y;
            double pz = Mth.lerp(partialTicks, player.zo, player.getZ()) - camPos.z;

            // 1. Susanoo Ribcage surrounding player body in 1st person
            if (hasSusanoo) {
                poseStack.pushPose();
                poseStack.translate(px, py, pz);

                float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(0.0D, -1.501D - 0.06D, 0.0D);
                poseStack.scale(1.22F, 1.22F, 1.22F);

                VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(SUSANOO_TEXTURE));
                susanooModel.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.55F, 0.25F, 0.50F);
                bufferSource.endBatch(RenderType.entityTranslucent(SUSANOO_TEXTURE));
                poseStack.popPose();
            }

            // 2. Eight Gates (Hachimon Tonkō) Crimson Steam Ring at ground level around feet
            if (hasEightGates) {
                poseStack.pushPose();
                poseStack.translate(px, py + 0.05D, pz);
                poseStack.scale(0.65F, 0.65F, 0.65F);
                gatesAuraModel.setupAnim(player.tickCount + partialTicks);

                VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND));
                gatesAuraModel.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.15F, 0.15F, 0.40F);
                bufferSource.endBatch(RenderType.entityTranslucent(JutsuVisualEffectRenderer.TEX_WIND));
                poseStack.popPose();
            }
        });
    }
}
