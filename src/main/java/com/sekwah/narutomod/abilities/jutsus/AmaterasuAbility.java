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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Amaterasu (Enton / Blaze Release)
 * Uchiha S-rank forbidden technique: Black flames that burn the target directly.
 */
public class AmaterasuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 113; // C - C - B
    }

    @Override
    public int getCooldown() {
        return 140; // 7 seconds cooldown
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Uchiha").withStyle(ChatFormatting.RED)), true);
            return false;
        }

        if (ninjaData.getChakra() < 60) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getStamina() < 40) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(60, 40);
        ninjaData.useStamina(40, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        HitResult hit = player.pick(25.0D, 0.0F, false);
        Vec3 targetPos = hit.getLocation();

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0f, 0.8f);

        // Spawn soul flame & smoke particles along the ray
        Vec3 eyePos = player.getEyePosition();
        Vec3 dir = targetPos.subtract(eyePos).normalize();
        double dist = eyePos.distanceTo(targetPos);
        for (double d = 0; d < dist; d += 0.8) {
            Vec3 point = eyePos.add(dir.scale(d));
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.01);
            serverLevel.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Catch target blocks on fire
        BlockPos hitBlockPos = BlockPos.containing(targetPos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos firePos = hitBlockPos.offset(dx, 0, dz);
                if (serverLevel.isEmptyBlock(firePos.above()) && serverLevel.getBlockState(firePos).isSolidRender(serverLevel, firePos)) {
                    serverLevel.setBlockAndUpdate(firePos.above(), Blocks.SOUL_FIRE.defaultBlockState());
                }
            }
        }

        // Damage and wither targets in area
        AABB hitBox = new AABB(targetPos.x - 3.0, targetPos.y - 2.0, targetPos.z - 3.0,
                targetPos.x + 3.0, targetPos.y + 3.0, targetPos.z + 3.0);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.setRemainingFireTicks(200); // 10 seconds of fire
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1)); // Wither II representing black flame eating away
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            target.hurt(serverLevel.damageSources().playerAttack(player), 14.0f);

            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    25, 0.4, 0.6, 0.4, 0.05);
        }
    }
}
