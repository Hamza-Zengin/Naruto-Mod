package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
        } else if ("elemental_burst".equals(path)) {
            DustParticleOptions burstDust = new DustParticleOptions(ARGB.opaque(msg.colorHex), Math.min(2.2f, scale));
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI * i) / 24.0;
                double vx = Math.cos(angle) * 0.35 * scale;
                double vz = Math.sin(angle) * 0.35 * scale;
                level.addParticle(burstDust,
                        pos.x + vx * 0.4, pos.y + (rand.nextDouble() - 0.5) * 0.4, pos.z + vz * 0.4,
                        vx * 0.25, 0.08 + rand.nextDouble() * 0.08, vz * 0.25);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y, pos.z,
                        vx * 0.6, 0.12 + rand.nextDouble() * 0.1, vz * 0.6);
            }
        } else if ("hand_seal_spark".equals(path)) {
            DustParticleOptions sparkDust = new DustParticleOptions(ARGB.opaque(msg.colorHex), 0.65f);
            for (int i = 0; i < 12; i++) {
                double ox = (rand.nextDouble() - 0.5) * 0.25 * scale;
                double oy = (rand.nextDouble() - 0.5) * 0.25 * scale;
                double oz = (rand.nextDouble() - 0.5) * 0.25 * scale;
                level.addParticle(sparkDust,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        ox * 0.35, 0.04 + rand.nextDouble() * 0.05, oz * 0.35);
            }
        } else if ("rasenshuriken_vortex".equals(path)) {
            BlockState surfaceState = Block.stateById(msg.colorHex);
            if (surfaceState.isAir()) {
                surfaceState = Blocks.DIRT.defaultBlockState();
            }
            BlockParticleOption dustOption = new BlockParticleOption(ParticleTypes.BLOCK, surfaceState);
            for (int i = 0; i < 36; i++) {
                double angle = (2.0 * Math.PI * i) / 18.0 + rand.nextDouble() * 0.35;
                double radius = (0.6 + (i / 18.0) * 1.4) * scale;
                double px = pos.x + Math.cos(angle) * radius;
                double pz = pos.z + Math.sin(angle) * radius;
                double py = pos.y + 0.1 + rand.nextDouble() * 0.6;
                double tangentX = -Math.sin(angle) * 0.32 * scale;
                double tangentZ = Math.cos(angle) * 0.32 * scale;
                level.addParticle(dustOption, px, py, pz, tangentX, 0.22 + rand.nextDouble() * 0.28, tangentZ);
                if (i % 2 == 0) {
                    level.addParticle(ParticleTypes.CLOUD, px, py + 0.3, pz, tangentX * 0.8, 0.14 + rand.nextDouble() * 0.16, tangentZ * 0.8);
                }
            }
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6.0;
                level.addParticle(ParticleTypes.SWEEP_ATTACK,
                        pos.x + Math.cos(angle) * scale, pos.y + 0.5 + rand.nextDouble() * 1.2, pos.z + Math.sin(angle) * scale,
                        0.0, 0.05, 0.0);
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
