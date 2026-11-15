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

public class ShurikenEntity extends AbstractArrow {

    private int prevRotateTicks = 0;

    private int rotateTicks = 0;

    private int rotOffset = 0;

    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, Level worldIn) {
        super(type, worldIn);
        this.rotateTicks = worldIn.getRandom().nextInt(140);
        this.rotOffset = (worldIn.getRandom().nextInt(100) - 50);
    }

    public ShurikenEntity(Level worldIn, LivingEntity shooter) {
        super(NarutoEntities.SHURIKEN.get(), shooter, worldIn, new ItemStack(NarutoItems.SHURIKEN.get()), null);
    }

    public ShurikenEntity(EntityType<? extends ShurikenEntity> kunaiEntityEntityType, LivingEntity shooter, Level worldIn) {
        super(kunaiEntityEntityType, shooter, worldIn, new ItemStack(NarutoItems.SHURIKEN.get()), null);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevRotateTicks = this.rotateTicks;
        if (!this.isInGround()) {
            this.rotateTicks += 1;
        }
    }

    @Override
    public void setSoundEvent(SoundEvent pSound) {
        super.setSoundEvent(this.getDefaultHitGroundSoundEvent());
    }

    public int getRotateTicks() {
        return rotateTicks;
    }

    public int getPrevRotateTicks() {
        return prevRotateTicks;
    }

    public int getRotOffset() {
        return rotOffset;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return NarutoSounds.KUNAI_THUD.get();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(NarutoItems.SHURIKEN.get());
    }
}
