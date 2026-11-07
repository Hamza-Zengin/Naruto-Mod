package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.sekclib.player.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * High speed burst dash using chakra
 */
public class ChakraDashAbility extends Ability implements Ability.Cooldown {

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
        return 22;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 15) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getStamina() < 10) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(15, 20);
        ninjaData.useStamina(10, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 look = player.getLookAngle();
        double speed = 2.8;
        PlayerUtil.setVelocity(player, look.x * speed, Math.max(look.y * 0.6 + 0.35, 0.2), look.z * speed, true);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                NarutoSounds.LEAP.get(), SoundSource.PLAYERS, 0.8f, 1.3f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    15, 0.4, 0.2, 0.4, 0.1);
        }
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }
}
