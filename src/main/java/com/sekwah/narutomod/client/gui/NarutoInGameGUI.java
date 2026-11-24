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

    static int clampMaxBadgeWidth(int screenWidth) {
        return Math.min(180, Math.max(64, (int) (screenWidth * 0.40f)));
    }

    static int[] computeBadgePosition(int screenWidth, int screenHeight, int pillW, int pillH) {
        int pillX = Math.max(8, screenWidth - pillW - 8);
        // Stamina value text is drawn at screenHeight - 26 with 1px outline (top at screenHeight - 27).
        int pillY = Math.max(8, screenHeight - 31 - pillH);
        return new int[]{pillX, pillY};
    }

    /**
     * Renders a minimalist floating HUD pill in the bottom-right corner (above the stamina bar)
     * showing an element-colored dot, the active jutsu name, and optional cooldown/toggle status,
     * or a compact empty indicator when no jutsu is equipped.
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
        if (ability != null && !ability.isWheelEquippable()) {
            ability = null;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int accent = ability != null ? JutsuWheelOverlay.getElementAccentColor(ability.getChakraElement()) : 0xFF64748B;
        String dot = ability != null ? "● " : "○ ";
        String labelText;
        int labelColor;
        String suffixText = "";
        int suffixColor = 0xFFFFFFFF;

        if (ability == null) {
            labelText = "—";
            labelColor = 0xFF94A3B8;
        } else {
            labelText = Component.translatable(ability.getTranslationKey()).getString();
            labelColor = 0xFFF1F5F9;

            CooldownTickEvent cd = ninjaData.getCooldownEvents().get(ability.getTranslationKey());
            ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
            boolean isToggled = ability.activationType() == Ability.ActivationType.TOGGLE
                    && toggleData != null
                    && toggleData.hasToggleAbility(selectedId);

            if (ninjaData.getActiveHandJutsuTicks() > 0 && selectedId != null && selectedId.getPath().equalsIgnoreCase(ninjaData.getActiveHandJutsu())) {
                suffixText = " " + String.format(java.util.Locale.ROOT, "%.1fs", ninjaData.getActiveHandJutsuTicks() / 20.0f);
                suffixColor = 0xFFFACC15;
            } else if (cd != null && cd.ticks > 0) {
                suffixText = " " + String.format(java.util.Locale.ROOT, "%.1fs", cd.ticks / 20.0f);
                suffixColor = 0xFFF87171;
            } else if (isToggled) {
                suffixText = " •";
                suffixColor = 0xFF4ADE80;
            }
        }

        int padX = 6;
        int dotW = font.width(dot);
        int suffixW = suffixText.isEmpty() ? 0 : font.width(suffixText);
        int maxPillW = clampMaxBadgeWidth(screenWidth);
        int maxLabelW = Math.max(16, maxPillW - (padX * 2 + dotW + suffixW));
        int labelW = font.width(labelText);
        if (labelW > maxLabelW) {
            int ellipsisW = font.width("...");
            if (maxLabelW > ellipsisW) {
                labelText = font.plainSubstrByWidth(labelText, maxLabelW - ellipsisW) + "...";
            } else {
                labelText = font.plainSubstrByWidth(labelText, maxLabelW);
            }
            labelW = font.width(labelText);
        }

        int pillW = padX * 2 + dotW + labelW + suffixW;
        int pillH = 14;
        int[] pos = computeBadgePosition(screenWidth, screenHeight, pillW, pillH);
        int pillX = pos[0];
        int pillY = pos[1];

        guiGraphics.fill(pillX, pillY, pillX + pillW, pillY + pillH, 0x55000000);

        int textY = pillY + 3;
        int cursorX = pillX + padX;
        guiGraphics.text(font, dot, cursorX, textY, accent);
        cursorX += dotW;
        guiGraphics.text(font, labelText, cursorX, textY, labelColor);
        if (!suffixText.isEmpty()) {
            cursorX += labelW;
            guiGraphics.text(font, suffixText, cursorX, textY, suffixColor);
        }
    }
}
