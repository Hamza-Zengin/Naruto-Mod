package com.sekwah.narutomod.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectedJutsuHudBadgeTest {

    @Test
    public void badgeStaysAboveStaminaBarAndTextAcrossGuiScales() {
        int pillH = 14;
        int pillW = 90;
        int[][] scaledResolutions = {
                {320, 180},
                {427, 240},
                {480, 270},
                {640, 360},
                {960, 540}
        };

        for (int[] res : scaledResolutions) {
            int screenW = res[0];
            int screenH = res[1];
            int[] pos = NarutoInGameGUI.computeBadgePosition(screenW, screenH, pillW, pillH);
            int pillX = pos[0];
            int pillY = pos[1];

            int staminaTextTop = screenH - 27;
            assertTrue(pillY + pillH < staminaTextTop,
                    "Badge bottom should stay above stamina text top at " + screenW + "x" + screenH);
            assertTrue(pillX >= 8, "Badge X should respect left margin at " + screenW + "x" + screenH);
            assertTrue(pillX + pillW <= screenW - 8 || pillX == 8,
                    "Badge should align to right margin unless clamped at left");
        }
    }

    @Test
    public void badgeClampsLeftMarginOnVeryNarrowScreen() {
        int[] pos = NarutoInGameGUI.computeBadgePosition(100, 200, 120, 14);
        assertEquals(8, pos[0]);
        assertEquals(155, pos[1]);
    }

    @Test
    public void maxBadgeWidthScalesWithinBounds() {
        assertEquals(64, NarutoInGameGUI.clampMaxBadgeWidth(120));
        assertEquals(128, NarutoInGameGUI.clampMaxBadgeWidth(320));
        assertEquals(180, NarutoInGameGUI.clampMaxBadgeWidth(480));
        assertEquals(180, NarutoInGameGUI.clampMaxBadgeWidth(960));
    }
}
