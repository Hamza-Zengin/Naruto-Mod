package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Katon: Hōsenka no Jutsu (Phoenix Flower / Phoenix Sage Fire)
 * Launches 5 fanned, rapid-fire mini fireballs that scatter across a wide arc and trigger Wind+Fire synergy.
 */
public class PhoenixFlowerAbility extends Ability implements Ability.Cooldown {

    private static final float[] YAW_OFFSETS = new float[]{-12.0F, -6.0F, 0.0F, 6.0F, 12.0F};

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.FIRE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 131L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 26.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 100;
    }

    @Override
    public float getDefaultDamage() {
        return 7.5f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        final float perBallDamage = this.getDamage(ninjaData);
        for (int i = 0; i < YAW_OFFSETS.length; i++) {
            final float yawDeg = YAW_OFFSETS[i];
            int delay = i * 2;
            if (delay == 0) {
                spawnMiniFireball(player, yawDeg, perBallDamage);
            } else {
                ninjaData.scheduleDelayedTickEvent(delayedPlayer -> spawnMiniFireball(delayedPlayer, yawDeg, perBallDamage), delay);
            }
        }
    }

    private static void spawnMiniFireball(Player player, float yawOffsetDeg, float perBallDamage) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) {
            return;
        }
        Vec3 look = player.getLookAngle()
                .yRot((float) Math.toRadians(-yawOffsetDeg))
                .normalize()
                .scale(1.18D);
        FireballJutsuEntity miniFireball = new FireballJutsuEntity(player, look.x, look.y, look.z);
        miniFireball.setPhoenixMini(perBallDamage);
        Vec3 spawnPos = player.getEyePosition().add(look.scale(0.65D)).subtract(0.0D, 0.12D, 0.0D);
        miniFireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        player.level().addFreshEntity(miniFireball);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                NarutoSounds.FIREBALL_CAST.get(), SoundSource.PLAYERS, 0.65f, 1.35f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.45f, 1.3f);
    }
}
