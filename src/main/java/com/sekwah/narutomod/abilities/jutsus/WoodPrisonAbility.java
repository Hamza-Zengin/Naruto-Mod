package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mokuton: Jukai Kōtan (Wood Style: Deep Forest Emergence)
 * Senju Clan Kekkei Genkai: Erects swirling organic root pillars from the earth,
 * trapping enemies in place, dealing crushing nature damage, and siphoning life force back to the caster.
 */
public class WoodPrisonAbility extends Ability implements Ability.Cooldown {

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
        return 2321; // B - C - B
    }

    @Override
    public int getCooldown() {
        return 14 * 20;
    }

    @Override
    public float getDefaultChakraCost() {
        return 28.0f;
    }

    @Override
    public float getDefaultDamage() {
        return 3.5f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("senju")) {
            player.sendOverlayMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Senju").withStyle(ChatFormatting.GREEN)));
            return false;
        }
        return tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), 0, 40);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        int prisonLife = (int) (75 * powerMult);

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(14.0));
        BlockHitResult hit = serverLevel.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : player.position().add(player.getLookAngle().scale(7.0));

        serverLevel.playSound(null, BlockPos.containing(center), com.sekwah.narutomod.sounds.NarutoSounds.WOOD_PRISON_ERUPT.get(), SoundSource.PLAYERS, 1.6f, 0.9f);
        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.CHERRY_WOOD_PLACE, SoundSource.PLAYERS, 1.8f, 0.7f);

        // Erupt 3D Intertwined Wood Prison Root Dome & Pillars (WoodPrisonEntity handles periodic constriction damage & healing)
        WoodPrisonEntity woodPrison = WoodPrisonEntity.create(
                serverLevel, center.x, center.y, center.z,
                prisonLife, player.getYRot(), player.getUUID());
        serverLevel.addFreshEntity(woodPrison);
    }
}
