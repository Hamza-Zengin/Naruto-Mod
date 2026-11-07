package com.sekwah.narutomod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NarutoInGameGUI {

    private final ChakraAndStaminaGUI chakraOverlay;

    public NarutoInGameGUI(){
        Minecraft mc = Minecraft.getInstance();
        this.chakraOverlay = new ChakraAndStaminaGUI(mc);
        MinecraftForge.EVENT_BUS.addListener(this::renderGameOverlay);
        MinecraftForge.EVENT_BUS.addListener(this::clientTickEvent);
    }

    @SubscribeEvent
    public void clientTickEvent(TickEvent.ClientTickEvent event) {
        this.chakraOverlay.tick();
    }

    @SubscribeEvent
    public void renderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if(event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            this.chakraOverlay.render(event.getGuiGraphics());
        }
    }
}
