package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Suiton: Water Dragon (Suiryūdan - 水遁·水龍弾)
 * High-level Water Release jutsu summoning a surging homing water dragon torrent.
 */
public class WaterDragonAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.WATER;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_bullet"));
    }

    @Override
    public int getRequiredMastery() {
        return 150;
    }

    @Override
    public long defaultCombo() {
        return 2121;
    }

    @Override
    public boolean requiresScroll() {
        return true;
    }

    @Override
    public float getDefaultChakraCost() {
        return 48.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 220;
    }

    @Override
    public float getDefaultDamage() {
        return 24.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.getEyePosition().add(look.scale(1.5D));

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0F, 0.75F);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.6F, 0.9F);

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float dragonDamage = this.getDamage(ninjaData);

        WaterBulletEntity dragon = new WaterBulletEntity(serverLevel, player);
        dragon.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        dragon.setDragon(true);
        dragon.setDamage(dragonDamage);
        dragon.setMaxLife(45);
        dragon.setDeltaMovement(look.scale(1.05D * Math.min(1.3D, powerMult)));
        dragon.alignRotationToMovement();
        serverLevel.addFreshEntity(dragon);

        serverLevel.sendParticles(ParticleTypes.SPLASH, spawnPos.x, spawnPos.y, spawnPos.z,
                35, 0.6D, 0.6D, 0.6D, 0.14D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.BUBBLE, spawnPos.x, spawnPos.y, spawnPos.z,
                20, 0.45D, 0.45D, 0.45D, 0.10D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
    }
}
