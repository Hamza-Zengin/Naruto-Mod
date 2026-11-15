package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Kaguya Calcified Bone Sword (Tsubaki no Mai Blade)
 * Forged from hyper-dense osteoblast calcium. Deals bonus piercing damage when wielded by a Kaguya clan member.
 */
public class BoneSwordItem extends Item {

    public BoneSwordItem(Properties properties) {
        super(properties.sword(ToolMaterial.DIAMOND, 4, -2.2F).rarity(Rarity.RARE));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel && attacker instanceof Player player) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase("kaguya")) {
                target.setInvulnerableTime(0);
                target.hurtServer(serverLevel, player.damageSources().playerAttack(player), 2.5f);
            }
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.bone_sword.desc").withStyle(ChatFormatting.WHITE));
    }
}
