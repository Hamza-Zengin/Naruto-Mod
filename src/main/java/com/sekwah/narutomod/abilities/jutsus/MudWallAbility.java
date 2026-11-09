package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Doton: Mud Wall (Doryuheki)
 * Raises a temporary protective stone/mud barrier in front of the caster.
 */
public class MudWallAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 321;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(30, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Direction direction = player.getDirection();
        Direction perpendicular = direction.getClockWise();
        BlockPos basePos = player.blockPosition().relative(direction, 2);

        BlockState mudState = Blocks.MUD_BRICKS.defaultBlockState();

        serverLevel.playSound(null, basePos, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.5f, 0.8f);
        serverLevel.playSound(null, basePos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.55f);

        // Erupt 3D Sculpted Shinobi Earth Bulwark & Stone Buttresses
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity earthWall3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, basePos.getX() + 0.5, basePos.getY(), basePos.getZ() + 0.5,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_EARTH_WALL,
                        160, direction.toYRot());
        serverLevel.addFreshEntity(earthWall3D);

        java.util.List<BlockPos> placedBlocks = new java.util.ArrayList<>();
        for (int width = -1; width <= 1; width++) {
            for (int height = 0; height <= 2; height++) {
                BlockPos targetPos = basePos.relative(perpendicular, width).above(height);
                if (serverLevel.getBlockState(targetPos).isAir()) {
                    serverLevel.setBlockAndUpdate(targetPos, mudState);
                    placedBlocks.add(targetPos);
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, mudState),
                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                            10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }

        // Crumble temporary earth wall blocks cleanly when the 3D bulwark expires
        ninjaData.scheduleDelayedTickEvent(p -> {
            if (!(p.level() instanceof ServerLevel sLevel)) return;
            for (BlockPos pos : placedBlocks) {
                if (sLevel.getBlockState(pos).is(Blocks.MUD_BRICKS)) {
                    sLevel.destroyBlock(pos, false);
                }
            }
        }, 160);
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }
}
