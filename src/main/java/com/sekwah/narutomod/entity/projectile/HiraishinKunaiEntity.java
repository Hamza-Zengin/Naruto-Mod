package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Flying Raijin Kunai (Hiraishin Kunai) projectile entity.
 * Registers itself with {@link HiraishinMarkerTracker} when thrown and upon impact,
 * remains marked for 60 seconds (1200 ticks), and emits subtle golden electric sparks.
 */
public class HiraishinKunaiEntity extends KunaiEntity {

    private static final DustParticleOptions GOLD_SEAL_DUST = new DustParticleOptions(0xFFFFD700, 0.7F);

    public HiraishinKunaiEntity(EntityType<? extends HiraishinKunaiEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    public HiraishinKunaiEntity(Level worldIn, LivingEntity shooter) {
        super(NarutoEntities.HIRAISHIN_KUNAI.get(), shooter, worldIn, new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get()));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount % 10 == 0 && HiraishinMarkerTracker.isKunaiMarked(this, serverLevel.getGameTime())) {
                serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + 0.12D, this.getZ(),
                        1, 0.08D, 0.08D, 0.08D, 0.02D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
                );
                if (this.tickCount % 20 == 0) {
                    serverLevel.sendParticles(
                            GOLD_SEAL_DUST,
                            this.getX(), this.getY() + 0.15D, this.getZ(),
                            1, 0.06D, 0.06D, 0.06D, 0.0D,
                            ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
                    );
                }
            }
        }
    }

    @Override
    protected void tickDespawn() {
        if (this.level() instanceof ServerLevel serverLevel
                && HiraishinMarkerTracker.isKunaiMarked(this, serverLevel.getGameTime())) {
            return;
        }
        super.tickDespawn();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide() && this.getOwner() instanceof Player owner) {
            HiraishinMarkerTracker.registerMarker(owner, this, this.level().getGameTime());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && !this.isRemoved() && this.getOwner() instanceof Player owner) {
            HiraishinMarkerTracker.registerMarker(owner, this, this.level().getGameTime());
        }
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        super.onRemoval(reason);
        if (!this.level().isClientSide() && reason.shouldDestroy() && this.getOwner() instanceof Player owner) {
            HiraishinMarkerTracker.clearMarkerIfMatches(owner.getUUID(), this.getUUID());
        }
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return NarutoSounds.KUNAI_THUD.get();
    }

    @Override
    public void setSoundEvent(SoundEvent pSound) {
        super.setSoundEvent(this.getDefaultHitGroundSoundEvent());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
    }
}
