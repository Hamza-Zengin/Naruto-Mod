package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.network.PacketDistributor;

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
            if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                        .trigger(attacker, NinjaProgressTrigger.Action.ELEMENTAL_COMBO, "water_lightning");
            }
        }

        // 2. EARTH + LIGHTNING (Grounding via MudWallEntity)
        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos != null && isShieldedByMudWall(serverLevel, sourcePos, target)) {
            amount *= NarutoConfig.earthLightningDamageMultiplier;
            if (target instanceof ServerPlayer defender) {
                NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                        .trigger(defender, NinjaProgressTrigger.Action.ELEMENTAL_COMBO, "earth_lightning");
            }
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
        if (fireball.getOwner() instanceof ServerPlayer owner) {
            NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .trigger(owner, NinjaProgressTrigger.Action.ELEMENTAL_COMBO, "wind_fire");
        }
        if (fireball.level() instanceof ServerLevel serverLevel) {
            triggerComboShockwave(serverLevel, fireball.position(), 0xFF6622);
        }
    }

    /**
     * Applies Fire + Earth Molten synergy to an EarthBulletEntity passing near a FireballJutsuEntity.
     */
    public static void igniteEarthWithFire(
            com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity earthBullet,
            FireballJutsuEntity fireball) {
        if (earthBullet == null || fireball == null || earthBullet.isMolten()) {
            return;
        }
        earthBullet.setMolten(true);
        earthBullet.setDamage(earthBullet.getDamage() * 1.35F);
        if (earthBullet.getOwner() instanceof ServerPlayer owner) {
            NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .trigger(owner, NinjaProgressTrigger.Action.ELEMENTAL_COMBO, "fire_earth");
        }
        if (earthBullet.level() instanceof ServerLevel serverLevel) {
            triggerComboShockwave(serverLevel, earthBullet.position(), 0xFF4500);
        }
    }

    private static void triggerComboShockwave(ServerLevel level, Vec3 pos, int colorHex) {
        final float maxRadius = 16.0f;
        final double maxRadiusSq = maxRadius * maxRadius;
        ClientCameraShakePacket shakePacket = new ClientCameraShakePacket(0.75f, 10, pos, maxRadius);
        ClientJutsuEffectPacket burstPacket = new ClientJutsuEffectPacket(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "elemental_burst"),
                pos,
                new Vec3(0.0, 1.0, 0.0),
                1.5f,
                colorHex
        );
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(pos) <= maxRadiusSq) {
                PacketDistributor.sendToPlayer(player, shakePacket);
                PacketDistributor.sendToPlayer(player, burstPacket);
            }
        }
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
