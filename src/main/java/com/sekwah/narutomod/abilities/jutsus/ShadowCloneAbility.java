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
    public long defaultCombo() {
        return 123;
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

        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + (look.x * 1.5);
        double spawnY = player.getY();
        double spawnZ = player.getZ() + (look.z * 1.5);

        ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), serverLevel);
        clone.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), player.getXRot());
        clone.setOwner(player);

        serverLevel.addFreshEntity(clone);

        serverLevel.sendParticles(ParticleTypes.POOF, spawnX, spawnY + 0.8, spawnZ,
                30, 0.5, 0.8, 0.5, 0.05);
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }
}
