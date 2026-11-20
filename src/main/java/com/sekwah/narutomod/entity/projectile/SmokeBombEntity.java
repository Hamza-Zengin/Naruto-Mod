package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Smoke Bomb Entity (Kemuridama)
 * Explodes into a dense blinding smoke cloud on impact.
 */
public class SmokeBombEntity extends ThrowableItemProjectile {

    public SmokeBombEntity(EntityType<? extends SmokeBombEntity> type, Level level) {
        super(type, level);
    }

    public SmokeBombEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.SMOKE_BOMB.get(), shooter, level, new ItemStack(NarutoItems.SMOKE_BOMB.get()));
    }

    public SmokeBombEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(NarutoEntities.SMOKE_BOMB.get(), shooter, level, stack);
    }

    public SmokeBombEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.SMOKE_BOMB.get(), x, y, z, level, new ItemStack(NarutoItems.SMOKE_BOMB.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return NarutoItems.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            // Sound
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0f, 0.6f);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.5f);

            // Large billowing smoke cloud (batched into 2 packets instead of 80)
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.25, z,
                    40, 1.1, 0.8, 1.1, 0.01,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.25, z,
                    80, 1.1, 0.8, 1.1, 0.02,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            // Blinding effect on entities in 5 block radius
            AABB area = new AABB(x - 5.0, y - 2.0, z - 5.0, x + 5.0, y + 4.0, z + 5.0);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            }

            this.discard();
        }
    }
}
