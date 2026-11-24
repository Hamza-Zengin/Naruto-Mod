package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * GTA 5-style semi-transparent 8-slot Radial Jutsu Wheel rendered while holding the Wheel keybind (V).
 */
public final class JutsuWheelOverlay {

    public static final double DEADZONE_RADIUS = 30.0D;
    private static final int OUTER_RADIUS = 114;
    private static final int INNER_RADIUS = 34;
    private static final int LABEL_RADIUS = 75;

    private static boolean open = false;
    private static int hoveredSlot = -1;

    private JutsuWheelOverlay() {}

    public static boolean isOpen() {
        return open;
    }

    public static int getHoveredSlot() {
        return hoveredSlot;
    }

    public static void open(int initialSlot) {
        open = true;
        hoveredSlot = (initialSlot >= 0 && initialSlot < INinjaData.MAX_JUTSU_SLOTS) ? initialSlot : -1;
    }

    public static void close() {
        open = false;
        hoveredSlot = -1;
    }

    /**
     * Pure geometric helper mapping cursor coordinates relative to screen center onto one of the 8 radial slices.
     * Slot 0 is centered at 12 o'clock (top), progressing clockwise through slot 7 (top-left).
     * Returns -1 when inside the central deadzone.
     */
    public static int computeHoveredSlot(double mouseX, double mouseY, double centerX, double centerY, double deadzoneRadius) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        if ((dx * dx + dy * dy) < (deadzoneRadius * deadzoneRadius)) {
            return -1;
        }
        // atan2(dy, dx): -PI/2 is straight up (12 o'clock). Shift by +PI/2 + PI/8 so slice 0 spans [-22.5°, +22.5°] around top.
        double angle = Math.atan2(dy, dx) + (Math.PI / 2.0D) + (Math.PI / 8.0D);
        double twoPi = Math.PI * 2.0D;
        angle = ((angle % twoPi) + twoPi) % twoPi;
        int sector = (int) Math.floor(angle / (twoPi / INinjaData.MAX_JUTSU_SLOTS));
        return Math.max(0, Math.min(INinjaData.MAX_JUTSU_SLOTS - 1, sector));
    }

    public static int getElementAccentColor(ChakraElement element) {
        if (element == null) {
            return 0xFFA78BFA;
        }
        return switch (element) {
            case FIRE -> 0xFFFF6B35;
            case WATER -> 0xFF38BDF8;
            case EARTH -> 0xFFD97706;
            case WIND -> 0xFF34D399;
            case LIGHTNING -> 0xFF60A5FA;
            case NONE -> 0xFFA78BFA;
        };
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!open) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) {
            close();
            return;
        }

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int centerX = screenW / 2;
        int centerY = screenH / 2;

        int rawScreenW = mc.getWindow().getScreenWidth();
        int rawScreenH = mc.getWindow().getScreenHeight();
        if (rawScreenW > 0 && rawScreenH > 0) {
            double mouseX = mc.mouseHandler.xpos() * (double) screenW / (double) rawScreenW;
            double mouseY = mc.mouseHandler.ypos() * (double) screenH / (double) rawScreenH;
            int computed = computeHoveredSlot(mouseX, mouseY, centerX, centerY, DEADZONE_RADIUS);
            if (computed >= 0) {
                hoveredSlot = computed;
            }
        }

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        int activeSlot = hoveredSlot >= 0 ? hoveredSlot : ninjaData.getSelectedJutsuSlot();
        Identifier activeAbilityId = ninjaData.getEquippedJutsu(activeSlot);
        Ability activeAbility = NarutoAbilities.getAbility(activeAbilityId);
        if (activeAbility != null && !activeAbility.isWheelEquippable()) {
            activeAbility = null;
        }
        int activeAccent = activeAbility != null ? getElementAccentColor(activeAbility.getChakraElement()) : 0xFF64748B;

        // Subtle translucent full-screen dim to separate wheel cleanly from world geometry
        graphics.fill(0, 0, screenW, screenH, 0x45000000);

        // Draw circular semi-transparent ring and highlighted wedge
        drawRadialRing(graphics, centerX, centerY, activeSlot, activeAccent);

        // Draw 8 radial divider lines between sectors
        for (int i = 0; i < INinjaData.MAX_JUTSU_SLOTS; i++) {
            double sepAngle = -Math.PI / 2.0D - (Math.PI / 8.0D) + i * (Math.PI / 4.0D);
            for (int r = INNER_RADIUS + 1; r < OUTER_RADIUS - 1; r += 3) {
                int px = centerX + (int) Math.round(Math.cos(sepAngle) * r);
                int py = centerY + (int) Math.round(Math.sin(sepAngle) * r);
                graphics.fill(px, py, px + 1, py + 1, 0x6694A3B8);
            }
        }

        Font font = mc.font;
        ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();

        // Render each of the 8 slices
        for (int slot = 0; slot < INinjaData.MAX_JUTSU_SLOTS; slot++) {
            double sliceAngle = -Math.PI / 2.0D + slot * (Math.PI / 4.0D);
            int sx = centerX + (int) Math.round(Math.cos(sliceAngle) * LABEL_RADIUS);
            int sy = centerY + (int) Math.round(Math.sin(sliceAngle) * LABEL_RADIUS);

            Identifier slotAbilityId = ninjaData.getEquippedJutsu(slot);
            Ability slotAbility = NarutoAbilities.getAbility(slotAbilityId);
            if (slotAbility != null && !slotAbility.isWheelEquippable()) {
                slotAbility = null;
            }
            boolean isHovered = (slot == activeSlot);
            int accent = slotAbility != null ? getElementAccentColor(slotAbility.getChakraElement()) : 0xFF64748B;

            int cardW = 66;
            int cardH = 26;
            int cx = sx - cardW / 2;
            int cy = sy - cardH / 2;

            int cardBg = isHovered ? 0xD0162030 : 0xAA0C111A;
            int cardBorder = isHovered ? accent : 0x66475569;
            graphics.fill(cx, cy, cx + cardW, cy + cardH, cardBg);
            graphics.outline(cx, cy, cardW, cardH, cardBorder);

            String slotBadge = "[" + (slot + 1) + "]";
            if (slotAbility != null) {
                String rawName = Component.translatable(slotAbility.getTranslationKey()).getString();
                String label = font.width(rawName) > cardW - 8
                        ? font.plainSubstrByWidth(rawName, cardW - 14) + ".."
                        : rawName;
                int nameColor = isHovered ? 0xFFFFFFFF : 0xFFCBD5E1;
                graphics.centeredText(font, label, sx, cy + 4, nameColor);

                // Secondary status line: Cooldown / Active Toggle / Chakra Cost
                String statusText;
                int statusColor;
                CooldownTickEvent cd = ninjaData.getCooldownEvents().get(slotAbility.getTranslationKey());
                boolean isToggled = slotAbility.activationType() == Ability.ActivationType.TOGGLE
                        && toggleData != null
                        && toggleData.hasToggleAbility(slotAbilityId);
                if (cd != null && cd.ticks > 0) {
                    statusText = String.format(java.util.Locale.ROOT, "%.1fs", cd.ticks / 20.0f);
                    statusColor = 0xFFF87171;
                } else if (isToggled) {
                    statusText = "● " + Component.translatable("naruto.hud.jutsu.active").getString();
                    statusColor = 0xFF4ADE80;
                } else {
                    float cost = slotAbility.getFinalChakraCost(mc.player, ninjaData);
                    statusText = slotBadge + " " + (cost > 0 ? ((int) Math.ceil(cost)) + " CP" : "Free");
                    statusColor = accent;
                }
                graphics.centeredText(font, statusText, sx, cy + 14, statusColor);
            } else {
                graphics.centeredText(font, slotBadge, sx, cy + 4, isHovered ? 0xFFE2E8F0 : 0xFF64748B);
                String emptyStr = Component.translatable("naruto.hud.jutsu.empty").getString();
                graphics.centeredText(font, emptyStr, sx, cy + 14, 0xFF64748B);
            }
        }

        // Center hub info
        if (activeAbility != null) {
            String title = Component.translatable(activeAbility.getTranslationKey()).getString();
            String shortTitle = font.width(title) > 130 ? font.plainSubstrByWidth(title, 124) + "..." : title;
            graphics.centeredText(font, shortTitle, centerX, centerY - 12, activeAccent);

            String typeKey = switch (activeAbility.activationType()) {
                case TOGGLE -> "naruto.gui.jutsu_wheel.activation.toggle";
                case CHANNELED -> "naruto.gui.jutsu_wheel.activation.channeled";
                default -> "naruto.gui.jutsu_wheel.activation.instant";
            };
            String typeName = Component.translatable(typeKey).getString();
            float cost = activeAbility.getFinalChakraCost(mc.player, ninjaData);
            String subInfo = typeName + (cost > 0 ? " • " + (int) Math.ceil(cost) + " CP" : "");
            graphics.centeredText(font, subInfo, centerX, centerY + 1, 0xFFE2E8F0);
        } else {
            graphics.centeredText(font, Component.translatable("naruto.gui.jutsu_wheel.title"), centerX, centerY - 10, 0xFFFBBF24);
            graphics.centeredText(font, Component.translatable("naruto.gui.jutsu_wheel.empty"), centerX, centerY + 2, 0xFF94A3B8);
        }

        graphics.centeredText(font, Component.translatable("naruto.gui.jutsu_wheel.hint"), centerX, centerY + OUTER_RADIUS + 10, 0xFFCBD5E1);
    }

    private static void drawRadialRing(GuiGraphicsExtractor graphics, int centerX, int centerY, int highlightedSlot, int accentColor) {
        int outerSq = OUTER_RADIUS * OUTER_RADIUS;
        int innerSq = INNER_RADIUS * INNER_RADIUS;
        int highlightFill = ARGB.color(0x55, ARGB.red(accentColor), ARGB.green(accentColor), ARGB.blue(accentColor));
        int ringFill = 0x8C0B1018;
        int centerFill = 0xB8080B10;

        for (int dy = -OUTER_RADIUS; dy <= OUTER_RADIUS; dy++) {
            int maxDx = (int) Math.floor(Math.sqrt(outerSq - dy * dy));
            int y = centerY + dy;
            if (Math.abs(dy) < INNER_RADIUS) {
                int minDx = (int) Math.ceil(Math.sqrt(innerSq - dy * dy));
                graphics.fill(centerX - maxDx, y, centerX - minDx, y + 1, ringFill);
                graphics.fill(centerX - minDx, y, centerX + minDx, y + 1, centerFill);
                graphics.fill(centerX + minDx, y, centerX + maxDx, y + 1, ringFill);
            } else {
                graphics.fill(centerX - maxDx, y, centerX + maxDx, y + 1, ringFill);
            }
        }

        // Subtle highlight overlay across the active slice
        if (highlightedSlot >= 0 && highlightedSlot < INinjaData.MAX_JUTSU_SLOTS) {
            for (int dy = -OUTER_RADIUS; dy <= OUTER_RADIUS; dy += 2) {
                int maxDx = (int) Math.floor(Math.sqrt(outerSq - dy * dy));
                int minDx = Math.abs(dy) < INNER_RADIUS ? (int) Math.ceil(Math.sqrt(innerSq - dy * dy)) : 0;
                int y = centerY + dy;
                for (int side = -1; side <= 1; side += 2) {
                    int startX = side < 0 ? -maxDx : minDx;
                    int endX = side < 0 ? -minDx : maxDx;
                    int runStart = Integer.MIN_VALUE;
                    for (int dx = startX; dx <= endX; dx += 2) {
                        int sector = computeHoveredSlot(centerX + dx, centerY + dy, centerX, centerY, INNER_RADIUS);
                        if (sector == highlightedSlot) {
                            if (runStart == Integer.MIN_VALUE) {
                                runStart = dx;
                            }
                        } else if (runStart != Integer.MIN_VALUE) {
                            graphics.fill(centerX + runStart, y, centerX + dx, y + 2, highlightFill);
                            runStart = Integer.MIN_VALUE;
                        }
                    }
                    if (runStart != Integer.MIN_VALUE) {
                        graphics.fill(centerX + runStart, y, centerX + endX, y + 2, highlightFill);
                    }
                }
            }
        }
    }
}
