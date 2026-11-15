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

public class SenbonEntity extends AbstractArrow {

    public SenbonEntity(EntityType<? extends SenbonEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    public SenbonEntity(Level worldIn, LivingEntity shooter) {
        super(NarutoEntities.SENBON.get(), shooter, worldIn, new ItemStack(NarutoItems.SENBON.get()), null);
    }

    public SenbonEntity(EntityType<? extends SenbonEntity> kunaiEntityEntityType, LivingEntity shooter, Level worldIn) {
        super(kunaiEntityEntityType, shooter, worldIn, new ItemStack(NarutoItems.SENBON.get()), null);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return NarutoSounds.NEEDLE_HIT.get();
    }

    @Override
    public void setSoundEvent(SoundEvent pSound) {
        super.setSoundEvent(this.getDefaultHitGroundSoundEvent());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(NarutoItems.SENBON.get());
    }
}
