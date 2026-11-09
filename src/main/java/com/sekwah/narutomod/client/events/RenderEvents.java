package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderEvents {

    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre event) {
        // Reserved for future model rendering
    }

    @SubscribeEvent
    public static void renderNameplateEvent(RenderNameTagEvent event) {
        if (event.getResult() != Event.Result.DENY) {
            Entity entity = event.getEntity();
            if (entity instanceof Player player) {
                ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
                Item item = itemStack.getItem();
                if (item instanceof IShouldHideNameplate hideNameplate && (hideNameplate.shouldHideNameplate(entity))) {
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        player.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.getActiveHandJutsuTicks() > 0 && !ninjaData.getActiveHandJutsu().isEmpty()) {
                com.sekwah.narutomod.network.PacketHandler.sendToServer(new com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket(-1));
            }
        });
    }

    @SubscribeEvent
    public static void onLeftClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        player.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.getActiveHandJutsuTicks() > 0 && !ninjaData.getActiveHandJutsu().isEmpty()) {
                if (event.getLevel().isClientSide()) {
                    com.sekwah.narutomod.network.PacketHandler.sendToServer(new com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket(-1));
                }
                event.setCanceled(true);
            }
        });
    }

}
