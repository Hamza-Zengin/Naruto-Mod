package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
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
 * Pure, save-compatible helper managing Nature Energy gathering and 3-stage active Sage Mode state
 * via player PersistentData NBT keys without mutating the NinjaData serialization schema.
 */
public final class SageModeHelper {

    public static final String NBT_NATURE_ENERGY = "narutomod_nature_energy";
    public static final String NBT_SAGE_TICKS = "narutomod_sage_ticks";
    public static final String SAGE_STAGE_TAG = "narutomod:sage_stage";
    public static final String NBT_PREV_X = "narutomod_sage_prev_x";
    public static final String NBT_PREV_Z = "narutomod_sage_prev_z";

    public static final Identifier SAGE_MODE_ID =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sage_mode");
    public static final Identifier SAGE_PIGMENT_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/eyes/sage_pigment.png");
    public static final Identifier SAGE_EYES_1_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/eyes/sage_eyes_1.png");
    public static final Identifier SAGE_EYES_2_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/eyes/sage_eyes_2.png");
    public static final Identifier SAGE_EYES_3_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/eyes/sage_eyes_3.png");

    public static final int STAGE_2_MASTERY_THRESHOLD = 500;
    public static final int STAGE_3_MASTERY_THRESHOLD = 1000;

    public static final float MAX_NATURE_ENERGY = 100.0F;
    public static final float STAGE_1_NATURE_ENERGY_PER_TICK = 1.25F;
    public static final float STAGE_2_NATURE_ENERGY_PER_TICK = 2.2F;
    public static final float STAGE_3_NATURE_ENERGY_PER_TICK = 3.8F;
    public static final float BASE_NATURE_ENERGY_PER_TICK = STAGE_1_NATURE_ENERGY_PER_TICK;

    public static final int STAGE_1_DURATION_TICKS = 400;
    public static final int STAGE_2_DURATION_TICKS = 700;
    public static final int STAGE_3_DURATION_TICKS = 1100;
    public static final int BASE_SAGE_DURATION_TICKS = STAGE_1_DURATION_TICKS;

    public static final float SAGE_CLOAK_CHARGE_MULTIPLIER = 1.35F;
    public static final float SAGE_CLOAK_DURATION_MULTIPLIER = 1.25F;
    public static final int MAX_SAGE_DURATION_TICKS =
            Math.round(STAGE_3_DURATION_TICKS * SAGE_CLOAK_DURATION_MULTIPLIER);

    public static final float STAGE_1_DAMAGE_MULTIPLIER = 1.20F;
    public static final float STAGE_2_DAMAGE_MULTIPLIER = 1.32F;
    public static final float STAGE_3_DAMAGE_MULTIPLIER = 1.45F;
    public static final float SAGE_DAMAGE_MULTIPLIER = STAGE_1_DAMAGE_MULTIPLIER;

    public static final double STATIONARY_SPEED_SQR_THRESHOLD = 0.0025D;

    public static final double STAGE_1_SENSING_RADIUS = 16.0D;
    public static final double STAGE_2_SENSING_RADIUS = 26.0D;
    public static final double STAGE_3_SENSING_RADIUS = 36.0D;
    public static final double SENSING_RADIUS = STAGE_1_SENSING_RADIUS;
    public static final int SENSING_GLOW_TICKS = 25;

    private static final DustParticleOptions SAGE_AURA_DUST = new DustParticleOptions(0xFFFFAA22, 1.15F);

    private SageModeHelper() {}

    public static int getSageStage(int mastery) {
        if (mastery >= STAGE_3_MASTERY_THRESHOLD) {
            return 3;
        }
        if (mastery >= STAGE_2_MASTERY_THRESHOLD) {
            return 2;
        }
        return 1;
    }

    public static int getSageStage(@Nullable INinjaData ninjaData) {
        if (ninjaData == null) {
            return 1;
        }
        return getSageStage(ninjaData.getMastery(SAGE_MODE_ID));
    }

    public static int getSageStage(@Nullable Player player) {
        if (player == null) {
            return 1;
        }
        INinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        return getSageStage(ninjaData);
    }

    public static int clampStage(int stage) {
        return Math.max(1, Math.min(3, stage));
    }

    public static Identifier getSageEyeTexture(int stage) {
        return switch (clampStage(stage)) {
            case 2 -> SAGE_EYES_2_TEXTURE;
            case 3 -> SAGE_EYES_3_TEXTURE;
            default -> SAGE_EYES_1_TEXTURE;
        };
    }

    public static boolean isStationary(double horizontalSpeedSqr) {
        return horizontalSpeedSqr <= STATIONARY_SPEED_SQR_THRESHOLD;
    }

    public static boolean isWearingSageCloak(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() == NarutoItems.SAGE_CLOAK.get();
    }

    public static float getBaseNatureEnergyGainPerTick(int stage) {
        return switch (clampStage(stage)) {
            case 2 -> STAGE_2_NATURE_ENERGY_PER_TICK;
            case 3 -> STAGE_3_NATURE_ENERGY_PER_TICK;
            default -> STAGE_1_NATURE_ENERGY_PER_TICK;
        };
    }

    public static float computeNatureEnergyGainPerTick(int stage, boolean wearingSageCloak) {
        return getBaseNatureEnergyGainPerTick(stage) * (wearingSageCloak ? SAGE_CLOAK_CHARGE_MULTIPLIER : 1.0F);
    }

    public static float computeNatureEnergyGainPerTick(boolean wearingSageCloak) {
        return computeNatureEnergyGainPerTick(1, wearingSageCloak);
    }

    public static int getBaseSageDurationTicks(int stage) {
        return switch (clampStage(stage)) {
            case 2 -> STAGE_2_DURATION_TICKS;
            case 3 -> STAGE_3_DURATION_TICKS;
            default -> STAGE_1_DURATION_TICKS;
        };
    }

    public static int computeSageModeDurationTicks(int stage, boolean wearingSageCloak) {
        int base = getBaseSageDurationTicks(stage);
        return wearingSageCloak ? Math.round(base * SAGE_CLOAK_DURATION_MULTIPLIER) : base;
    }

    public static int computeSageModeDurationTicks(boolean wearingSageCloak) {
        return computeSageModeDurationTicks(1, wearingSageCloak);
    }

    public static float getSageDamageMultiplier(int stage) {
        if (stage <= 0) {
            return 1.0F;
        }
        return switch (clampStage(stage)) {
            case 2 -> STAGE_2_DAMAGE_MULTIPLIER;
            case 3 -> STAGE_3_DAMAGE_MULTIPLIER;
            default -> STAGE_1_DAMAGE_MULTIPLIER;
        };
    }

    public static float getActiveSageDamageMultiplier(@Nullable Player player) {
        return getSageDamageMultiplier(getActiveSageStage(player));
    }

    public static double getSageSensoryRadius(int stage) {
        if (stage <= 0) {
            return 0.0D;
        }
        return switch (clampStage(stage)) {
            case 2 -> STAGE_2_SENSING_RADIUS;
            case 3 -> STAGE_3_SENSING_RADIUS;
            default -> STAGE_1_SENSING_RADIUS;
        };
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

    public static int getActiveSageStage(@Nullable Player player) {
        return player == null ? 0 : getActiveSageStage(player.getPersistentData());
    }

    public static int getActiveSageStage(@Nullable CompoundTag persistentData) {
        if (!isSageModeActive(persistentData)) {
            return 0;
        }
        int raw = persistentData.getIntOr(SAGE_STAGE_TAG, 1);
        return clampStage(raw);
    }

    public static void setActiveSageStage(@Nullable Player player, int stage) {
        if (player != null) {
            setActiveSageStage(player.getPersistentData(), stage);
        }
    }

    public static void setActiveSageStage(@Nullable CompoundTag persistentData, int stage) {
        if (persistentData != null) {
            int sanitized = stage <= 0 ? 0 : clampStage(stage);
            persistentData.putInt(SAGE_STAGE_TAG, sanitized);
        }
    }

    public static boolean isSageModeActive(@Nullable Player player) {
        return getSageModeTicks(player) > 0;
    }

    public static boolean isSageModeActive(@Nullable CompoundTag persistentData) {
        return getSageModeTicks(persistentData) > 0;
    }

    public static float applySageModeDamageMultiplier(float amount, @Nullable Player attacker) {
        return amount * getActiveSageDamageMultiplier(attacker);
    }

    public static float applySageModeDamageMultiplier(float amount, int stage) {
        return amount * getSageDamageMultiplier(stage);
    }

    public static float applySageModeDamageMultiplier(float amount, boolean sageModeActive) {
        return sageModeActive ? amount * SAGE_DAMAGE_MULTIPLIER : amount;
    }

    public static void activateSageMode(@Nullable Player player, boolean wearingSageCloak) {
        if (player == null) {
            return;
        }
        activateSageMode(player, getSageStage(player), wearingSageCloak);
    }

    public static void activateSageMode(@Nullable Player player, int stage, boolean wearingSageCloak) {
        if (player == null) {
            return;
        }
        int clampedStage = clampStage(stage);
        setSageModeTicks(player, computeSageModeDurationTicks(clampedStage, wearingSageCloak));
        setActiveSageStage(player, clampedStage);
        setNatureEnergy(player, 0.0F);
    }

    public static void deactivateSageMode(@Nullable Player player) {
        if (player == null) {
            return;
        }
        setSageModeTicks(player, 0);
        setActiveSageStage(player, 0);
        setNatureEnergy(player, 0.0F);
    }

    public static void applyStageBuffs(@Nullable Player player, int stage) {
        if (player == null || stage <= 0) {
            return;
        }
        int clamped = clampStage(stage);
        int speedAmp = clamped >= 2 ? 1 : 0;
        ensureSustainedEffect(player, MobEffects.SPEED, 45, speedAmp);
        if (clamped >= 2) {
            int jumpAmp = clamped >= 3 ? 1 : 0;
            ensureSustainedEffect(player, MobEffects.JUMP_BOOST, 45, jumpAmp);
        }
        if (clamped >= 3) {
            ensureRegenEffect(player, 75, 0);
            ensureSustainedEffect(player, MobEffects.RESISTANCE, 45, 0);
        }
    }

    /**
     * Advances active Sage Mode timer, sustains stage-scaled physical/sensory buffs, emits golden-orange
     * Nature Energy aura particles, and marks nearby hostile entities with short Glowing sensing.
     */
    public static void tickServer(@Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            if (isSageModeActive(player) || getNatureEnergy(player) > 0.0F || getActiveSageStage(player) > 0) {
                deactivateSageMode(player);
            }
            return;
        }

        int remaining = getSageModeTicks(player);
        if (remaining <= 0) {
            return;
        }

        int stage = getActiveSageStage(player);
        remaining--;
        setSageModeTicks(player, remaining);

        if (remaining == 0) {
            setActiveSageStage(player, 0);
            player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.ended")
                    .withStyle(ChatFormatting.YELLOW));
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.1F);
            return;
        }

        applyStageBuffs(player, stage);

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
                double sensingRadius = getSageSensoryRadius(stage);
                AABB senseBox = player.getBoundingBox().inflate(sensingRadius);
                double maxDistSq = sensingRadius * sensingRadius;
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
