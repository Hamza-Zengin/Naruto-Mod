package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class RenderEvents {

    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre<?> event) {
        // Reserved for future model rendering
    }

    @SubscribeEvent
    public static void renderNameplateEvent(RenderNameTagEvent.CanRender event) {
        if (event.canRender() != TriState.FALSE) {
            Entity entity = event.getEntity();
            if (entity instanceof Player player) {
                ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
                Item item = itemStack.getItem();
                if (item instanceof IShouldHideNameplate hideNameplate && (hideNameplate.shouldHideNameplate(entity))) {
                    event.setCanRender(TriState.FALSE);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        var ninjaData = player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
        if (ninjaData.getActiveHandJutsuTicks() > 0 && !ninjaData.getActiveHandJutsu().isEmpty()) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket(-1));
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        var ninjaData = player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
        if (ninjaData.getActiveHandJutsuTicks() > 0 && !ninjaData.getActiveHandJutsu().isEmpty()) {
            if (event.getLevel().isClientSide()) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket(-1));
            }
            event.setCanceled(true);
        }
    }

}
