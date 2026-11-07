package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public class ChakraAndStaminaGUI {

    public static final BarDesigns.BarInfo[] barTypes = BarDesigns.BarInfo.values();

    private final Minecraft minecraft;
    private int screenWidth;
    private int screenHeight;

    private boolean shouldRender;

    private float chakra;
    private float stamina;

    private float maxChakra;
    private float maxStamina;

    public ChakraAndStaminaGUI(Minecraft mc) {
        this.minecraft = mc;
    }

    public void render(GuiGraphics guiGraphics) {
        if(!shouldRender) {
            return;
        }
        this.screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        this.screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int barDesign = Math.min(Math.max(NarutoConfig.chakraBarDesign, 0), barTypes.length - 1);

        float currentChakraPercent = maxChakra > 0 ? (chakra) / maxChakra : 0;
        float currentStaminaPercent = maxStamina > 0 ? (stamina) / maxStamina : 0;

        int width = 100;
        int offset = 128;

        ResourceLocation barTexture = barTypes[barDesign].texture;
        int barWidth = barTypes[barDesign].width;
        int xOffset = barTypes[barDesign].offset;

        int valuesOffset = 128 + (barWidth / 2);
        int valuesHeight = 26;

        int screenMid = this.screenWidth / 2;

        float darkenFactor = 0.25f;

        Color chakraColor = new Color(255,0,0);
        Color staminaColor = new Color(0,255,0);
        int intStaminaColor = ColorUtil.toMCColor(staminaColor).getValue();
        int intChakraColor = ColorUtil.toMCColor(chakraColor).getValue();

        int intStaminaColorDarker = ColorUtil.toMCColor(new Color((int) (staminaColor.getRed() * darkenFactor),
                (int) (staminaColor.getGreen() * darkenFactor),
                (int) (staminaColor.getBlue() * darkenFactor))).getValue();
        int intChakraColorDarker = ColorUtil.toMCColor(new Color((int) (chakraColor.getRed() * darkenFactor),
                (int) (chakraColor.getGreen() * darkenFactor),
                (int) (chakraColor.getBlue() * darkenFactor))).getValue();

        // Chakra Bar underlay
        int chakraWidth = (int) (barWidth * currentChakraPercent);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(barTexture, screenMid - width - offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44);

        // Stamina Bar underlay
        int staminaWidth = (int) (barWidth * currentStaminaPercent);
        guiGraphics.blit(barTexture, screenMid + offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44);

        // Chakra Bar color
        this.setColor(chakraColor);
        guiGraphics.blit(barTexture, screenMid - chakraWidth - offset - (width - xOffset - barWidth), this.screenHeight - 22,
                xOffset + (barWidth - chakraWidth), 0,
                chakraWidth, 22,
                width, 44);

        // Stamina Bar color
        this.setColor(staminaColor);
        guiGraphics.blit(barTexture, screenMid + offset + (100 - barWidth - xOffset), this.screenHeight - 22,
                xOffset, 0,
                staminaWidth, 22,
                width, 44);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String chakraText = (int) chakra + "/" + (int) maxChakra;
        this.centeredTextOutlined(guiGraphics,
                chakraText,
                screenMid - valuesOffset,
                this.screenHeight - valuesHeight,
                intChakraColor,
                intChakraColorDarker);

        String staminaText = (int) stamina + "/" + (int) maxStamina;
        this.centeredTextOutlined(guiGraphics,
                staminaText,
                screenMid + valuesOffset,
                this.screenHeight - valuesHeight,
                intStaminaColor,
                intStaminaColorDarker);
    }

    private void centeredTextOutlined(GuiGraphics guiGraphics, String text, int x, int y, int color, int backgroundColor) {
        int width = this.getFont().width(text) / 2;
        guiGraphics.drawString(this.getFont(), text, x + 1 - width, y, backgroundColor, false);
        guiGraphics.drawString(this.getFont(), text, x - 1 - width, y, backgroundColor, false);
        guiGraphics.drawString(this.getFont(), text, x - width, y + 1, backgroundColor, false);
        guiGraphics.drawString(this.getFont(), text, x - width, y - 1, backgroundColor, false);
        guiGraphics.drawString(this.getFont(), text, x - width, y, color, false);
    }

    public void tick() {
        if(this.minecraft.getCameraEntity() instanceof Player player) {
            shouldRender = true;
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                chakra = ninjaData.getChakra();
                stamina = ninjaData.getStamina();
                maxChakra = ninjaData.getMaxChakra();
                maxStamina = ninjaData.getMaxStamina();
            });
        } else {
            shouldRender = false;
        }
    }

    private void setColor(Color color) {
        RenderSystem.setShaderColor(color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                1.0F);
    }

    private Font getFont() {
        return this.minecraft.font;
    }
}
