package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shadow Clone Jutsu (Kage Bunshin no Jutsu)
 * Spawns a physical clone that attacks enemies and vanishes when hit.
 */
public class ShadowCloneAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 123;
    }

    @Override
    public float getDefaultChakraCost() {
        return 30.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 12 * 20;
    }

    public static int getCloneCount(int mastery) {
        if (mastery < 100) return 1;
        if (mastery < 300) return 2;
        if (mastery < 700) return 4;
        return 8;
    }

    public static int getCloneLifespan(int mastery) {
        if (mastery < 100) return 20 * 20; // 20s (400 ticks)
        if (mastery < 300) return 30 * 20; // 30s (600 ticks)
        if (mastery < 700) return 45 * 20; // 45s (900 ticks)
        return 60 * 20; // 60s (1200 ticks)
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int count = getCloneCount(mastery);
        int lifespan = getCloneLifespan(mastery);

        float yaw = player.getYRot();
        double radius = count > 1 ? 3.2D : 2.2D;
        double angleStep = (2 * Math.PI) / count;

        java.util.List<net.minecraft.resources.ResourceLocation> cloneJutsus = new java.util.ArrayList<>();
        if (ninjaData != null && mastery >= 300) {
            String[] basicPool = {"fireball", "water_bullet", "wind_breaker", "rasengan", "chidori"};
            for (String jutsuName : basicPool) {
                net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(com.sekwah.narutomod.NarutoMod.MOD_ID, jutsuName);
                if (ninjaData.getMastery(id) >= 250) {
                    cloneJutsus.add(id);
                }
            }
        }

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(yaw) + (i * angleStep) + (Math.PI / 2);
            double spawnX = player.getX() - Math.sin(angle) * radius;
            double spawnY = player.getY();
            double spawnZ = player.getZ() + Math.cos(angle) * radius;

            ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), serverLevel);
            clone.moveTo(spawnX, spawnY, spawnZ, yaw, player.getXRot());
            clone.setOwner(player);
            clone.setMaxLifespan(lifespan);
            clone.setUnlockedCloneJutsus(cloneJutsus);
            clone.setFastJutsuCooldown(mastery >= 600);

            serverLevel.addFreshEntity(clone);

            serverLevel.sendParticles(ParticleTypes.POOF, spawnX, spawnY + 0.8, spawnZ,
                    25, 0.4, 0.6, 0.4, 0.05);
        }

        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
