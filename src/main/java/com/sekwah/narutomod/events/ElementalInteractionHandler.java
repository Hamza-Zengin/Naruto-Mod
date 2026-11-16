package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Event-driven elemental synergy handler using ChakraElement rules without per-tick area scanning:
 * - WATER + LIGHTNING (Conductivity): Wet targets take +25% lightning damage and brief Slowness.
 * - WIND + FIRE (Fanning): FireballJutsuEntity boosted by Wind gains +20% speed and +20% explosion radius.
 * - EARTH + LIGHTNING (Grounding): Targets shielded behind MudWallEntity take 50% less lightning damage.
 */
@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class ElementalInteractionHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) {
            return;
        }

        float amount = event.getAmount();

        // 1. WATER + LIGHTNING (Conductivity)
        if (isWetOrNearWaterBullet(serverLevel, target)) {
            amount *= NarutoConfig.waterLightningDamageMultiplier;
            if (NarutoConfig.waterLightningSlownessTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, NarutoConfig.waterLightningSlownessTicks, 1));
            }
        }

        // 2. EARTH + LIGHTNING (Grounding via MudWallEntity)
        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos != null && isShieldedByMudWall(serverLevel, sourcePos, target)) {
            amount *= NarutoConfig.earthLightningDamageMultiplier;
        }

        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FireballJutsuEntity fireball)) {
            return;
        }
        if (fireball.level().isClientSide() || fireball.isWindBoosted()) {
            return;
        }
        AABB checkBox = fireball.getBoundingBox().inflate(2.5D);
        List<JutsuVisualEffectEntity> windEffects = fireball.level().getEntitiesOfClass(
                JutsuVisualEffectEntity.class,
                checkBox,
                e -> e.isAlive() && e.getVisualType() == JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE
        );
        if (!windEffects.isEmpty()) {
            boostFireballWithWind(fireball);
        }
    }

    /**
     * Applies Wind + Fire synergy (ChakraElement.WIND fanning ChakraElement.FIRE) to a FireballJutsuEntity once.
     */
    public static void boostFireballWithWind(FireballJutsuEntity fireball) {
        if (fireball == null || fireball.isWindBoosted() || fireball.getElement() != ChakraElement.FIRE) {
            return;
        }
        fireball.markWindBoosted();
        fireball.setDeltaMovement(fireball.getDeltaMovement().scale(NarutoConfig.windFireSpeedMultiplier));
        fireball.explosionPower *= NarutoConfig.windFireRadiusMultiplier;
        fireball.needsSync = true;
    }

    private static boolean isWetOrNearWaterBullet(ServerLevel level, LivingEntity target) {
        if (target.isInWaterOrRain()) {
            return true;
        }
        AABB checkBox = target.getBoundingBox().inflate(2.5D);
        return !level.getEntitiesOfClass(WaterBulletEntity.class, checkBox, WaterBulletEntity::isAlive).isEmpty();
    }

    private static boolean isShieldedByMudWall(ServerLevel level, Vec3 sourcePos, LivingEntity target) {
        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        AABB sweepBox = new AABB(sourcePos, targetPos).inflate(1.5D);
        List<MudWallEntity> walls = level.getEntitiesOfClass(MudWallEntity.class, sweepBox, MudWallEntity::isAlive);
        for (MudWallEntity wall : walls) {
            if (wall.getPhysicalBarrierBox().inflate(0.5D).clip(sourcePos, targetPos).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
