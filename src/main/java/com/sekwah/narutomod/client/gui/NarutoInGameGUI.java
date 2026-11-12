package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(com.sekwah.narutomod.capabilities.INinjaData::tick);
        }
    }

    @SubscribeEvent
    public void renderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            this.chakraOverlay.render(event.getGuiGraphics());
            this.renderActiveHandSeals(event.getGuiGraphics());
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

            // 2. Eighth Gate of Death Crimson Steam Heartbeat Vignette
            if (hasEightGatesDeath) {
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

    /**
     * Renders the active hand seal combo sequence side-by-side as styled seal badges
     * right above the player hotbar.
     */
    public void renderActiveHandSeals(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            List<Byte> seals = ninjaData.getActiveSeals();
            if (seals == null || seals.isEmpty()) {
                return;
            }

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            Font font = mc.font;

            int badgeWidth = 22;
            int badgeHeight = 22;
            int spacing = 4;
            int totalWidth = seals.size() * badgeWidth + (seals.size() - 1) * spacing;
            int startX = (screenWidth - totalWidth) / 2;
            int startY = screenHeight - 68; // Positioned neatly above the hotbar and chakra bars

            // Check for matched jutsu preview
            long combo = 0;
            for (byte b : seals) {
                combo = combo * 10 + b;
            }
            com.sekwah.narutomod.abilities.Ability matched = com.sekwah.narutomod.abilities.NarutoAbilities.getAbilityFromCombo(combo);
            if (matched != null) {
                String jutsuName = Component.translatable(matched.getTranslationKey()).getString();
                String previewText = "§a§l» " + jutsuName + " «";
                int textW = font.width(previewText);
                int midX = screenWidth / 2;
                int previewY = startY - 19;
                guiGraphics.fill(midX - textW / 2 - 5, previewY - 2, midX + textW / 2 + 5, previewY + 11, 0x80000000);
                guiGraphics.renderOutline(midX - textW / 2 - 5, previewY - 2, textW + 10, 13, 0x4034D399);
                guiGraphics.drawCenteredString(font, previewText, midX, previewY, 0xFF55FF55);
            }

            // Subtle dark translucent background card with border
            guiGraphics.fill(startX - 6, startY - 4, startX + totalWidth + 6, startY + badgeHeight + 5, 0xAA000000);
            guiGraphics.renderOutline(startX - 6, startY - 4, totalWidth + 12, badgeHeight + 9, 0xFF4A6572);

            for (int i = 0; i < seals.size(); i++) {
                byte seal = seals.get(i);
                int bx = startX + i * (badgeWidth + spacing);
                int by = startY;

                int bgColor = switch (seal) {
                    case 1 -> 0xD01B2A47; // Seal A: Deep Ocean Blue
                    case 2 -> 0xD0133B32; // Seal B: Emerald Chakra Green
                    case 3 -> 0xD03E1C3B; // Seal C: Mystic Violet
                    default -> 0xD023272A;
                };
                int borderColor = switch (seal) {
                    case 1 -> 0xFF38BDF8; // Bright Blue Cyan
                    case 2 -> 0xFF34D399; // Bright Mint Jade
                    case 3 -> 0xFFF472B6; // Bright Lotus Pink
                    default -> 0xFFFBBF24; // Amber Gold for zodiacs
                };

                // Badge background and glowing border
                guiGraphics.fill(bx, by, bx + badgeWidth, by + badgeHeight, bgColor);
                guiGraphics.renderOutline(bx, by, badgeWidth, badgeHeight, borderColor);

                // Seal Kanji / Glyph
                String sealGlyph = getSealGlyph(seal);
                guiGraphics.drawCenteredString(font, "§f§l" + sealGlyph, bx + badgeWidth / 2, by + 3, 0xFFFFFFFF);

                // Key / Canon Label
                String keyLabel = getSealKeyLabel(seal);
                guiGraphics.drawCenteredString(font, "§7" + keyLabel, bx + badgeWidth / 2, by + 12, 0xFFAAAAAA);
            }

            // Timeout timer bar under the badges
            int timeout = ninjaData.getSealTimeout();
            if (timeout > 0) {
                float ratio = Math.min(1.0f, (float) timeout / NinjaData.DEFAULT_SEAL_TIMEOUT);
                int barW = (int) (totalWidth * ratio);
                guiGraphics.fill(startX, startY + badgeHeight + 2, startX + barW, startY + badgeHeight + 4, 0xFFF59E0B);
            }
        });
    }

    public static String getSealGlyph(byte seal) {
        return switch (seal) {
            case 1 -> "寅"; // Tiger / Seal A
            case 2 -> "巳"; // Snake / Seal B
            case 3 -> "申"; // Monkey / Seal C
            case 4 -> "卯"; // Hare
            case 5 -> "辰"; // Dragon
            case 6 -> "巳"; // Snake
            case 7 -> "午"; // Horse
            case 8 -> "未"; // Ram
            case 9 -> "申"; // Monkey
            case 10 -> "酉"; // Bird
            case 11 -> "戌"; // Dog
            case 12 -> "亥"; // Boar
            default -> String.valueOf(seal);
        };
    }

    public static String getSealKeyLabel(byte seal) {
        return switch (seal) {
            case 1 -> "C";
            case 2 -> "V";
            case 3 -> "B";
            case 6 -> "巳";
            case 7 -> "午";
            case 8 -> "未";
            case 9 -> "申";
            case 12 -> "亥";
            default -> "#" + seal;
        };
    }
}
