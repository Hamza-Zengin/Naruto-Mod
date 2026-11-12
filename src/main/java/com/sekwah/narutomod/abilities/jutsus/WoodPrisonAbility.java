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
        return 313; // B - C - B
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
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Senju").withStyle(ChatFormatting.GREEN)), true);
            return false;
        }
        if (checkCooldown(player, ninjaData, this.getTranslationKey())) {
            return false;
        }
        if (!tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), 0, 40)) {
            return false;
        }
        registerCooldown(ninjaData, this.getTranslationKey());
        return true;
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

        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.8f, 0.6f);
        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.CHERRY_WOOD_PLACE, SoundSource.PLAYERS, 1.8f, 0.7f);

        // Erupt 3D Intertwined Wood Prison Root Dome & Pillars
        WoodPrisonEntity woodPrison = WoodPrisonEntity.create(
                serverLevel, center.x, center.y, center.z,
                prisonLife, player.getYRot(), player.getUUID());
        serverLevel.addFreshEntity(woodPrison);

        BlockParticleOption woodParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_WOOD.defaultBlockState());
        BlockParticleOption leafParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState());

        float waveDamage = this.getDamage(ninjaData);
        // Spawn 5 waves of root constriction over 3.5 seconds
        for (int wave = 0; wave < 5; wave++) {
            final int w = wave;
            ninjaData.scheduleDelayedTickEvent(p -> {
                if (!(p.level() instanceof ServerLevel sLevel)) return;

                // Spiraling root pillars & emerald spore aura
                for (int i = 0; i < 16; i++) {
                    double angle = (i * Math.PI / 8.0) + (w * 0.4);
                    double r = 1.5 + (i % 3) * 1.3;
                    double px = center.x + Math.cos(angle) * r;
                    double pz = center.z + Math.sin(angle) * r;
                    for (double h = 0; h <= 2.4; h += 0.6) {
                        sLevel.sendParticles(woodParticle, px, center.y + h, pz, 2, 0.15, 0.2, 0.15, 0.05);
                    }
                    sLevel.sendParticles(leafParticle, px, center.y + 1.5, pz, 1, 0.2, 0.2, 0.2, 0.02);
                }

                AABB trapBox = new AABB(center.x - 5.0, center.y - 2.0, center.z - 5.0,
                        center.x + 5.0, center.y + 3.5, center.z + 5.0);
                List<LivingEntity> targets = sLevel.getEntitiesOfClass(LivingEntity.class, trapBox,
                        e -> e.isAlive() && e != p);

                for (LivingEntity target : targets) {
                    // Entangle and pull toward root nexus
                    Vec3 pull = center.subtract(target.position()).scale(0.25);
                    target.setDeltaMovement(pull.x, -0.25, pull.z);
                    target.hurtMarked = true;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 5));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                    target.hurt(p.damageSources().playerAttack(p), waveDamage);

                    // Senju life-force siphon
                    p.heal(0.6f * powerMult);
                }
                sLevel.playSound(null, BlockPos.containing(center), SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.4f, 0.7f);
            }, wave * 14);
        }
    }
}
