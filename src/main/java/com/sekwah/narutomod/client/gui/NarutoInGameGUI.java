package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NarutoInGameGUI {

    private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");

    private final ChakraAndStaminaGUI chakraOverlay;

    public NarutoInGameGUI() {
        Minecraft mc = Minecraft.getInstance();
        this.chakraOverlay = new ChakraAndStaminaGUI(mc);
        MinecraftForge.EVENT_BUS.addListener(this::renderGameOverlay);
        MinecraftForge.EVENT_BUS.addListener(this::clientTickEvent);
    }

    @SubscribeEvent
    public void clientTickEvent(TickEvent.ClientTickEvent event) {
        this.chakraOverlay.tick();
    }

    @SubscribeEvent
    public void renderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            this.chakraOverlay.render(event.getGuiGraphics());
        } else if (event.getOverlay() == VanillaGuiOverlay.VIGNETTE.type()) {
            renderFirstPersonAuraVignette(event.getGuiGraphics());
        }
    }

    /**
     * Renders an atmospheric, pulsing peripheral chakra aura vignette when active abilities
     * are toggled in first-person mode (Susanoo Ribcage, Eight Gates, Byakugan).
     */
    private void renderFirstPersonAuraVignette(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean hasSusanoo = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId());
            boolean hasEightGates = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId());
            boolean hasByakugan = ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.BYAKUGAN.getId());

            if (!hasSusanoo && !hasEightGates && !hasByakugan) {
                return;
            }

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            float ticks = mc.player.tickCount + mc.getFrameTime();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 1. Susanoo Amber/Gold Ethereal Protective Vignette
            if (hasSusanoo) {
                float pulse = 0.16F + 0.04F * Mth.sin(ticks * 0.08F);
                RenderSystem.setShaderColor(1.0F, 0.60F, 0.20F, pulse);
                guiGraphics.blit(VIGNETTE_LOCATION, 0, 0, -90, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
            }

            // 2. Eight Gates Crimson Steam Heartbeat Vignette
            if (hasEightGates) {
                float pulse = 0.20F + 0.07F * Mth.sin(ticks * 0.22F);
                RenderSystem.setShaderColor(1.0F, 0.12F, 0.18F, pulse);
                guiGraphics.blit(VIGNETTE_LOCATION, 0, 0, -90, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
            }

            // 3. Byakugan Sensory Pale Blue/White Vignette
            if (hasByakugan) {
                float pulse = 0.13F + 0.03F * Mth.sin(ticks * 0.06F);
                RenderSystem.setShaderColor(0.80F, 0.95F, 1.0F, pulse);
                guiGraphics.blit(VIGNETTE_LOCATION, 0, 0, -90, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        });
    }
}
