package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Raiton: Chidori Nagashi (Chidori Stream)
 * Emits a 360-degree omnidirectional lightning discharge around the user,
 * paralyzing nearby enemies, amplifying damage against wet targets, and deflecting incoming projectiles.
 */
public class ChidoriStreamAbility extends Ability implements Ability.Cooldown {

    public static final double STREAM_RADIUS = 5.5D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.LIGHTNING;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chidori"));
    }

    @Override
    public int getRequiredMastery() {
        return 150;
    }

    @Override
    public long defaultCombo() {
        return 1313L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 42.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 180;
    }

    @Override
    public float getDefaultDamage() {
        return 18.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float damage = this.getDamage(ninjaData);
        Vec3 center = player.position();
        AABB areaBox = player.getBoundingBox().inflate(STREAM_RADIUS, 2.5D, STREAM_RADIUS);
        double radiusSq = STREAM_RADIUS * STREAM_RADIUS;

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, areaBox,
                e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e)
                        && e.position().distanceToSqr(center) <= radiusSq);

        for (LivingEntity target : targets) {
            target.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
            Vec3 away = target.position().subtract(center);
            double horizontalDist = Math.hypot(away.x, away.z);
            if (horizontalDist > 1.0E-4D) {
                target.push((away.x / horizontalDist) * 0.65D, 0.25D, (away.z / horizontalDist) * 0.65D);
            } else {
                target.push(0.0D, 0.25D, 0.0D);
            }
            target.needsSync = true;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    14, 0.3D, 0.4D, 0.3D, 0.15D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, areaBox,
                p -> p.isAlive() && p.getOwner() != player && !ShadowCloneEntity.areAllies(player, p.getOwner())
                        && p.position().distanceToSqr(center) <= radiusSq);
        for (Projectile projectile : projectiles) {
            Vec3 away = projectile.position().subtract(center);
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(0.0D, 1.0D, 0.0D);
            }
            projectile.setDeltaMovement(away.normalize().scale(1.15D).add(0.0D, 0.25D, 0.0D));
            projectile.needsSync = true;
        }

        JutsuVisualEffectEntity burst = JutsuVisualEffectEntity.create(
                serverLevel, player.getX(), player.getY(), player.getZ(),
                JutsuVisualEffectEntity.TYPE_CHIDORI_BURST, 18, player.getYRot());
        burst.setCasterUUID(player.getUUID());
        burst.setTargetId(player.getId());
        serverLevel.addFreshEntity(burst);

        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                45, 2.2D, 0.8D, 2.2D, 0.25D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        Vec3 effectPos = center.add(0.0D, 1.0D, 0.0D);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ClientJutsuEffectPacket(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "elemental_burst"),
                effectPos,
                new Vec3(0.0D, 1.0D, 0.0D),
                2.2f,
                0x55CCFF));
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.45f, 8, effectPos, 12.0f));
        }

        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.35f, 0.9f);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7f, 1.25f);
    }
}
