package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuTargetingHelper;
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
 * Katon: Haisekishō (Ash Pile Burning - 火遁·灰積焼)
 * Exhales a lingering superheated cloud of volcanic ash that blinds and slows foes
 * before erupting in a chain-reaction fireball when ignited by Fire or Explosives (or on expiry).
 */
public class AshPileBurningAbility extends Ability implements Ability.Cooldown {

    public static final int CLOUD_DURATION_TICKS = 80;
    public static final double CLOUD_RADIUS = 3.6D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.FIRE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 1231L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 36.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 180;
    }

    @Override
    public float getDefaultDamage() {
        return 20.0f;
    }

    public static int getScaledDuration(int mastery) {
        return Ability.getScaledDurationTicks(CLOUD_DURATION_TICKS, mastery, 0.60D);
    }

    public static double getScaledRange(int mastery) {
        double progress = Math.min(1.0D, Math.max(0, mastery) / 700.0D);
        return 18.0D + (4.0D * progress);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        Vec3 targetPos = JutsuTargetingHelper.resolveTargetOrGroundOrigin(serverLevel, player, getScaledRange(mastery), 2.8D);
        int duration = getScaledDuration(mastery);
        float damage = this.getDamage(ninjaData);

        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.3F, 0.72F);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.1F, 0.85F);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                targetPos.x, targetPos.y + 0.8D, targetPos.z,
                28, 1.4D, 0.6D, 1.4D, 0.05D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                targetPos.x, targetPos.y + 0.6D, targetPos.z,
                16, 1.0D, 0.4D, 1.0D, 0.04D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        JutsuVisualEffectEntity ashCloud = JutsuVisualEffectEntity.create(
                serverLevel, targetPos.x, targetPos.y, targetPos.z,
                JutsuVisualEffectEntity.TYPE_ASH_CLOUD, duration, player.getYRot());
        ashCloud.setCasterUUID(player.getUUID());
        ashCloud.setEffectDamage(damage);
        serverLevel.addFreshEntity(ashCloud);
    }
}
