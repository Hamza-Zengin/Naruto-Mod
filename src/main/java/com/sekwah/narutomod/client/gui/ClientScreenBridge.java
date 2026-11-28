package com.sekwah.narutomod.client.gui;

import net.minecraft.client.Minecraft;

/**
 * Client-only bridge for opening screens from common block/item interaction hooks
 * without referencing client classes during dedicated-server class loading.
 */
public final class ClientScreenBridge {

    private ClientScreenBridge() {
    }

    public static void openMissionsScreen() {
        Minecraft.getInstance().gui.setScreen(new NinjaMenuScreen(3));
    }
}
