package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Byakugan (All Seeing White Eye)
 * Hyūga Clan Dōjutsu: 360-degree vision, grants glowing detection to all entities through walls.
 */
public class ByakuganAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 333;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("hyuga")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Hyūga").withStyle(ChatFormatting.AQUA)), true);
            return false;
        }

        if (ninjaData.getChakra() < 0.25f) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(0.25f, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Ping all entities in 30 blocks radius with GLOWING effect every second
        if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            AABB field = player.getBoundingBox().inflate(30.0);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, field, e -> e != player && e.isAlive());
            for (LivingEntity entity : entities) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
    }
}
