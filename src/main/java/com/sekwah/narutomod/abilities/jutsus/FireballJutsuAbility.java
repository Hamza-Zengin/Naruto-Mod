package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FireballJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.FIRE;
    }

    @Override
    public long defaultCombo() {
        return 121;
    }

    /**
     * Canon hand seals: Snake (6) - Ram (8) - Monkey (9) - Boar (12) - Horse (7) - Tiger (3)
     */
    public static final byte[] FIREBALL_CANON_SEALS = new byte[]{6, 8, 9, 12, 7, 3};

    @Override
    public byte[] getExpectedSeals() {
        return FIREBALL_CANON_SEALS;
    }

    @Override
    public float getDefaultChakraCost() {
        return 30.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 5 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 20.0f;
    }

    public static int getScaledCastDelayTicks(int mastery) {
        return Math.max(2, (int) Math.round(10.0D - 8.0D * Ability.getMasteryProgress(mastery)));
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int delayTicks = getScaledCastDelayTicks(mastery);
        double speedMult = 1.0D + 0.25D * Ability.getMasteryProgress(mastery);
        int lifeSpan = 70 + (int) Math.round(25.0D * Ability.getMasteryProgress(mastery));

        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
            Vec3 shootSpeed = delayedPlayer.getLookAngle().scale(speedMult);
            FireballJutsuEntity fireball = new FireballJutsuEntity(delayedPlayer, shootSpeed.x, shootSpeed.y, shootSpeed.z);
            fireball.lifeSpan = lifeSpan;
            delayedPlayer.level().addFreshEntity(fireball);
            delayedPlayer.level().playSound(null, delayedPlayer.getX(), delayedPlayer.getY(), delayedPlayer.getZ(), com.sekwah.narutomod.sounds.NarutoSounds.FIREBALL_CAST.get(), SoundSource.PLAYERS, 1.4f, 1.0f);
            delayedPlayer.level().playSound(null, delayedPlayer.getX(), delayedPlayer.getY(), delayedPlayer.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.8f, 0.9f);
        }, delayTicks);
    }
}
