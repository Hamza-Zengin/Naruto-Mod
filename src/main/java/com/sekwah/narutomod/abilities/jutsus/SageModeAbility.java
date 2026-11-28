package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.capabilities.SageModeHelper;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Sennin Mōdo (Sage Mode)
 * Channeled senjutsu unlocked by defeating the Myōbokuzan Sage Trial Boss.
 * Requires standing still while channeling to gather Nature Energy up to 100%,
 * scaling across 3 mastery stages in charge speed, duration, damage (+20%/+32%/+45%),
 * buffs, and sensory awareness (16/26/36 blocks).
 */
public class SageModeAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public boolean requiresScroll() {
        return true;
    }

    @Override
    public float getDefaultChakraCost() {
        return 0.0F;
    }

    @Override
    public float getDefaultStaminaCost() {
        return 0.0F;
    }

    @Override
    public int getDefaultCooldown() {
        return 200;
    }

    @Override
    public float getDefaultDamage() {
        return 0.0F;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean useChargedMessages() {
        return true;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (player != null && player.isSpectator()) {
            return false;
        }
        if (!isExplicitlyUnlocked(ninjaData)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.requires_trial")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return player == null || !player.isSpectator();
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        CompoundTag persistent = player.getPersistentData();
        if (ticksChanneled == 0) {
            SageModeHelper.setNatureEnergy(persistent, 0.0F);
            persistent.putDouble(SageModeHelper.NBT_PREV_X, player.getX());
            persistent.putDouble(SageModeHelper.NBT_PREV_Z, player.getZ());
        }

        double speedSqr = player.getDeltaMovement().horizontalDistanceSqr();
        if (ticksChanneled > 0) {
            double dx = player.getX() - persistent.getDoubleOr(SageModeHelper.NBT_PREV_X, player.getX());
            double dz = player.getZ() - persistent.getDoubleOr(SageModeHelper.NBT_PREV_Z, player.getZ());
            speedSqr = Math.max(speedSqr, dx * dx + dz * dz);
        }
        persistent.putDouble(SageModeHelper.NBT_PREV_X, player.getX());
        persistent.putDouble(SageModeHelper.NBT_PREV_Z, player.getZ());

        if (!SageModeHelper.isStationary(speedSqr)) {
            if (ticksChanneled % 10 == 0) {
                player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.must_stand_still")
                        .withStyle(ChatFormatting.YELLOW));
            }
            return;
        }

        Identifier reg = this.getRegistryName() != null ? this.getRegistryName() : SageModeHelper.SAGE_MODE_ID;
        int mastery = ninjaData != null ? ninjaData.getMastery(reg) : 0;
        int stage = SageModeHelper.getSageStage(mastery);
        boolean wearingCloak = SageModeHelper.isWearingSageCloak(player);
        float gain = SageModeHelper.computeNatureEnergyGainPerTick(stage, wearingCloak);
        float nextEnergy = Math.min(
                SageModeHelper.MAX_NATURE_ENERGY,
                SageModeHelper.getNatureEnergy(persistent) + gain);
        SageModeHelper.setNatureEnergy(persistent, nextEnergy);

        if (player.level() instanceof ServerLevel serverLevel) {
            if (ticksChanneled % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 0.9D, player.getZ(),
                        3, 0.45D, 0.55D, 0.45D, 0.02D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.1D, player.getZ(),
                        1, 0.3D, 0.45D, 0.3D, 0.02D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            int pct = Math.min(100, Math.round(nextEnergy));
            if (ticksChanneled % 2 == 0 || nextEnergy >= SageModeHelper.MAX_NATURE_ENERGY) {
                player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.charging", pct)
                        .withStyle(ChatFormatting.GREEN));
            }

            if (nextEnergy >= SageModeHelper.MAX_NATURE_ENERGY) {
                SageModeHelper.activateSageMode(player, stage, wearingCloak);
                if (this.getRegistryName() != null && ninjaData != null) {
                    ninjaData.incrementJutsuMastery(this.getRegistryName(), false);
                    ninjaData.addJutsuCast();
                }

                serverLevel.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                        SoundSource.PLAYERS, 0.75F, 1.15F);
                serverLevel.playSound(null, player.blockPosition(), NarutoSounds.JUTSU_CAST.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        24, 0.5D, 0.6D, 0.5D, 0.15D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        16, 0.45D, 0.5D, 0.45D, 0.08D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

                player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.activated")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                if (ninjaData != null) {
                    this.registerCooldown(ninjaData, this.getTranslationKey());
                    ninjaData.setCurrentlyChanneledAbility(player, null);
                }
            }
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player != null && !player.level().isClientSide()) {
            if (SageModeHelper.getNatureEnergy(player) < SageModeHelper.MAX_NATURE_ENERGY) {
                SageModeHelper.setNatureEnergy(player, 0.0F);
                player.sendOverlayMessage(Component.translatable("jutsu.sage_mode.cancelled")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}
