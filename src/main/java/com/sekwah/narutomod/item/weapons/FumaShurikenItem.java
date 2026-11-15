package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Giant Folding Fūma Shuriken (Fūma Shuriken)
 * Heavy 4-bladed aerodynamic wind demon shuriken that slices through multiple enemies.
 */
public class FumaShurikenItem extends Item {

    public FumaShurikenItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        if (!level.isClientSide()) {
            FumaShurikenEntity entity = new FumaShurikenEntity(level, player);
            entity.pickup = player.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.2F, 0.5F);
            entity.setBaseDamage(4.5D);
            level.addFreshEntity(entity);

            player.getCooldowns().addCooldown(stack, 18);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        player.getCooldowns().addCooldown(stack, 18);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.fuma_shuriken.desc").withStyle(ChatFormatting.GRAY));
    }
}
