package com.sekwah.narutomod.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Client-only overlay state manager for the Flash Bomb (Hikaridama) white-to-warm screen flash.
 */
public final class FlashOverlayManager {

    private static final float MAX_PEAK_ALPHA = 0.96F;

    private static float flashAlpha = 0.0F;
    private static int remainingTicks = 0;
    private static int totalDurationTicks = 0;

    private FlashOverlayManager() {
    }

    public static void triggerFlash(float intensity, int durationTicks) {
        if (!Float.isFinite(intensity) || intensity <= 0.0F || durationTicks <= 0) {
            return;
        }
        float clampedIntensity = Mth.clamp(intensity, 0.0F, 1.0F);
        if (remainingTicks <= 0 || totalDurationTicks <= 0) {
            flashAlpha = clampedIntensity;
            remainingTicks = durationTicks;
            totalDurationTicks = durationTicks;
            return;
        }

        float currentProgress = Mth.clamp((float) remainingTicks / (float) totalDurationTicks, 0.0F, 1.0F);
        float currentEffectiveIntensity = flashAlpha * easeOutSmooth(currentProgress);

        if (clampedIntensity > currentEffectiveIntensity) {
            flashAlpha = clampedIntensity;
            remainingTicks = Math.max(remainingTicks, durationTicks);
            totalDurationTicks = remainingTicks;
        } else if (durationTicks > remainingTicks) {
            flashAlpha = Math.max(currentEffectiveIntensity, clampedIntensity);
            remainingTicks = durationTicks;
            totalDurationTicks = durationTicks;
        }
    }

    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                clear();
            }
        }
    }

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        if (guiGraphics == null || remainingTicks <= 0 || totalDurationTicks <= 0 || flashAlpha <= 0.0F) {
            return;
        }
        float clampedPartial = Mth.clamp(partialTick, 0.0F, 1.0F);
        float progress = Mth.clamp((remainingTicks - clampedPartial) / (float) totalDurationTicks, 0.0F, 1.0F);
        if (progress <= 0.0F) {
            return;
        }

        float eased = easeOutSmooth(progress);
        float alpha = Mth.clamp(flashAlpha * MAX_PEAK_ALPHA * eased, 0.0F, 1.0F);
        if (alpha <= 0.001F) {
            return;
        }

        float fadeElapsed = 1.0F - progress;
        float r = 1.0F;
        float g = Mth.lerp(fadeElapsed, 1.0F, 0.92F);
        float b = Mth.lerp(fadeElapsed, 1.0F, 0.78F);

        int color = ARGB.colorFromFloat(alpha, r, g, b);
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
    }

    public static void clear() {
        flashAlpha = 0.0F;
        remainingTicks = 0;
        totalDurationTicks = 0;
    }

    public static boolean isActive() {
        return remainingTicks > 0 && totalDurationTicks > 0 && flashAlpha > 0.0F;
    }

    private static float easeOutSmooth(float progress) {
        float p = Mth.clamp(progress, 0.0F, 1.0F);
        return p * p * (3.0F - 2.0F * p);
    }
}
