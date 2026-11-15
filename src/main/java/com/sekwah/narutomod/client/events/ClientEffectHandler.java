package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only handler for lightweight S2C jutsu particle and visual effects.
 */
public class ClientEffectHandler {

    public static void handleJutsuEffect(ClientJutsuEffectPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        RandomSource rand = level.getRandom();
        Vec3 pos = msg.position;
        Vec3 dir = msg.direction.lengthSqr() > 1e-6 ? msg.direction.normalize() : new Vec3(0, 1, 0);
        float scale = Math.max(0.1f, Math.min(msg.scale, 10.0f));

        DustParticleOptions coloredDust = new DustParticleOptions(ARGB.opaque(msg.colorHex), Math.min(2.5f, scale));

        String path = msg.effectId.getPath();
        if ("hakke_kusho".equals(path)) {
            for (int i = 0; i < 16; i++) {
                double dist = 0.8 + i * 0.75 * scale;
                Vec3 p = pos.add(dir.scale(dist));
                level.addParticle(coloredDust,
                        p.x + (rand.nextDouble() - 0.5) * 0.4,
                        p.y + (rand.nextDouble() - 0.5) * 0.4,
                        p.z + (rand.nextDouble() - 0.5) * 0.4,
                        dir.x * 0.15, dir.y * 0.15, dir.z * 0.15);
            }
        } else if ("wind_breaker".equals(path)) {
            for (int i = 0; i < 24; i++) {
                double spread = 0.35 * scale;
                level.addParticle(ParticleTypes.CLOUD,
                        pos.x + (rand.nextDouble() - 0.5) * spread,
                        pos.y + (rand.nextDouble() - 0.5) * spread,
                        pos.z + (rand.nextDouble() - 0.5) * spread,
                        dir.x * 0.45 * scale, dir.y * 0.45 * scale, dir.z * 0.45 * scale);
            }
        } else if ("kirin_impact".equals(path)) {
            for (int i = 0; i < 30; i++) {
                double angle = (2.0 * Math.PI * i) / 30.0;
                double vx = Math.cos(angle) * 0.45 * scale;
                double vz = Math.sin(angle) * 0.45 * scale;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.25, pos.z, vx, 0.15, vz);
                level.addParticle(coloredDust, pos.x + vx, pos.y + 0.2, pos.z + vz, 0.0, 0.05, 0.0);
            }
        } else {
            for (int i = 0; i < 18; i++) {
                level.addParticle(coloredDust,
                        pos.x + (rand.nextDouble() - 0.5) * scale,
                        pos.y + (rand.nextDouble() - 0.5) * scale,
                        pos.z + (rand.nextDouble() - 0.5) * scale,
                        dir.x * 0.2, dir.y * 0.2, dir.z * 0.2);
            }
        }
    }
}
