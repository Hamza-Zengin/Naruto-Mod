package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Samehada (Shark Skin Greatsword)
 * Sentient Seven Ninja Swordsmen blade that shreds and devours enemy chakra on every strike,
 * restoring the wielder's Chakra (+8) and Health (+2).
 */
public class SamehadaItem extends SwordItem {

    public SamehadaItem(Properties properties) {
        super(Tiers.NETHERITE, 7, -2.8F, properties.rarity(Rarity.EPIC));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                ninjaData.addChakra(8.0f);
            });
            player.heal(2.0f);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1.0, target.getZ(),
                        10, 0.3, 0.4, 0.3, 0.08);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 0.9f, 0.8f);
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getChakra() >= 12.0f) {
                    ninjaData.useChakra(12.0f, 25);
                    player.getCooldowns().addCooldown(this, 200);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
                    level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 0.7f);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.narutomod.samehada.desc").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.narutomod.samehada.ability").withStyle(ChatFormatting.GRAY));
    }
}
