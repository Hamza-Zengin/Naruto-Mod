package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
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
    public long defaultCombo() {
        return 121;
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

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
            Vec3 shootSpeed = player.getLookAngle();
            FireballJutsuEntity fireball = new FireballJutsuEntity(player, shootSpeed.x, shootSpeed.y, shootSpeed.z);
            player.level().addFreshEntity(fireball);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1f, 1.0f);
        }, 10);
    }
}
