package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Kaguya Calcified Bone Sword (Tsubaki no Mai Blade)
 * Forged from hyper-dense osteoblast calcium. Deals bonus piercing damage when wielded by a Kaguya clan member.
 */
public class BoneSwordItem extends SwordItem {

    public BoneSwordItem(Properties properties) {
        super(Tiers.DIAMOND, 4, -2.2F, properties.rarity(Rarity.RARE));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase("kaguya")) {
                    target.invulnerableTime = 0;
                    target.hurt(player.damageSources().playerAttack(player), 2.5f);
                }
            });
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.narutomod.bone_sword.desc").withStyle(ChatFormatting.WHITE));
    }
}
