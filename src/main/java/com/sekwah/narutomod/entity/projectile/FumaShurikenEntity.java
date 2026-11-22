package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Giant 4-Bladed Fūma Shuriken Projectile Entity
 * High damage, pierces through up to 2 targets with heavy metallic spin.
 */
public class FumaShurikenEntity extends AbstractArrow {

    private int prevRotateTicks = 0;
    private int rotateTicks = 0;

    public static final double SAFE_SPAWN_FORWARD_OFFSET = 1.25D;

    public FumaShurikenEntity(EntityType<? extends FumaShurikenEntity> type, Level level) {
        super(type, level);
    }

    public FumaShurikenEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.FUMA_SHURIKEN.get(), shooter, level, new ItemStack(NarutoItems.FUMA_SHURIKEN.get()), null);
        Vec3 eyePos = new Vec3(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        Vec3 spawnPos = computeSafeSpawnPosition(
                eyePos.x,
                eyePos.y,
                eyePos.z,
                shooter.getLookAngle(),
                SAFE_SPAWN_FORWARD_OFFSET
        );
        // Only move forward if the path up to the offset position is unobstructed so we don't spawn past thin walls
        BlockHitResult hit = level.clip(new ClipContext(
                eyePos, spawnPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        if (hit.getType() == HitResult.Type.MISS) {
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        }
    }

    public static Vec3 computeSafeSpawnPosition(double originX, double originY, double originZ,
                                                Vec3 lookDir,
                                                double forwardOffset) {
        double lengthSq = lookDir.lengthSqr();
        if (lengthSq < 1.0E-8D) {
            return new Vec3(originX, originY, originZ);
        }
        Vec3 normalized = lookDir.scale(1.0D / Math.sqrt(lengthSq));
        return new Vec3(
                originX + normalized.x * forwardOffset,
                originY + normalized.y * forwardOffset,
                originZ + normalized.z * forwardOffset
        );
    }

    @Override
    public byte getPierceLevel() {
        return (byte) 2;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevRotateTicks = this.rotateTicks;
        if (!this.isInGround()) {
            this.rotateTicks += 1;
        }
    }

    public int getRotateTicks() {
        return this.rotateTicks;
    }

    public int getPrevRotateTicks() {
        return this.prevRotateTicks;
    }

    @Override
    public void setSoundEvent(SoundEvent sound) {
        super.setSoundEvent(this.getDefaultHitGroundSoundEvent());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return NarutoSounds.KUNAI_THUD.get();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(NarutoItems.FUMA_SHURIKEN.get());
    }
}
