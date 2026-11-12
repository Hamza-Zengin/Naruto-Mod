package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

import java.util.List;

/**
 * Kage Mane no Jutsu (Shadow Possession Jutsu)
 * Nara Clan Secret Technique: Extends a dark shadow tendril across the ground.
 * Captured targets are paralyzed and forced to mirror the caster's movements and gaze.
 */
public class ShadowPossessionAbility extends Ability implements Ability.Cooldown {

    private static final DustParticleOptions SHADOW_INK = new DustParticleOptions(new Vector3f(0.06F, 0.04F, 0.10F), 1.35F);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.GENIN;
    }

    @Override
    public long defaultCombo() {
        return 331; // B - B - C
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (checkCooldown(player, ninjaData, this.getTranslationKey())) {
            return false;
        }
        boolean isNara = ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase("nara");
        float baseCost = isNara ? 22.0f : 32.0f;
        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float cost = baseCost * getChakraCostMultiplier(mastery);
        if (!tryConsume(player, ninjaData, cost, 0, 40)) {
            return false;
        }
        registerCooldown(ninjaData, this.getTranslationKey());
        return true;
    }

    public static int getScaledHoldDuration(int mastery) {
        return (int) Math.round(30.0 * (1.0 + 0.10 * (Math.max(0, mastery) / 100.0)));
    }

    public static double getScaledRange(int mastery) {
        return 14.0D * (1.0 + 0.10 * (Math.max(0, mastery) / 100.0));
    }

    public static float getScaledDamage(int mastery) {
        return (float) (4.0 * (1.0 + 0.10 * (Math.max(0, mastery) / 100.0)));
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int holdTicks = getScaledHoldDuration(mastery);
        double maxReach = getScaledRange(mastery);
        float baseDamage = getScaledDamage(mastery);

        Vec3 look = player.getLookAngle();
        Vec3 horizDir = new Vec3(look.x, 0, look.z).normalize();
        if (horizDir.lengthSqr() < 0.001) {
            horizDir = new Vec3(0, 0, 1);
        }

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.5f, 0.7f);

        // Extend shadow line up to scaled reach
        LivingEntity capturedTarget = null;
        for (double d = 1.0; d <= maxReach; d += 0.6) {
            Vec3 pos = player.position().add(horizDir.scale(d));
            serverLevel.sendParticles(SHADOW_INK, pos.x, player.getY() + 0.1, pos.z, 3, 0.12, 0.02, 0.12, 0.0);

            AABB checkBox = new AABB(pos.x - 1.1, player.getY() - 1.5, pos.z - 1.1,
                    pos.x + 1.1, player.getY() + 2.5, pos.z + 1.1);
            List<LivingEntity> found = serverLevel.getEntitiesOfClass(LivingEntity.class, checkBox,
                    e -> e.isAlive() && e != player && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(player, e));
            if (!found.isEmpty()) {
                capturedTarget = found.get(0);
                break;
            }
        }

        if (capturedTarget == null) {
            Vec3 endSpot = player.position().add(horizDir.scale(5.5));
            com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity missShadow =
                    com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                            serverLevel, endSpot.x, player.getY(), endSpot.z,
                            com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_SHADOW_BIND,
                            24, player.getYRot());
            serverLevel.addFreshEntity(missShadow);
            return;
        }

        final LivingEntity boundEntity = capturedTarget;

        // Spawn 3D Shadow Bind Claws & Ribbon locked onto captured target for full holdTicks
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity shadowBind3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, boundEntity.getX(), boundEntity.getY(), boundEntity.getZ(),
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_SHADOW_BIND,
                        holdTicks, player.getYRot());
        shadowBind3D.setTargetId(boundEntity.getId());
        serverLevel.addFreshEntity(shadowBind3D);

        serverLevel.playSound(null, boundEntity.blockPosition(), SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.PLAYERS, 1.5f, 0.8f);
        boundEntity.hurt(player.damageSources().magic(), baseDamage);
        boundEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, holdTicks, 255, false, false));
        boundEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, holdTicks, 2, false, false));

        // Lock target movement & draw shadow tether for full holdTicks
        int steps = Math.max(1, holdTicks / 5);
        for (int step = 0; step < steps; step++) {
            final int s = step;
            ninjaData.scheduleDelayedTickEvent(p -> {
                if (!(p.level() instanceof ServerLevel sLevel) || !boundEntity.isAlive()) return;

                // Draw shadow tether between player and bound entity
                Vec3 pPos = p.position();
                Vec3 tPos = boundEntity.position();
                for (int i = 1; i <= 8; i++) {
                    double f = i / 8.0;
                    double lx = pPos.x + (tPos.x - pPos.x) * f;
                    double ly = pPos.y + (tPos.y - pPos.y) * f + 0.1;
                    double lz = pPos.z + (tPos.z - pPos.z) * f;
                    sLevel.sendParticles(SHADOW_INK, lx, ly, lz, 2, 0.06, 0.02, 0.06, 0.0);
                }

                // Full movement lock
                boundEntity.setDeltaMovement(0.0, Math.min(0.0, boundEntity.getDeltaMovement().y), 0.0);
                boundEntity.hurtMarked = true;

                // Final Shadow Neck Bind (Kage Kubishibari) constriction damage
                if (s == steps - 1) {
                    boundEntity.hurt(p.damageSources().magic(), baseDamage * 1.5f);
                    sLevel.sendParticles(SHADOW_INK, boundEntity.getX(), boundEntity.getY() + 1.0, boundEntity.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                }
            }, step * 5);
        }
    }
}
