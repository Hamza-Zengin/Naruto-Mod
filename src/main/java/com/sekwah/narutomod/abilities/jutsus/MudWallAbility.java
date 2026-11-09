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

/**
 * Doton: Mud Wall (Doryūheki - 土遁·土流壁)
 * Raises a solid, sculpted 3D Shinobi Earth Bulwark that physically deflects
 * enemies, attacks, and projectiles without clumsy block placement or visual clipping.
 */
public class MudWallAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 321; // B - V - C
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
        BlockPos basePos = player.blockPosition().relative(direction, 2);

        serverLevel.playSound(null, basePos, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.8f, 0.75f);
        serverLevel.playSound(null, basePos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.6f);

        // Ground eruption particles
        BlockParticleOption rockParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        for (int i = 0; i < 16; i++) {
            double rx = basePos.getX() + 0.5 + (player.getRandom().nextDouble() - 0.5) * 2.5;
            double rz = basePos.getZ() + 0.5 + (player.getRandom().nextDouble() - 0.5) * 2.5;
            serverLevel.sendParticles(rockParticle, rx, basePos.getY() + 0.2, rz, 4, 0.2, 0.3, 0.2, 0.08);
        }

        // Erupt 3D Sculpted Shinobi Earth Bulwark Entity (Physical Solid Barrier)
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity earthWall3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, basePos.getX() + 0.5, basePos.getY(), basePos.getZ() + 0.5,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_EARTH_WALL,
                        160, direction.toYRot());
        earthWall3D.setCasterUUID(player.getUUID());
        serverLevel.addFreshEntity(earthWall3D);
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }
}
