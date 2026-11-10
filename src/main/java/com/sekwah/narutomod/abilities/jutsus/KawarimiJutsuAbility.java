package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Substitution Jutsu (Kawarimi no Jutsu)
 * Leaves behind a wooden log and dashes safely backwards.
 */
public class KawarimiJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.ACADEMY_STUDENT;
    }

    @Override
    public long defaultCombo() {
        return 131;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, 20.0f, 0, 20);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        BlockPos currentPos = player.blockPosition();

        // Spawn poof particles at origin
        serverLevel.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + 0.5, player.getZ(),
                30, 0.5, 0.8, 0.5, 0.05);

        // Place a temporary oak log if empty
        if (serverLevel.getBlockState(currentPos).isAir()) {
            serverLevel.setBlockAndUpdate(currentPos, Blocks.OAK_LOG.defaultBlockState());
        }

        // Teleport 6 blocks backwards
        Vec3 look = player.getLookAngle();
        double targetX = player.getX() - (look.x * 6);
        double targetZ = player.getZ() - (look.z * 6);
        double targetY = player.getY();

        player.teleportTo(targetX, targetY, targetZ);

        // Sound effect
        serverLevel.playSound(null, currentPos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.2f, 0.9f);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    @Override
    public int getCooldown() {
        return 6 * 20;
    }
}
