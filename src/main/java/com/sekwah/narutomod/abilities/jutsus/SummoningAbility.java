package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kuchiyose no Jutsu (Summoning Technique: Ninken)
 * Summons a loyal, chakra-enhanced Ninja Hound (Ninken) to fight alongside the caster.
 */
public class SummoningAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 132;
    }

    @Override
    public float getDefaultChakraCost() {
        return 40.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 20 * 20;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);

        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.position().add(look.x * 2.0, 0.2, look.z * 2.0);

        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 1.4f, 0.85f);
        serverLevel.sendParticles(ParticleTypes.CLOUD, spawnPos.x, spawnPos.y + 0.5, spawnPos.z, 30, 0.6, 0.5, 0.6, 0.08,
                net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        Wolf ninken = net.minecraft.world.entity.EntityTypes.WOLF.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (ninken != null) {
            ninken.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0f);
            ninken.tame(player);
            ninken.setCustomName(Component.literal("Ninken").withStyle(ChatFormatting.GOLD));
            ninken.setCustomNameVisible(true);

            double maxHp = 30.0D * powerMult;
            if (ninken.getAttribute(Attributes.MAX_HEALTH) != null) {
                ninken.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
            }
            ninken.setHealth((float) maxHp);
            if (ninken.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                ninken.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0D * powerMult);
            }

            int durationTicks = (int) (600 * powerMult);
            ninken.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, 1));
            ninken.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, 0));

            serverLevel.addFreshEntity(ninken);
        }
    }
}
