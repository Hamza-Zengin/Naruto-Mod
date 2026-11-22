package com.sekwah.narutomod.client.keybinds;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import com.sekwah.narutomod.network.c2s.ServerJutsuCastingPacket;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class NarutoKeyHandler {

    private static final Map<String, KeyBindingTickHeld> keys = new HashMap<>();

    private static final KeyMapping.Category NARUTO_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "keys"));

    private static final List<KeyBindingTickHeld> JUTSU_KEYS = new ArrayList<>();

    private static long currentJutsuCombo = 0;
    private static Ability currentJutsuComboAbility = null;
    private static final long MAX_JUTSU_VALUE = 9999999999L;
    private static int ticksSinceLastKey = 0;
    private static boolean isCurrentlyChargingAbility = false;

    private static KeyBindingTickHeld LEAP_KEY;

    private static KeyBindingTickHeld JUTSU_C_KEY;
    private static KeyBindingTickHeld JUTSU_V_KEY;
    private static KeyBindingTickHeld JUTSU_B_KEY;
    private static KeyBindingTickHeld NINJA_MENU_KEY;

    public static void registerKeyBinds() {
        LEAP_KEY = registerKeyBind("naruto.keys.leap", KeyEvent.VK_X);
        JUTSU_C_KEY = registerKeyBind("naruto.keys.key1", KeyEvent.VK_C);
        JUTSU_V_KEY = registerKeyBind("naruto.keys.key2", KeyEvent.VK_V);
        JUTSU_B_KEY = registerKeyBind("naruto.keys.key3", KeyEvent.VK_B);
        NINJA_MENU_KEY = registerKeyBind("naruto.keys.ninja_menu", KeyEvent.VK_J);

        JUTSU_KEYS.add(LEAP_KEY);
        JUTSU_KEYS.add(JUTSU_C_KEY);
        JUTSU_KEYS.add(JUTSU_V_KEY);
        JUTSU_KEYS.add(JUTSU_B_KEY);

        LEAP_KEY.registerClickConsumer(() -> {
            Minecraft mc = Minecraft.getInstance();
            if(mc.player != null) {
                NarutoAbilities.triggerAbility(NarutoAbilities.LEAP.getId());
            }
        });

        JUTSU_C_KEY.registerClickConsumer(() -> handleJustuKey(1));
        JUTSU_V_KEY.registerClickConsumer(() -> handleJustuKey(2));
        JUTSU_B_KEY.registerClickConsumer(() -> handleJustuKey(3));

        NINJA_MENU_KEY.registerClickConsumer(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new com.sekwah.narutomod.client.gui.NinjaMenuScreen());
            }
        });
    }

    public static KeyBindingTickHeld registerKeyBind(String name, int keyCode) {
        if (keys.containsKey(name)) {
            return keys.get(name);
        }

        KeyBindingTickHeld key = new KeyBindingTickHeld(name, keyCode, NARUTO_KEY_CATEGORY);
        keys.put(name, key);
        return key;
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(NARUTO_KEY_CATEGORY);
        registerKeyBinds();
        for (KeyBindingTickHeld key : JUTSU_KEYS) {
            event.register(key);
        }
        if (NINJA_MENU_KEY != null) {
            event.register(NINJA_MENU_KEY);
        }
    }

    public static void handleJustuKey(int i) {
        if(isCurrentlyChargingAbility) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ServerJutsuCastingPacket(i));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).addSeal((byte) i);
        }
        ticksSinceLastKey = 0;
        if (currentJutsuCombo < MAX_JUTSU_VALUE) {
            currentJutsuCombo *= 10;
            currentJutsuCombo += i;
            currentJutsuComboAbility = NarutoAbilities.getAbilityFromCombo(currentJutsuCombo);
        } else {
            NarutoMod.LOGGER.info("Combo too long, ignoring keypress");
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if(currentJutsuCombo != 0) {
            ticksSinceLastKey++;
        }

        int lastKey = (int) (currentJutsuCombo % 10);
        boolean lastKeyHeld = false;
        boolean isPossibleAbilityCharged = currentJutsuComboAbility != null && currentJutsuComboAbility.activationType() == Ability.ActivationType.CHANNELED;
        KeyBindingTickHeld currentKey = null;

        if(lastKey > 0 && lastKey < JUTSU_KEYS.size()) {
            currentKey = JUTSU_KEYS.get(lastKey);
            if(currentKey.isDown()) {
                lastKeyHeld = true;
            }
        }

        if(isPossibleAbilityCharged) {
            checkCharging(currentKey, lastKeyHeld);
        } else {
            checkNonCharging();
        }

        for (KeyBindingTickHeld key : JUTSU_KEYS) {
            key.update();
        }
        if (NINJA_MENU_KEY != null) {
            NINJA_MENU_KEY.update();
        }
    }

    private static void checkCharging(KeyBindingTickHeld currentKey, boolean isHoldingLastKey) {
        if(isHoldingLastKey && currentKey.heldTicks >= NarutoConfig.jutsuKeybindHoldThreshold) {
            if(!isCurrentlyChargingAbility) {
                NarutoAbilities.handleCharging(currentJutsuCombo, ServerAbilityChannelPacket.ChannelStatus.START);
            }
            isCurrentlyChargingAbility = true;
        } else if(!isHoldingLastKey) {
            if (currentKey != null) {
                currentKey.consumeReleaseDuration();
            }
            if(isCurrentlyChargingAbility) {
                NarutoAbilities.handleCharging(currentJutsuCombo, ServerAbilityChannelPacket.ChannelStatus.STOP);
                resetJutsuCasting();
            } else if(ticksSinceLastKey > getSealTimeoutTicks()){
                NarutoAbilities.handleCharging(currentJutsuCombo, ServerAbilityChannelPacket.ChannelStatus.MIN_ACTIVATE);
                resetJutsuCasting();
            }
            isCurrentlyChargingAbility = false;
        }
    }

    private static int getSealTimeoutTicks() {
        int baseTicks = Math.max(4, NarutoConfig.sealTimeoutMs / 50);
        if (currentJutsuComboAbility == null) {
            return baseTicks;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return baseTicks;
        }
        Identifier abilityId = currentJutsuComboAbility.getRegistryName();
        if (abilityId == null) {
            return baseTicks;
        }
        var ninjaData = mc.player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
        int mastery = ninjaData != null ? ninjaData.getMastery(abilityId) : 0;
        return Ability.getScaledSealTimeoutTicks(baseTicks, mastery);
    }

    private static void resetJutsuCasting() {
        ticksSinceLastKey = 0;
        currentJutsuCombo = 0;
        currentJutsuComboAbility = null;
        isCurrentlyChargingAbility = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).clearSeals();
        }
    }

    private static void checkNonCharging() {
        if(ticksSinceLastKey > getSealTimeoutTicks()) {
            Minecraft mc = Minecraft.getInstance();
            if(mc.player != null) {
                NarutoMod.LOGGER.debug("Casting jutsu {}", currentJutsuCombo);
                if(!NarutoAbilities.triggerAbility(currentJutsuCombo)) {
                    NarutoMod.LOGGER.debug("No jutsu found for combo {}", currentJutsuCombo);
                }
            }
            resetJutsuCasting();
        }
    }

    public static long getCurrentJutsuCombo() {
        return currentJutsuCombo;
    }

    public static int getTicksSinceLastKey() {
        return ticksSinceLastKey;
    }
}
