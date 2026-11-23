package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuTargetingHelper;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kongō Fūsa (Adamantine Sealing Chains - 金剛封鎖)
 * Uzumaki Clan Secret Fūinjutsu: Manifests radiant golden chakra chains from a sealing circle
 * that pin enemies in place, seal chakra regeneration, and constrict targets over time.
 */
public class AdamantineChainsAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.NONE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public String getRequiredClanPath() {
        return "uzumaki";
    }

    @Override
    public float getDefaultChakraCost() {
        return 42.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 220;
    }

    @Override
    public float getDefaultDamage() {
        return 16.0f;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (ninjaData == null || ninjaData.getClan() == null || !"uzumaki".equalsIgnoreCase(ninjaData.getClan().getPath())) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notclan",
                        Component.literal("Uzumaki").withStyle(ChatFormatting.RED)));
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!canActivate(player, ninjaData)) {
            return false;
        }
        return tryConsume(player, ninjaData, getFinalChakraCost(player, ninjaData), getStaminaCost(), 45);
    }

    public static double getScaledRange(int mastery) {
        double progress = Math.min(1.0D, Math.max(0, mastery) / 700.0D);
        return 22.0D + (4.0D * progress);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        Vec3 targetPos = JutsuTargetingHelper.resolveTargetOrGroundOrigin(serverLevel, player, getScaledRange(mastery), 3.2D);
        float damage = this.getDamage(ninjaData);

        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.6F, 0.85F);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.3F, 1.35F);

        JutsuVisualEffectEntity chains = JutsuVisualEffectEntity.create(
                serverLevel, targetPos.x, targetPos.y, targetPos.z,
                JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS,
                Ability.getScaledDurationTicks(60, mastery, 0.60D), player.getYRot());
        chains.setCasterUUID(player.getUUID());
        chains.setEffectDamage(damage);
        serverLevel.addFreshEntity(chains);
    }
}
