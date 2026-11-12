package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Kirin (Raiton: Kirin - 雷遁·麒麟)
 * S-Rank Lightning Release technique that summons natural lightning in the shape of a divine dragon.
 */
public class KirinAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.LIGHTNING;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 2113; // V - C - C - B
    }

    @Override
    public int getCooldown() {
        return 220; // 11 seconds cooldown
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        boolean isStormy = player.level().isThundering() || player.level().isRaining();
        float baseChakra = isStormy ? 85.0f : 130.0f; // 35% natural lightning storm discount
        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float chakraCost = baseChakra * getChakraCostMultiplier(mastery);
        float staminaCost = isStormy ? 45.0f : 60.0f;

        return tryConsume(player, ninjaData, chakraCost, staminaCost, 45);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Summoning sound at caster position: crackling thunder from the sky
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.85f);

        double maxRange = 36.0D;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookDir.scale(maxRange));

        // 1. Raycast blocks
        HitResult blockHit = serverLevel.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = (blockHit.getType() != HitResult.Type.MISS) ? eyePos.distanceTo(blockHit.getLocation()) : maxRange;

        // 2. Raycast entities
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(maxDist)).inflate(1.5D);
        List<LivingEntity> possibleTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive() && !e.isSpectator());

        LivingEntity directEntity = null;
        double closestDist = maxDist;
        Vec3 targetPos = blockHit.getLocation();

        for (LivingEntity e : possibleTargets) {
            AABB entityBox = e.getBoundingBox().inflate(0.4D);
            Optional<Vec3> optHit = entityBox.clip(eyePos, endPos);
            if (optHit.isPresent()) {
                double dist = eyePos.distanceTo(optHit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    directEntity = e;
                    targetPos = optHit.get();
                }
            }
        }

        BlockPos targetBlock = BlockPos.containing(targetPos);

        // Spawn Colossal 3D Lightning Dragon descending from the sky
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity kirinDragon =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, targetPos.x, targetPos.y, targetPos.z,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON,
                        38, player.getYRot());
        serverLevel.addFreshEntity(kirinDragon);

        // Lightning strike
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightningBolt != null) {
            lightningBolt.moveTo(targetPos);
            lightningBolt.setVisualOnly(false);
            serverLevel.addFreshEntity(lightningBolt);
        }

        // Thunderous boom and electric particles (majestic, fitting, crisp audio)
        serverLevel.playSound(null, targetBlock, NarutoSounds.KIRIN_STRIKE.get(), SoundSource.WEATHER, 3.5f, 0.85f);
        serverLevel.playSound(null, targetBlock, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.5f, 0.75f);
        serverLevel.playSound(null, targetBlock, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.WEATHER, 1.8f, 0.6f);
        serverLevel.playSound(null, targetBlock, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4f, 1.1f);

        // Huge vertical electric dragon beam
        for (double y = targetPos.y; y < targetPos.y + 25; y += 0.5) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    targetPos.x, y, targetPos.z,
                    6, 0.5, 0.2, 0.5, 0.08);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    targetPos.x, y, targetPos.z,
                    1, 0.1, 0.1, 0.1, 0.0);
        }

        // Expanding circular electric spark ring
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double vx = Math.cos(angle) * 0.35;
            double vz = Math.sin(angle) * 0.35;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    targetPos.x + Math.cos(angle) * 1.5,
                    targetPos.y + 0.2,
                    targetPos.z + Math.sin(angle) * 1.5,
                    1, vx, 0.04, vz, 0.12);
        }

        // Sky-charging arc: lightning guides from caster's hand up towards the heavens
        for (double y = eyePos.y; y < eyePos.y + 25; y += 1.2) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    eyePos.x + (player.getRandom().nextDouble() - 0.5) * 0.4,
                    y,
                    eyePos.z + (player.getRandom().nextDouble() - 0.5) * 0.4,
                    2, 0.05, 0.1, 0.05, 0.04);
        }

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);

        // Direct damage if mob directly struck: 50.0f damage * powerMultiplier
        if (directEntity != null) {
            directEntity.hurt(serverLevel.damageSources().lightningBolt(), 50.0f * powerMult);
            directEntity.setRemainingFireTicks(140);
            directEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)); // Slowness IV
        }

        // Massive electric shockwave blast: 28.0f damage * powerMultiplier in scaled radius
        double blastRadius = 6.5 + (getMasteryTier(mastery) >= 4 ? 2.0 : 0.0);
        final LivingEntity finalDirectTarget = directEntity;
        AABB blastArea = new AABB(targetPos.x - blastRadius, targetPos.y - 3.0, targetPos.z - blastRadius,
                targetPos.x + blastRadius, targetPos.y + 5.0, targetPos.z + blastRadius);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e != finalDirectTarget && e.isAlive());

        for (LivingEntity victim : victims) {
            victim.hurt(serverLevel.damageSources().lightningBolt(), 28.0f * powerMult);
            Vec3 knockback = victim.position().subtract(targetPos).normalize().scale(2.2);
            victim.push(knockback.x, 0.85, knockback.z);
            victim.setRemainingFireTicks(80);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
        }
    }
}
