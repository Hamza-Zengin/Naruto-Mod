package com.sekwah.narutomod.client.keybinds;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.gui.JutsuWheelOverlay;
import com.sekwah.narutomod.client.gui.NinjaMenuScreen;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket;
import com.sekwah.narutomod.network.c2s.ServerUpdateJutsuLoadoutPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class NarutoKeyHandler {

    private static final Map<String, KeyBindingTickHeld> keys = new HashMap<>();

    private static final KeyMapping.Category NARUTO_KEY_CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "keys"));

    private static final List<KeyBindingTickHeld> ALL_KEYS = new ArrayList<>();

    private static KeyBindingTickHeld JUTSU_WHEEL_KEY;
    private static KeyBindingTickHeld USE_JUTSU_KEY;
    private static KeyBindingTickHeld CHAKRA_CHARGE_KEY;
    private static KeyBindingTickHeld WATER_WALK_KEY;
    private static KeyBindingTickHeld LEAP_KEY;
    private static KeyBindingTickHeld NINJA_MENU_KEY;

    private static boolean isChargingChakra = false;
    private static Identifier channelingSelectedId = null;

    public static void registerKeyBinds() {
        JUTSU_WHEEL_KEY = registerKeyBind("naruto.keys.jutsu_wheel", KeyEvent.VK_V);
        USE_JUTSU_KEY = registerKeyBind("naruto.keys.use_jutsu", KeyEvent.VK_R);
        CHAKRA_CHARGE_KEY = registerKeyBind("naruto.keys.chakra_charge", KeyEvent.VK_C);
        WATER_WALK_KEY = registerKeyBind("naruto.keys.water_walk", KeyEvent.VK_B);
        LEAP_KEY = registerKeyBind("naruto.keys.leap", KeyEvent.VK_X);
        NINJA_MENU_KEY = registerKeyBind("naruto.keys.ninja_menu", KeyEvent.VK_J);

        ALL_KEYS.clear();
        ALL_KEYS.add(JUTSU_WHEEL_KEY);
        ALL_KEYS.add(USE_JUTSU_KEY);
        ALL_KEYS.add(CHAKRA_CHARGE_KEY);
        ALL_KEYS.add(WATER_WALK_KEY);
        ALL_KEYS.add(LEAP_KEY);
        ALL_KEYS.add(NINJA_MENU_KEY);

        LEAP_KEY.registerClickConsumer(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null && !JutsuWheelOverlay.isOpen()) {
                NarutoAbilities.triggerAbility(NarutoAbilities.LEAP.getId());
            }
        });

        WATER_WALK_KEY.registerClickConsumer(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null && !JutsuWheelOverlay.isOpen()) {
                NarutoAbilities.triggerAbility(NarutoAbilities.WATER_WALK.getId());
            }
        });

        USE_JUTSU_KEY.registerClickConsumer(NarutoKeyHandler::handleUseJutsuClick);

        NINJA_MENU_KEY.registerClickConsumer(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                if (JutsuWheelOverlay.isOpen()) {
                    JutsuWheelOverlay.close();
                }
                mc.gui.setScreen(new NinjaMenuScreen());
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
        for (KeyBindingTickHeld key : ALL_KEYS) {
            event.register(key);
        }
    }

    private static void handleUseJutsuClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || JutsuWheelOverlay.isOpen()) {
            return;
        }
        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);

        // If a hand-infused jutsu (Rasengan / Chidori / Chakra Scalpel) is ready, R also triggers the strike
        if (ninjaData.getActiveHandJutsuTicks() > 0 && !ninjaData.getActiveHandJutsu().isEmpty()) {
            int targetId = (mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive())
                    ? living.getId()
                    : -1;
            ClientPacketDistributor.sendToServer(new ServerTriggerHandJutsuPacket(targetId));
            return;
        }

        Identifier selectedId = ninjaData.getSelectedJutsuId();
        if (selectedId == null) {
            return;
        }
        Ability ability = NarutoAbilities.getAbility(selectedId);
        if (ability == null || ability.isUtilityAbility()) {
            return;
        }
        if (ability.activationType() == Ability.ActivationType.CHANNELED) {
            if (channelingSelectedId == null) {
                NarutoAbilities.handleCharging(selectedId, ServerAbilityChannelPacket.ChannelStatus.START);
                channelingSelectedId = selectedId;
            }
        } else {
            NarutoAbilities.triggerAbility(selectedId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        handleRadialWheelTick(mc);
        handleChakraChargeTick(mc);
        handleSelectedChannelReleaseTick(mc);

        for (KeyBindingTickHeld key : ALL_KEYS) {
            key.update();
        }
    }

    private static void handleRadialWheelTick(Minecraft mc) {
        if (JUTSU_WHEEL_KEY == null) {
            return;
        }
        if (mc.player == null || !mc.player.isAlive() || mc.gui.screen() != null) {
            if (JutsuWheelOverlay.isOpen()) {
                JutsuWheelOverlay.close();
            }
            return;
        }

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        boolean keyHeld = JUTSU_WHEEL_KEY.isDown();

        if (keyHeld && !JutsuWheelOverlay.isOpen()) {
            JutsuWheelOverlay.open(ninjaData.getSelectedJutsuSlot());
            mc.mouseHandler.releaseMouse();
        } else if (!keyHeld && JutsuWheelOverlay.isOpen()) {
            int chosen = JutsuWheelOverlay.getHoveredSlot();
            if (chosen >= 0 && chosen < INinjaData.MAX_JUTSU_SLOTS) {
                ninjaData.setSelectedJutsuSlot(chosen);
                ClientPacketDistributor.sendToServer(ServerUpdateJutsuLoadoutPacket.selectSlot(chosen));
            }
            JutsuWheelOverlay.close();
            mc.mouseHandler.grabMouse();
        }
    }

    private static void handleChakraChargeTick(Minecraft mc) {
        if (CHAKRA_CHARGE_KEY == null) {
            return;
        }
        boolean canCharge = mc.player != null && mc.player.isAlive() && mc.gui.screen() == null && !JutsuWheelOverlay.isOpen();
        if (canCharge && CHAKRA_CHARGE_KEY.isDown()) {
            if (!isChargingChakra) {
                NarutoAbilities.handleCharging(NarutoAbilities.CHAKRA_CHARGE.getId(), ServerAbilityChannelPacket.ChannelStatus.START);
                isChargingChakra = true;
            }
        } else if (isChargingChakra) {
            NarutoAbilities.handleCharging(NarutoAbilities.CHAKRA_CHARGE.getId(), ServerAbilityChannelPacket.ChannelStatus.STOP);
            isChargingChakra = false;
        }
    }

    private static void handleSelectedChannelReleaseTick(Minecraft mc) {
        if (channelingSelectedId == null) {
            return;
        }
        boolean stillHolding = USE_JUTSU_KEY != null
                && USE_JUTSU_KEY.isDown()
                && mc.player != null
                && mc.player.isAlive()
                && mc.gui.screen() == null
                && !JutsuWheelOverlay.isOpen();
        if (!stillHolding) {
            NarutoAbilities.handleCharging(channelingSelectedId, ServerAbilityChannelPacket.ChannelStatus.STOP);
            channelingSelectedId = null;
        }
    }
}
