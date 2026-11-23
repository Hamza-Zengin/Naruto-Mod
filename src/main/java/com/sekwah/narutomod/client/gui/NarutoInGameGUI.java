package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public class NarutoInGameGUI {

    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/vignette.png");

    private final ChakraAndStaminaGUI chakraOverlay;

    public NarutoInGameGUI() {
        Minecraft mc = Minecraft.getInstance();
        this.chakraOverlay = new ChakraAndStaminaGUI(mc);
        NeoForge.EVENT_BUS.addListener(this::renderGameOverlay);
        NeoForge.EVENT_BUS.addListener(this::clientTickEvent);
    }

    @SubscribeEvent
    public void clientTickEvent(ClientTickEvent.Post event) {
        this.chakraOverlay.tick();
    }

    @SubscribeEvent
    public void renderGameOverlay(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            this.chakraOverlay.render(event.getGuiGraphics());
            this.renderSelectedJutsuHudBadge(event.getGuiGraphics());
            JutsuWheelOverlay.render(event.getGuiGraphics());
        } else if (event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) {
            renderFirstPersonAuraVignette(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    /**
     * Renders an atmospheric, pulsing peripheral chakra aura vignette when active abilities
     * are toggled in first-person mode (Susanoo Ribcage, Eight Gates, Byakugan).
     */
    private void renderFirstPersonAuraVignette(GuiGraphicsExtractor guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        var ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
        boolean hasSusanoo = toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId());
        boolean hasEightGatesDeath = ninjaData.isEighthGateActive()
                || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.EIGHT_GATES_DEATH.getId()));
        boolean hasByakugan = toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.BYAKUGAN.getId());

        if (!hasSusanoo && !hasEightGatesDeath && !hasByakugan) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float ticks = mc.player.tickCount + partialTick;

        if (hasSusanoo) {
            float pulse = 0.16F + 0.04F * Mth.sin(ticks * 0.08F);
            int color = ARGB.colorFromFloat(pulse, 1.0F, 0.60F, 0.20F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight, color);
        }

        if (hasEightGatesDeath) {
            float pulse = 0.20F + 0.07F * Mth.sin(ticks * 0.22F);
            int color = ARGB.colorFromFloat(pulse, 1.0F, 0.12F, 0.18F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight, color);
        }

        if (hasByakugan) {
            float pulse = 0.13F + 0.03F * Mth.sin(ticks * 0.06F);
            int color = ARGB.colorFromFloat(pulse, 0.80F, 0.95F, 1.0F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight, color);
        }
    }

    /**
     * Renders a compact HUD badge above the right side of the chakra/hotbar area showing
     * the currently selected Jutsu slot, element accent, cooldown, or toggle status.
     */
    public void renderSelectedJutsuHudBadge(GuiGraphicsExtractor guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        int slot = ninjaData.getSelectedJutsuSlot();
        Identifier selectedId = ninjaData.getEquippedJutsu(slot);
        Ability ability = NarutoAbilities.getAbility(selectedId);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int badgeW = 132;
        int badgeH = 24;
        int badgeX = screenWidth - badgeW - 10;
        int badgeY = screenHeight - badgeH - 10;

        int accent = ability != null ? JutsuWheelOverlay.getElementAccentColor(ability.getChakraElement()) : 0xFF475569;
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xAA0C111A);
        guiGraphics.outline(badgeX, badgeY, badgeW, badgeH, accent);

        // Slot number badge on the left
        String slotTag = "[R] #" + (slot + 1);
        guiGraphics.text(font, slotTag, badgeX + 5, badgeY + 4, 0xFFFBBF24);

        if (ability == null) {
            String noJutsu = Component.translatable("naruto.gui.jutsu_wheel.no_jutsu").getString();
            String clipped = font.width(noJutsu) > badgeW - 10 ? font.plainSubstrByWidth(noJutsu, badgeW - 16) + ".." : noJutsu;
            guiGraphics.text(font, clipped, badgeX + 5, badgeY + 13, 0xFF94A3B8);
            return;
        }

        String jutsuName = Component.translatable(ability.getTranslationKey()).getString();
        String clippedName = font.width(jutsuName) > badgeW - 10 ? font.plainSubstrByWidth(jutsuName, badgeW - 16) + ".." : jutsuName;
        guiGraphics.text(font, clippedName, badgeX + 5, badgeY + 13, accent);

        // Right-aligned status pill
        String statusText;
        int statusColor;
        CooldownTickEvent cd = ninjaData.getCooldownEvents().get(ability.getTranslationKey());
        ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
        boolean isToggled = ability.activationType() == Ability.ActivationType.TOGGLE
                && toggleData != null
                && toggleData.hasToggleAbility(selectedId);

        if (ninjaData.getActiveHandJutsuTicks() > 0 && selectedId != null && selectedId.getPath().equalsIgnoreCase(ninjaData.getActiveHandJutsu())) {
            statusText = String.format(java.util.Locale.ROOT, "%.1fs!", ninjaData.getActiveHandJutsuTicks() / 20.0f);
            statusColor = 0xFFFACC15;
        } else if (cd != null && cd.ticks > 0) {
            statusText = String.format(java.util.Locale.ROOT, "%.1fs", cd.ticks / 20.0f);
            statusColor = 0xFFF87171;
        } else if (isToggled) {
            statusText = "● " + Component.translatable("naruto.hud.jutsu.active").getString();
            statusColor = 0xFF4ADE80;
        } else {
            float cost = ability.getFinalChakraCost(mc.player, ninjaData);
            statusText = cost > 0 ? ((int) Math.ceil(cost)) + " CP" : "Free";
            statusColor = 0xFF38BDF8;
        }

        int sw = font.width(statusText);
        guiGraphics.text(font, statusText, badgeX + badgeW - sw - 5, badgeY + 4, statusColor);
    }
}
