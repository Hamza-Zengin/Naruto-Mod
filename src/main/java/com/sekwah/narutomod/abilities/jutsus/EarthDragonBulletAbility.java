package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Doton: Earth Dragon Bullet (Doryūdan - 土遁·土龍弾)
 * Launches a high-mass earth projectile that knocks targets back and inflicts Slowness II + Weakness I.
 */
public class EarthDragonBulletAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.EARTH;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 231;
    }

    @Override
    public float getDefaultChakraCost() {
        return 28.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 110;
    }

    @Override
    public float getDefaultDamage() {
        return 14.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.4F, 0.8F);

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float damage = this.getDamage(ninjaData);

        EarthBulletEntity bullet = new EarthBulletEntity(serverLevel, player);
        Vec3 spawnPos = player.getEyePosition().add(look.scale(1.3D));
        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bullet.setYRot(player.getYRot());
        bullet.setXRot(player.getXRot());
        bullet.setDamage(damage);
        bullet.setMaxLife(45);
        bullet.setDeltaMovement(look.scale(1.15D * Math.min(1.35D, powerMult)));
        serverLevel.addFreshEntity(bullet);

        BlockParticleOption rockParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
        serverLevel.sendParticles(rockParticle, spawnPos.x, spawnPos.y, spawnPos.z, 18, 0.35D, 0.35D, 0.35D, 0.1D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
    }
}
