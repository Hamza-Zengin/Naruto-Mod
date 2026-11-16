package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client-only camera shake manager hooked into ViewportEvent.ComputeCameraAngles.
 */
@EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT)
public class CameraShakeManager {

    private static float intensity = 0.0f;
    private static int remainingTicks = 0;
    private static int totalTicks = 0;

    public static void triggerShake(float newIntensity, int durationTicks) {
        float scaledIntensity = newIntensity * (float) NarutoConfig.cameraShakeMultiplier;
        if (durationTicks <= 0 || scaledIntensity <= 0.0f) {
            return;
        }
        float clampedIntensity = Math.min(scaledIntensity, 5.0f);
        if (remainingTicks <= 0 || clampedIntensity >= intensity) {
            intensity = clampedIntensity;
        }
        if (durationTicks > remainingTicks) {
            remainingTicks = durationTicks;
            totalTicks = durationTicks;
        }
    }

    public static void triggerShakeAt(float baseIntensity, int durationTicks, Vec3 sourcePos, double maxRadius) {
        if (maxRadius <= 0.0 || sourcePos == null) {
            triggerShake(baseIntensity, durationTicks);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        double distance = mc.player.position().distanceTo(sourcePos);
        if (distance >= maxRadius) {
            return;
        }
        float attenuation = (float) Math.max(0.0, 1.0 - (distance / maxRadius));
        triggerShake(baseIntensity * attenuation, durationTicks);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                intensity = 0.0f;
                totalTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0 || totalTicks <= 0 || intensity <= 0.0f) {
            return;
        }
        double partial = event.getPartialTick();
        float progress = (float) Math.max(0.0, (remainingTicks - partial) / totalTicks);
        float currentStrength = intensity * progress;

        double time = (totalTicks - remainingTicks + partial) * 1.8;
        float pitchOffset = (float) (Math.sin(time * 2.7) * currentStrength * 1.4f);
        float yawOffset = (float) (Math.cos(time * 3.1) * currentStrength * 1.2f);
        float rollOffset = (float) (Math.sin(time * 4.3) * currentStrength * 0.8f);

        event.setPitch(event.getPitch() + pitchOffset);
        event.setYaw(event.getYaw() + yawOffset);
        event.setRoll(event.getRoll() + rollOffset);
    }
}
