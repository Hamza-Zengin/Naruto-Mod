package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.WorldProtectionHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Amaterasu (Enton / Blaze Release)
 * Uchiha S-rank forbidden technique: Inextinguishable black flames that erupt directly at the focal point of vision.
 */
public class AmaterasuAbility extends Ability implements Ability.Cooldown {

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
        return 12321; // C - C - V - B
    }

    @Override
    public float getDefaultChakraCost() {
        return 110.0f;
    }

    @Override
    public float getDefaultStaminaCost() {
        return 50.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 180;
    }

    @Override
    public float getDefaultDamage() {
        return 38.0f;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sharingan"));
    }

    @Override
    public int getRequiredMastery() {
        return SharinganAbility.SharinganStage.MANGEKYOU.getMinMastery();
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!canActivate(player, ninjaData)) {
            return false;
        }

        int castCooldown = Math.round(getExtraParam("cast_cooldown_ticks", 45.0f));
        return tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), getStaminaCost(), castCooldown);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        double maxRange = 32.0D;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookDir.scale(maxRange));

        // 1. Raycast for blocks
        HitResult blockHit = serverLevel.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = (blockHit.getType() != HitResult.Type.MISS) ? eyePos.distanceTo(blockHit.getLocation()) : maxRange;

        // 2. Raycast for entities along the line of sight
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(maxDist)).inflate(1.5D);
        List<LivingEntity> possibleTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive() && !e.isSpectator());

        LivingEntity directEntity = null;
        double closestDist = maxDist;
        Vec3 targetPos = blockHit.getLocation();

        for (LivingEntity e : possibleTargets) {
            AABB entityBox = e.getBoundingBox().inflate(0.35D);
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

        // Play sinister ignition sound
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.AMATERASU_IGNITE.get(), SoundSource.PLAYERS, 1.4f, 0.9f);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0f, 0.5f);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.5f);

        // Mangekyo Ocular Bleeding visual: subtle crimson blood drop and dark spark from caster eye
        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, eyePos.x, eyePos.y - 0.1, eyePos.z, 6, 0.05, 0.05, 0.05, 0.02, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, eyePos.x, eyePos.y - 0.15, eyePos.z, 1, 0.0, 0.0, 0.0, 0.0, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        // Dark chakra burst at impact
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, targetPos.x, targetPos.y + 0.6, targetPos.z, 20, 0.4, 0.4, 0.4, 0.08, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        // Black flame & smoke beam from caster's eye to target
        double dist = eyePos.distanceTo(targetPos);
        Vec3 dir = targetPos.subtract(eyePos).normalize();
        for (double d = 0; d < dist; d += 0.7) {
            Vec3 point = eyePos.add(dir.scale(d));
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.01, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0.01, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Spawn 3D Inextinguishable Black-Flame Shroud & Pillars at focal target
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity amaterasu3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, targetPos.x, targetPos.y, targetPos.z,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR,
                        90, player.getYRot());
        amaterasu3D.setCasterUUID(player.getUUID());
        if (directEntity != null) {
            amaterasu3D.setTargetId(directEntity.getId());
        }
        serverLevel.addFreshEntity(amaterasu3D);

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float directDamage = this.getDamage(ninjaData);
        int fireTicks = (int) (300 * powerMult);
        int witherTicks = (int) (180 * powerMult);

        // Direct Entity Target Impact: damage scaled with mastery
        if (directEntity != null) {
            directEntity.hurtServer(serverLevel, NarutoDamageSource.amaterasu(serverLevel, player, player), directDamage);
            directEntity.setRemainingFireTicks(fireTicks); // Inextinguishable black flames
            directEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 3)); // Wither IV
            directEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2)); // Slowness III
            directEntity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));

            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    directEntity.getX(), directEntity.getY() + directEntity.getBbHeight() * 0.5, directEntity.getZ(),
                    35, 0.4, 0.6, 0.4, 0.05, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    directEntity.getX(), directEntity.getY() + directEntity.getBbHeight() * 0.5, directEntity.getZ(),
                    15, 0.3, 0.4, 0.3, 0.02, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Area Blast around target point: splash damage (approx 47% of direct damage)
        final LivingEntity finalDirectTarget = directEntity;
        AABB hitBox = new AABB(targetPos.x - 4.0, targetPos.y - 2.5, targetPos.z - 4.0,
                targetPos.x + 4.0, targetPos.y + 3.0, targetPos.z + 4.0);
        List<LivingEntity> splashTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e != finalDirectTarget && e.isAlive());

        for (LivingEntity splash : splashTargets) {
            splash.hurtServer(serverLevel, NarutoDamageSource.amaterasu(serverLevel, player, player), directDamage * 0.47f);
            splash.setRemainingFireTicks((int) (200 * powerMult));
            splash.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (140 * powerMult), 1)); // Wither II
            splash.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));

            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    splash.getX(), splash.getY() + 1.0, splash.getZ(),
                    20, 0.3, 0.5, 0.3, 0.03, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Place temporary Amaterasu black flames at target feet or hit block surface (if permitted by config & protection rules)
        if (canPlaceAmaterasuFire(serverLevel, player)) {
            BlockPos focalGroundPos = resolveFocalBlockPos(serverLevel, directEntity, blockHit, targetPos);
            placeAmaterasuFirePatch(serverLevel, focalGroundPos, 1, player);
        }
    }

    public static boolean canPlaceAmaterasuFire(ServerLevel serverLevel, Player player) {
        return NarutoConfig.jutsuWorldDamage
                && NarutoConfig.amaterasuPlaceFire;
    }

    private static BlockPos resolveFocalBlockPos(ServerLevel level, LivingEntity directEntity, HitResult blockHit, Vec3 targetPos) {
        if (directEntity != null) {
            BlockPos feetPos = directEntity.blockPosition();
            for (int dy = 0; dy >= -2; dy--) {
                BlockPos check = feetPos.offset(0, dy, 0);
                if (level.getBlockState(check).isAir() && !level.getBlockState(check.below()).isAir()) {
                    return check;
                }
            }
            return feetPos;
        }
        if (blockHit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos().relative(bhr.getDirection());
        }
        return BlockPos.containing(targetPos);
    }

    private static void placeAmaterasuFirePatch(ServerLevel level, BlockPos center, int radius, Player player) {
        BlockState amaterasuState = NarutoBlocks.AMATERASU_FIRE.get().defaultBlockState();
        int clampedRadius = Math.max(1, Math.min(radius, 2));
        for (int dx = -clampedRadius; dx <= clampedRadius; dx++) {
            for (int dz = -clampedRadius; dz <= clampedRadius; dz++) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).isAir()
                            && amaterasuState.canSurvive(level, candidate)
                            && WorldProtectionHelper.canModifyBlock(level, candidate, player)) {
                        level.setBlockAndUpdate(candidate, amaterasuState);
                        break;
                    }
                }
            }
        }
    }
}
