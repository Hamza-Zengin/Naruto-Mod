package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

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
        super(NarutoEntities.SMOKE_BOMB.get(), shooter, level);
    }

    public SmokeBombEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.SMOKE_BOMB.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return NarutoItems.SMOKE_BOMB.get();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.5f);

            // Large billowing smoke cloud
            for (int i = 0; i < 40; i++) {
                double ox = (random.nextDouble() - 0.5) * 3.5;
                double oy = random.nextDouble() * 2.5;
                double oz = (random.nextDouble() - 0.5) * 3.5;
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + ox, y + oy, z + oz, 1, 0.02, 0.05, 0.02, 0.01);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x + ox, y + oy, z + oz, 2, 0.05, 0.05, 0.05, 0.02);
            }

            // Blinding effect on entities in 5 block radius
            AABB area = new AABB(x - 5.0, y - 2.0, z - 5.0, x + 5.0, y + 4.0, z + 5.0);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }

            this.discard();
        }
    }
}
