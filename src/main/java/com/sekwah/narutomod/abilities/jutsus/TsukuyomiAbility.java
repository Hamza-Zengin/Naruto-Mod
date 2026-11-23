package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mangekyou Sharingan Genjutsu: Tsukuyomi
 * Traps the target in an illusory world of mental torment.
 */
public class TsukuyomiAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.JONIN;
    }

    @Override
    public String getRequiredClanPath() {
        return "uchiha";
    }

    @Override
    public float getDefaultChakraCost() {
        return 45.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 18 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 18.0f;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (!super.canActivate(player, ninjaData)) return false;
        if (ninjaData == null) return false;
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notclan",
                        Component.literal("Uchiha").withStyle(ChatFormatting.RED)));
            }
            return false;
        }
        if (!ninjaData.isSharinganActive() || !SharinganAbility.hasMangekyou(ninjaData)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("narutomod.ability.amaterasu.requires_mangekyo")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        int duration = (int) (100 * powerMult);
        float mentalDamage = this.getDamage(ninjaData);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 20.0D;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5D);
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e));

        LivingEntity bestTarget = null;
        double bestDot = 0.85D;
        for (LivingEntity e : candidates) {
            Vec3 toTarget = e.getEyePosition().subtract(eyePos).normalize();
            double dot = look.dot(toTarget);
            if (dot > bestDot && e.distanceTo(player) <= range) {
                bestDot = dot;
                bestTarget = e;
            }
        }

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 1.5f);
        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, eyePos.x, eyePos.y, eyePos.z, 15, 0.2, 0.2, 0.2, 0.05,
                net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        if (bestTarget != null) {
            bestTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 1));
            bestTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0));
            bestTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 5));
            bestTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2));
            bestTarget.hurtServer(serverLevel, player.damageSources().magic(), mentalDamage);
            serverLevel.sendParticles(ParticleTypes.PORTAL, bestTarget.getX(), bestTarget.getEyeY(), bestTarget.getZ(), 35, 0.4, 0.6, 0.4, 0.2,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }
}
