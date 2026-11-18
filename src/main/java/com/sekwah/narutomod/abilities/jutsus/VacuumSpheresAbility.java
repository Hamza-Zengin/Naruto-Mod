package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fūton: Vacuum Spheres (Shinkūgyoku - 風遁·真空玉)
 * Rapidly fires 3 staggered piercing wind spheres forward.
 */
public class VacuumSpheresAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.WIND;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 221;
    }

    @Override
    public float getDefaultChakraCost() {
        return 26.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 110;
    }

    @Override
    public float getDefaultDamage() {
        return 6.5f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float damagePerSphere = this.getDamage(ninjaData);

        fireSphere(player, damagePerSphere, powerMult);
        if (ninjaData != null) {
            ninjaData.scheduleDelayedTickEvent(p -> fireSphere(p, damagePerSphere, powerMult), 4);
            ninjaData.scheduleDelayedTickEvent(p -> fireSphere(p, damagePerSphere, powerMult), 8);
        }
    }

    private static void fireSphere(Player player, float damage, float powerMult) {
        if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.getEyePosition().add(look.scale(1.1D));

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.1F, 1.35F);

        VacuumSphereEntity sphere = new VacuumSphereEntity(serverLevel, player);
        sphere.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        sphere.setYRot(player.getYRot());
        sphere.setXRot(player.getXRot());
        sphere.setDamage(damage);
        sphere.setMaxLife(32);
        sphere.setDeltaMovement(look.scale(1.65D * Math.min(1.25D, powerMult)));
        serverLevel.addFreshEntity(sphere);

        serverLevel.sendParticles(ParticleTypes.CLOUD, spawnPos.x, spawnPos.y, spawnPos.z,
                6, 0.2D, 0.2D, 0.2D, 0.06D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
    }
}
