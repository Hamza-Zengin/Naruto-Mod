package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Suiton: Suijinheki (Water Encampment Wall - 水遁·水陣壁)
 * Summons a surging 3D defensive water barrier in front of the caster that extinguishes
 * Fire jutsus, dampens incoming projectiles, pushes enemies back, and primes targets as Wet for Raiton combos.
 */
public class WaterEncampmentWallAbility extends Ability implements Ability.Cooldown {

    public static final int WALL_DURATION_TICKS = 100;
    public static final double WALL_OFFSET = 2.2D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.WATER;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 223L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 28.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 160;
    }

    @Override
    public float getDefaultDamage() {
        return 10.0f;
    }

    public static Vec3 computeWallOrigin(Vec3 casterPos, float yRotDeg) {
        double rad = Math.toRadians(yRotDeg);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        return new Vec3(casterPos.x + fx * WALL_OFFSET, casterPos.y, casterPos.z + fz * WALL_OFFSET);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 wallPos = computeWallOrigin(player.position(), player.getYRot());
        BlockPos baseBlock = BlockPos.containing(wallPos.x, wallPos.y, wallPos.z);
        if (!serverLevel.getBlockState(baseBlock).isAir() && serverLevel.getBlockState(baseBlock.above()).isAir()) {
            wallPos = new Vec3(wallPos.x, baseBlock.getY() + 1.0D, wallPos.z);
        } else if (serverLevel.getBlockState(baseBlock).isAir()) {
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos below = baseBlock.below(dy);
                if (!serverLevel.getBlockState(below).isAir()) {
                    wallPos = new Vec3(wallPos.x, below.getY() + 1.0D, wallPos.z);
                    break;
                }
            }
        }
        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int duration = Ability.getScaledDurationTicks(WALL_DURATION_TICKS, mastery, 0.80D);
        float damage = this.getDamage(ninjaData);

        serverLevel.playSound(null, BlockPos.containing(wallPos), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.8F, 0.85F);
        serverLevel.playSound(null, BlockPos.containing(wallPos), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.4F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                wallPos.x, wallPos.y + 1.0D, wallPos.z,
                32, 1.2D, 0.8D, 1.2D, 0.14D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        JutsuVisualEffectEntity wall = JutsuVisualEffectEntity.create(
                serverLevel, wallPos.x, wallPos.y, wallPos.z,
                JutsuVisualEffectEntity.TYPE_WATER_WALL, duration, player.getYRot());
        wall.setCasterUUID(player.getUUID());
        wall.setMaxLife(duration);
        wall.setEffectDamage(damage);
        wall.refreshDimensions();
        serverLevel.addFreshEntity(wall);
    }
}
