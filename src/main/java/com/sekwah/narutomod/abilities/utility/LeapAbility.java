package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LeapAbility extends Ability {
    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.ACADEMY_STUDENT;
    }

    @Override
    public boolean logInChat() {
        return false;
    }

    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(!player.onGround()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notonground", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        float staminaCost = Math.max(5.0f, 10.0f * getChakraCostMultiplier(mastery));
        if(ninjaData.getStamina() < staminaCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useStamina(staminaCost, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        float boostMult = 1.0f + (getPowerMultiplier(mastery) - 1.0f) * 0.58f; // up to +35% at Lv 5

        Vec3 lookVector = player.getLookAngle();
        float horScale = 2.41f * boostMult;
        float vertScale = (float) ((lookVector.y + 0.8F) * (1.0f + (boostMult - 1.0f) * 0.5f));
        PlayerUtil.setVelocity(player, lookVector.x * horScale, vertScale, lookVector.z * horScale, true);
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(), NarutoSounds.LEAP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
    }
}
