package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Ichiraku Miso Pork Ramen
 * The legendary meal of Konoha! Restores 4 Hearts, +60 Chakra, +60 Stamina, and grants Regeneration.
 */
public class IchirakuRamenItem extends Item {

    public static final FoodProperties RAMEN_FOOD = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(0.9F)
            .alwaysEdible()
            .build();

    public IchirakuRamenItem(Properties properties) {
        super(properties.food(RAMEN_FOOD).stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) {
            player.heal(8.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            ninjaData.addChakra(120.0f);
            ninjaData.addStamina(120.0f);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.0f);

            if (!player.getAbilities().instabuild) {
                if (result.isEmpty()) {
                    return new ItemStack(Items.BOWL);
                } else {
                    player.getInventory().add(new ItemStack(Items.BOWL));
                }
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.ichiraku_ramen.desc").withStyle(ChatFormatting.GOLD));
    }
}
