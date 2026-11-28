package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.abilities.jutsus.HiraishinAbility;
import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Flying Raijin Kunai (Hiraishin Kunai)
 * Marked space-time kunai that serves as a teleportation anchor for Hiraishin no Jutsu.
 */
public class HiraishinKunaiItem extends KunaiItem {

    public HiraishinKunaiItem(Item.Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public AbstractArrow createShootingEntity(Level worldIn, Player playerIn) {
        HiraishinKunaiEntity entity = new HiraishinKunaiEntity(worldIn, playerIn);
        entity.pickup = playerIn.getAbilities().instabuild
                ? AbstractArrow.Pickup.CREATIVE_ONLY
                : AbstractArrow.Pickup.ALLOWED;
        if (!worldIn.isClientSide()) {
            int mastery = getOwnerHiraishinMastery(playerIn);
            long durationTicks = HiraishinMarkerTracker.getMarkerDurationTicks(mastery);
            double maxRange = HiraishinMarkerTracker.getMaxTeleportRange(mastery);
            HiraishinMarkerTracker.registerMarker(playerIn, entity, worldIn.getGameTime(), durationTicks);
            if (playerIn instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("item.narutomod.hiraishin_kunai")
                                .append(Component.literal(String.format(Locale.ROOT, " [%ds | %.0fm]", durationTicks / 20L, maxRange)))
                                .withStyle(ChatFormatting.GOLD)
                );
            }
        }
        return entity;
    }

    public static int getOwnerHiraishinMastery(Player player) {
        if (player == null) {
            return 0;
        }
        var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        return ninjaData != null ? ninjaData.getMastery(HiraishinAbility.HIRAISHIN_ID) : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.hiraishin_kunai.desc").withStyle(ChatFormatting.GOLD));
    }
}
