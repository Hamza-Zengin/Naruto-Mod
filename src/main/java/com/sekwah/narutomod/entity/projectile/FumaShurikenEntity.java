package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Giant 4-Bladed Fūma Shuriken Projectile Entity
 * High damage, pierces through up to 2 targets with heavy metallic spin.
 */
public class FumaShurikenEntity extends AbstractArrow {

    private int prevRotateTicks = 0;
    private int rotateTicks = 0;

    public FumaShurikenEntity(EntityType<? extends FumaShurikenEntity> type, Level level) {
        super(type, level);
    }

    public FumaShurikenEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.FUMA_SHURIKEN.get(), shooter, level, new ItemStack(NarutoItems.FUMA_SHURIKEN.get()), null);
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
