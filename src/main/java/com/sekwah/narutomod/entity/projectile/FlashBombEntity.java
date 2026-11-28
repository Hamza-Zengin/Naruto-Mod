package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity;
import com.sekwah.narutomod.entity.mob.VillageNinjaEntity;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.network.s2c.ClientFlashEffectPacket;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Flash Bomb Entity (Hikaridama)
 * Detonates on impact in a blinding flash that disorients hostile targets within 6.5 blocks.
 */
public class FlashBombEntity extends ThrowableItemProjectile {

    private static final double FLASH_RADIUS = 6.5D;
    private static final double SELF_FLASH_RADIUS = 3.0D;

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

            // Sharp flashbang pop & high-pitched ringing burst
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0F, 1.45F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.6F, 1.7F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8F, 1.85F);

            // Radiant flash + spark burst particles
            serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
                    x, y + 0.5D, z, 2, 0.2D, 0.2D, 0.2D, 0.0D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    x, y + 0.4D, z, 36, 1.1D, 0.8D, 1.1D, 0.22D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.FIREWORK,
                    x, y + 0.4D, z, 32, 0.9D, 0.7D, 0.9D, 0.28D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            Entity owner = this.getOwner();
            double maxDistSq = FLASH_RADIUS * FLASH_RADIUS;
            double selfDistSq = SELF_FLASH_RADIUS * SELF_FLASH_RADIUS;
            AABB area = new AABB(
                    x - FLASH_RADIUS, y - FLASH_RADIUS, z - FLASH_RADIUS,
                    x + FLASH_RADIUS, y + FLASH_RADIUS, z + FLASH_RADIUS);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    target -> target.isAlive()
                            && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(x, y, z) <= maxDistSq);

            for (LivingEntity target : candidates) {
                double distSq = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(x, y, z);

                // Thrower self-protection: only mild brief flash within 3.0 blocks, zero debuffs
                if (owner != null && target == owner) {
                    if (distSq <= selfDistSq && !isProtectedFromFlash(target) && target instanceof ServerPlayer serverPlayer) {
                        sendFlashPacket(serverPlayer, 0.45F, 25);
                    }
                    continue;
                }

                if (isProtectedFromFlash(target)) {
                    continue;
                }

                if (target instanceof ServerPlayer serverPlayer) {
                    double dist = Math.sqrt(distSq);
                    float falloff = (float) Mth.clamp(1.0D - (dist / FLASH_RADIUS), 0.0D, 1.0D);
                    float intensity = Mth.clamp(0.65F + 0.35F * falloff, 0.0F, 1.0F);
                    sendFlashPacket(serverPlayer, intensity, 70);
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                } else if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);
                    mob.getNavigation().stop();
                    mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                }
            }

            this.discard();
        }
    }

    private static void sendFlashPacket(ServerPlayer serverPlayer, float intensity, int durationTicks) {
        ClientFlashEffectPacket packet = new ClientFlashEffectPacket(intensity, durationTicks);
        if (serverPlayer.connection != null && serverPlayer.connection.hasChannel(packet)) {
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    private boolean isProtectedFromFlash(LivingEntity target) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        Entity owner = this.getOwner();
        if (owner != null && target != owner && ShadowCloneEntity.areAllies(owner, target)) {
            return true;
        }
        return target instanceof VillageNinjaEntity
                || target instanceof ShinobiVillagerEntity
                || target instanceof AbstractVillager;
    }
}
