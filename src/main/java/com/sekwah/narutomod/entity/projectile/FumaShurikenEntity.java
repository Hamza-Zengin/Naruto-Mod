package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Giant 4-Bladed Fūma Shuriken Projectile Entity
 * High damage, pierces through up to 2 targets with heavy metallic spin.
 */
public class FumaShurikenEntity extends AbstractArrow {

    private int prevRotateTicks = 0;
    private int rotateTicks = 0;

    public FumaShurikenEntity(EntityType<? extends FumaShurikenEntity> type, Level level) {
        super(type, level);
        this.setPierceLevel((byte) 2);
    }

    public FumaShurikenEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.FUMA_SHURIKEN.get(), shooter, level);
        this.setPierceLevel((byte) 2);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevRotateTicks = this.rotateTicks;
        if (!this.inGround) {
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(NarutoItems.FUMA_SHURIKEN.get());
    }
}
