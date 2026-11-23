package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.ChakraElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/**
 * Shared drawing and formatting helpers used by {@link NinjaMenuScreen} tabs.
 */
final class NinjaMenuRenderUtils {

    private NinjaMenuRenderUtils() {}

    static void drawWrapped(Font font, GuiGraphicsExtractor graphics, Component text, int x, int y, int wrapWidth, int color) {
        for (FormattedCharSequence line : font.split(text, wrapWidth)) {
            graphics.text(font, line, x, y, color);
            y += font.lineHeight + 2;
        }
    }

    static void renderBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float ratio, int fillCol, int bgCol) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        graphics.fill(x, y, x + width, y + height, bgCol);
        graphics.fill(x, y, x + (int) (width * ratio), y + height, fillCol);
        graphics.outline(x, y, width, height, 0xFF555566);
    }

    static ChatFormatting getElementColor(ChakraElement element) {
        if (element == null) {
            return ChatFormatting.GRAY;
        }
        return switch (element) {
            case FIRE -> ChatFormatting.RED;
            case WIND -> ChatFormatting.GREEN;
            case LIGHTNING -> ChatFormatting.AQUA;
            case EARTH -> ChatFormatting.GOLD;
            case WATER -> ChatFormatting.BLUE;
            case NONE -> ChatFormatting.GRAY;
        };
    }

    static String formatStatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
