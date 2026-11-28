package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker;
import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker.MarkerResolution;
import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker.MarkerStatus;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Flying Raijin Technique (Hiraishin no Jutsu)
 * Space-time ninjutsu that instantaneously teleports the caster to their active Hiraishin Kunai marker.
 */
public class HiraishinAbility extends Ability implements Ability.Cooldown {

    private static final int GOLD_FLASH_ARGB = 0xFFFFD700;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public boolean requiresScroll() {
        return false;
    }

    @Override
    public float getDefaultChakraCost() {
        return 30.0f;
    }

    @Override
    public float getDefaultStaminaCost() {
        return 5.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 80;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (!super.canActivate(player, ninjaData)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return validateActiveMarkerTarget(serverPlayer, true) != null;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (player instanceof ServerPlayer serverPlayer && validateActiveMarkerTarget(serverPlayer, true) == null) {
            return false;
        }
        return super.handleCost(player, ninjaData, chargeAmount);
    }

    @Nullable
    private Vec3 validateActiveMarkerTarget(ServerPlayer serverPlayer, boolean notifyOnFail) {
        ServerLevel serverLevel = serverPlayer.level();
        MarkerResolution resolution = HiraishinMarkerTracker.resolveMarkerStatus(serverPlayer, serverLevel.getGameTime());
        if (!resolution.isUsable()) {
            if (notifyOnFail) {
                String key = (resolution.status() == MarkerStatus.OUT_OF_RANGE || resolution.status() == MarkerStatus.WRONG_DIMENSION)
                        ? "jutsu.hiraishin.out_of_range"
                        : "jutsu.hiraishin.no_marker";
                serverPlayer.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
            }
            return null;
        }
        Vec3 safePos = HiraishinMarkerTracker.findSafeTeleportPos(serverLevel, resolution.kunai().position());
        if (safePos == null) {
            if (notifyOnFail) {
                serverPlayer.sendOverlayMessage(Component.translatable("jutsu.hiraishin.blocked").withStyle(ChatFormatting.RED));
            }
            return null;
        }
        return safePos;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel serverLevel = serverPlayer.level();
        Vec3 safePos = validateActiveMarkerTarget(serverPlayer, true);
        if (safePos == null) {
            // Refund resource cost if marker expired or moved out of range during activation delay
            if (ninjaData != null) {
                float chakraRefund = getFinalChakraCost(player, ninjaData);
                float staminaRefund = getStaminaCost();
                if (chakraRefund > 0f) {
                    ninjaData.addChakra(chakraRefund);
                }
                if (staminaRefund > 0f) {
                    ninjaData.addStamina(staminaRefund);
                }
            }
            return;
        }

        Vec3 originPos = serverPlayer.position();
        spawnHiraishinBurst(serverLevel, originPos);
        serverLevel.playSound(null, originPos.x, originPos.y, originPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.35F);

        serverPlayer.setDeltaMovement(Vec3.ZERO);
        serverPlayer.teleportTo(serverLevel, safePos.x, safePos.y, safePos.z,
                Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot(), false);
        serverPlayer.resetFallDistance();

        spawnHiraishinBurst(serverLevel, safePos);
        serverLevel.playSound(null, safePos.x, safePos.y, safePos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.5F);
        serverLevel.playSound(null, safePos.x, safePos.y, safePos.z,
                NarutoSounds.LEAP.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private static void spawnHiraishinBurst(ServerLevel serverLevel, Vec3 pos) {
        ColorParticleOption goldFlash = ColorParticleOption.create(ParticleTypes.FLASH, GOLD_FLASH_ARGB);
        serverLevel.sendParticles(
                goldFlash,
                pos.x, pos.y + 1.0D, pos.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
        );
        serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1.0D, pos.z,
                26, 0.4D, 0.6D, 0.4D, 0.18D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
        );
        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y + 1.0D, pos.z,
                14, 0.3D, 0.5D, 0.3D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
        );
    }
}
