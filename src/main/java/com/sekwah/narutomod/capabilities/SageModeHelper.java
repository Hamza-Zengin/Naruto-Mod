package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Pure, save-compatible helper managing Nature Energy gathering and active Sage Mode state
 * via player PersistentData NBT keys without mutating the NinjaData serialization schema.
 */
public final class SageModeHelper {

    public static final String NBT_NATURE_ENERGY = "narutomod_nature_energy";
    public static final String NBT_SAGE_TICKS = "narutomod_sage_ticks";
    public static final String NBT_PREV_X = "narutomod_sage_prev_x";
    public static final String NBT_PREV_Z = "narutomod_sage_prev_z";

    public static final float MAX_NATURE_ENERGY = 100.0F;
    public static final float BASE_NATURE_ENERGY_PER_TICK = 5.0F;
    public static final int BASE_SAGE_DURATION_TICKS = 600;
    public static final int MAX_SAGE_DURATION_TICKS = 750;

    public static final float SAGE_DAMAGE_MULTIPLIER = 1.30F;
    public static final float SAGE_CLOAK_CHARGE_MULTIPLIER = 1.35F;
    public static final float SAGE_CLOAK_DURATION_MULTIPLIER = 1.25F;
    public static final double STATIONARY_SPEED_SQR_THRESHOLD = 0.0025D;

    public static final double SENSING_RADIUS = 24.0D;
    public static final int SENSING_GLOW_TICKS = 25;

    private static final DustParticleOptions SAGE_AURA_DUST = new DustParticleOptions(0xFFFFAA22, 1.15F);

    private SageModeHelper() {}

    public static boolean isStationary(double horizontalSpeedSqr) {
        return horizontalSpeedSqr <= STATIONARY_SPEED_SQR_THRESHOLD;
    }

    public static boolean isWearingSageCloak(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() == NarutoItems.SAGE_CLOAK.get();
    }

    public static float computeNatureEnergyGainPerTick(boolean wearingSageCloak) {
        return BASE_NATURE_ENERGY_PER_TICK * (wearingSageCloak ? SAGE_CLOAK_CHARGE_MULTIPLIER : 1.0F);
    }

    public static int computeSageModeDurationTicks(boolean wearingSageCloak) {
        return wearingSageCloak
                ? Math.round(BASE_SAGE_DURATION_TICKS * SAGE_CLOAK_DURATION_MULTIPLIER)
                : BASE_SAGE_DURATION_TICKS;
    }

    public static float getNatureEnergy(@Nullable Player player) {
        return player == null ? 0.0F : getNatureEnergy(player.getPersistentData());
    }

    public static float getNatureEnergy(@Nullable CompoundTag persistentData) {
        if (persistentData == null) {
            return 0.0F;
        }
        float raw = persistentData.getFloatOr(NBT_NATURE_ENERGY, 0.0F);
        return Math.max(0.0F, Math.min(MAX_NATURE_ENERGY, raw));
    }

    public static void setNatureEnergy(@Nullable Player player, float energy) {
        if (player != null) {
            setNatureEnergy(player.getPersistentData(), energy);
        }
    }

    public static void setNatureEnergy(@Nullable CompoundTag persistentData, float energy) {
        if (persistentData != null) {
            float clamped = Math.max(0.0F, Math.min(MAX_NATURE_ENERGY, energy));
            persistentData.putFloat(NBT_NATURE_ENERGY, clamped);
        }
    }

    public static int getSageModeTicks(@Nullable Player player) {
        return player == null ? 0 : getSageModeTicks(player.getPersistentData());
    }

    public static int getSageModeTicks(@Nullable CompoundTag persistentData) {
        if (persistentData == null) {
            return 0;
        }
        int raw = persistentData.getIntOr(NBT_SAGE_TICKS, 0);
        return Math.max(0, Math.min(MAX_SAGE_DURATION_TICKS, raw));
    }

    public static void setSageModeTicks(@Nullable Player player, int ticks) {
        if (player != null) {
            setSageModeTicks(player.getPersistentData(), ticks);
        }
    }

    public static void setSageModeTicks(@Nullable CompoundTag persistentData, int ticks) {
        if (persistentData != null) {
            int clamped = Math.max(0, Math.min(MAX_SAGE_DURATION_TICKS, ticks));
            persistentData.putInt(NBT_SAGE_TICKS, clamped);
        }
    }

    public static boolean isSageModeActive(@Nullable Player player) {
        return getSageModeTicks(player) > 0;
    }

    public static boolean isSageModeActive(@Nullable CompoundTag persistentData) {
        return getSageModeTicks(persistentData) > 0;
    }

    public static float applySageModeDamageMultiplier(float amount, @Nullable Player attacker) {
        return applySageModeDamageMultiplier(amount, isSageModeActive(attacker));
    }

    public static float applySageModeDamageMultiplier(float amount, boolean sageModeActive) {
        return sageModeActive ? amount * SAGE_DAMAGE_MULTIPLIER : amount;
    }

    public static void activateSageMode(@Nullable Player player, boolean wearingSageCloak) {
        if (player == null) {
            return;
        }
        setSageModeTicks(player, computeSageModeDurationTicks(wearingSageCloak));
        setNatureEnergy(player, 0.0F);
    }

    public static void deactivateSageMode(@Nullable Player player) {
        if (player == null) {
            return;
        }
        setSageModeTicks(player, 0);
        setNatureEnergy(player, 0.0F);
    }

    /**
     * Advances active Sage Mode timer, sustains physical/sensory buffs, emits golden-orange
     * Nature Energy aura particles, and marks nearby hostile entities with short Glowing sensing.
     */
    public static void tickServer(@Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            if (isSageModeActive(player) || getNatureEnergy(player) > 0.0F) {
                deactivateSageMode(player);
            }
            return;
        }

        int remaining = getSageModeTicks(player);
        if (remaining <= 0) {
            return;
        }

        remaining--;
        setSageModeTicks(player, remaining);

        if (remaining == 0) {
            player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.ended")
                    .withStyle(ChatFormatting.YELLOW));
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.1F);
            return;
        }

        ensureSustainedEffect(player, MobEffects.SPEED, 45, 1);
        ensureSustainedEffect(player, MobEffects.JUMP_BOOST, 45, 0);
        ensureRegenEffect(player, 75, 0);

        if (player.level() instanceof ServerLevel serverLevel) {
            if (player.tickCount % 4 == 0) {
                serverLevel.sendParticles(SAGE_AURA_DUST,
                        player.getX(), player.getY() + 0.95D, player.getZ(),
                        3, 0.35D, 0.45D, 0.35D, 0.02D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        1, 0.25D, 0.4D, 0.25D, 0.01D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            if (player.tickCount % 10 == 0) {
                AABB senseBox = player.getBoundingBox().inflate(SENSING_RADIUS);
                double maxDistSq = SENSING_RADIUS * SENSING_RADIUS;
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, senseBox)) {
                    if (target == player || !target.isAlive() || target.isSpectator()) {
                        continue;
                    }
                    if (ShadowCloneEntity.areAllies(player, target) || player.isAlliedTo(target)) {
                        continue;
                    }
                    if (!(target instanceof Monster) && !(target instanceof Player)) {
                        continue;
                    }
                    if (target.distanceToSqr(player) <= maxDistSq) {
                        target.addEffect(new MobEffectInstance(
                                MobEffects.GLOWING, SENSING_GLOW_TICKS, 0, false, false, false));
                    }
                }
            }
        }
    }

    private static void ensureSustainedEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getAmplifier() < amplifier
                || existing.getDuration() <= 20 || player.tickCount % 10 == 0) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
        }
    }

    private static void ensureRegenEffect(Player player, int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(MobEffects.REGENERATION);
        if (existing == null || existing.getAmplifier() < amplifier || existing.getDuration() <= 25) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, false, false, false));
        }
    }
}
