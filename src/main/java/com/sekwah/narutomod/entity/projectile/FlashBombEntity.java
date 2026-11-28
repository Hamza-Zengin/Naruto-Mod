package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity;
import com.sekwah.narutomod.entity.mob.VillageNinjaEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Flash Bomb Entity (Hikaridama)
 * Detonates on impact in a blinding flash that disorients hostile targets within 6 blocks.
 */
public class FlashBombEntity extends ThrowableItemProjectile {

    private static final double FLASH_RADIUS = 6.0D;

    public FlashBombEntity(EntityType<? extends FlashBombEntity> type, Level level) {
        super(type, level);
    }

    public FlashBombEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.FLASH_BOMB.get(), shooter, level, new ItemStack(NarutoItems.FLASH_BOMB.get()));
    }

    public FlashBombEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(NarutoEntities.FLASH_BOMB.get(), shooter, level, stack);
    }

    public FlashBombEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.FLASH_BOMB.get(), x, y, z, level, new ItemStack(NarutoItems.FLASH_BOMB.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return NarutoItems.FLASH_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            // Bright acoustic & chime burst
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.8F, 1.35F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.6F);

            // Radiant flash + spark burst particles
            serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
                    x, y + 0.5D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    x, y + 0.4D, z, 28, 0.9D, 0.7D, 0.9D, 0.16D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    x, y + 0.3D, z, 2, 0.25D, 0.25D, 0.25D, 0.0D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            // Disorient targets in 6.0-block radius (excluding thrower, allies, and protected village NPCs)
            Entity owner = this.getOwner();
            double maxDistSq = FLASH_RADIUS * FLASH_RADIUS;
            AABB area = new AABB(
                    x - FLASH_RADIUS, y - FLASH_RADIUS, z - FLASH_RADIUS,
                    x + FLASH_RADIUS, y + FLASH_RADIUS, z + FLASH_RADIUS);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    target -> target.isAlive()
                            && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(x, y, z) <= maxDistSq
                            && !isProtectedFromFlash(owner, target));

            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
            }

            this.discard();
        }
    }

    private static boolean isProtectedFromFlash(@Nullable Entity owner, LivingEntity target) {
        if (owner != null && (target == owner || ShadowCloneEntity.areAllies(owner, target))) {
            return true;
        }
        return target instanceof VillageNinjaEntity
                || target instanceof ShinobiVillagerEntity
                || target instanceof AbstractVillager;
    }
}
