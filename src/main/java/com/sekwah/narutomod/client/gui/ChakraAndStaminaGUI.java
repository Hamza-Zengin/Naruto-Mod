package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.sekclib.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
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

    private Minecraft getMinecraft() {
        return this.minecraft != null ? this.minecraft : Minecraft.getInstance();
    }

    public void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft mc = this.getMinecraft();
        if(!shouldRender || mc == null) {
            return;
        }
        this.screenWidth = mc.getWindow().getGuiScaledWidth();
        this.screenHeight = mc.getWindow().getGuiScaledHeight();
        int barDesign = Math.min(Math.max(NarutoConfig.chakraBarDesign, 0), barTypes.length - 1);

        float currentChakraPercent = maxChakra > 0 ? (chakra) / maxChakra : 0;
        float currentStaminaPercent = maxStamina > 0 ? (stamina) / maxStamina : 0;

        int width = 100;
        int offset = 128;

        Identifier barTexture = barTypes[barDesign].texture;
        int barWidth = barTypes[barDesign].width;
        int xOffset = barTypes[barDesign].offset;

        int valuesOffset = 128 + (barWidth / 2);
        int valuesHeight = 26;

        int screenMid = this.screenWidth / 2;

        float darkenFactor = 0.25f;

        Color chakraColor = new Color(255,0,0);
        Color staminaColor = new Color(0,255,0);
        int intStaminaColor = ARGB.opaque(ColorUtil.toMCColor(staminaColor).getValue());
        int intChakraColor = ARGB.opaque(ColorUtil.toMCColor(chakraColor).getValue());

        int intStaminaColorDarker = ARGB.opaque(ColorUtil.toMCColor(new Color((int) (staminaColor.getRed() * darkenFactor),
                (int) (staminaColor.getGreen() * darkenFactor),
                (int) (staminaColor.getBlue() * darkenFactor))).getValue());
        int intChakraColorDarker = ARGB.opaque(ColorUtil.toMCColor(new Color((int) (chakraColor.getRed() * darkenFactor),
                (int) (chakraColor.getGreen() * darkenFactor),
                (int) (chakraColor.getBlue() * darkenFactor))).getValue());

        // Chakra Bar underlay
        int chakraWidth = (int) (barWidth * currentChakraPercent);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid - width - offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44, -1);

        // Stamina Bar underlay
        int staminaWidth = (int) (barWidth * currentStaminaPercent);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid + offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44, -1);

        // Chakra Bar color
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid - chakraWidth - offset - (width - xOffset - barWidth), this.screenHeight - 22,
                xOffset + (barWidth - chakraWidth), 0,
                chakraWidth, 22,
                width, 44, toArgb(chakraColor));

        // Stamina Bar color
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid + offset + (100 - barWidth - xOffset), this.screenHeight - 22,
                xOffset, 0,
                staminaWidth, 22,
                width, 44, toArgb(staminaColor));

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

    private void centeredTextOutlined(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color, int backgroundColor) {
        int width = this.getFont().width(text) / 2;
        guiGraphics.text(this.getFont(), text, x + 1 - width, y, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - 1 - width, y, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y + 1, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y - 1, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y, color, false);
    }

    public void tick() {
        Minecraft mc = this.getMinecraft();
        if(mc != null && mc.getCameraEntity() instanceof Player player) {
            shouldRender = true;
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            chakra = ninjaData.getChakra();
            stamina = ninjaData.getStamina();
            maxChakra = ninjaData.getMaxChakra();
            maxStamina = ninjaData.getMaxStamina();
        } else {
            shouldRender = false;
        }
    }

    private static int toArgb(Color color) {
        return ARGB.color(255, color.getRed(), color.getGreen(), color.getBlue());
    }

    private Font getFont() {
        Minecraft mc = this.getMinecraft();
        return mc != null ? mc.font : null;
    }
}
