package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.events.PlayerEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper for server tick equipment/toggle sync, jutsu channeling flow,
 * active hand jutsu countdowns, and equipped jutsu slot management.
 */
public final class NinjaChannelAndEquipmentHelper {

    private static final Identifier SHARINGAN_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sharingan");
    private static final Identifier BYAKUGAN_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "byakugan");
    private static final Identifier EIGHT_GATES_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "eight_gates");
    private static final Identifier EIGHT_GATES_DEATH_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "eight_gates_death");
    private static final Identifier WATER_WALK_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_walk");

    private NinjaChannelAndEquipmentHelper() {}

    // --- Jutsu Channeling & Server Tick Flow ---

    public static void setCurrentlyChanneledAbility(NinjaData data, Player player, @Nullable Ability ability) {
        if (ability != null) {
            if (ability.castingSound() != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            if (ability instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                player.sendSystemMessage(Component.translatable("jutsu.charge.start",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.translatable("jutsu.channel.start",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GREEN));
            }

            data.setRawCurrentlyChanneled(ability.getRegistryName());
        } else {
            Identifier currentId = data.getCurrentlyChanneledAbility();
            if (currentId != null) {
                Ability currentAbility = NarutoAbilities.getAbility(currentId);
                if (currentAbility != null) {
                    if (currentAbility instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                        player.sendSystemMessage(Component.translatable("jutsu.cast",
                                Component.translatable(currentAbility.getTranslationKey()).withStyle(ChatFormatting.YELLOW))
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage(Component.translatable("jutsu.channel.stop",
                                Component.translatable(currentAbility.getTranslationKey()).withStyle(ChatFormatting.YELLOW))
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
            data.setRawCurrentlyChanneled(null);
        }
        data.setRawTicksChanneled(0);
    }

    public static void updateServerData(
            NinjaData data,
            Player player,
            NinjaTickScheduler tickScheduler,
            ChakraStaminaLogic.RegenInfo staminaRegenInfo,
            ChakraStaminaLogic.RegenInfo chakraRegenInfo
    ) {
        if (player.isDeadOrDying()) {
            if (data.getCurrentlyChanneledAbility() != null) {
                setCurrentlyChanneledAbility(data, player, null);
            }
            if (data.getActiveHandJutsuTicks() > 0) {
                data.clearActiveHandJutsu();
            }
            data.setActiveJutsusFlag(0);
            return;
        }

        // Countdown active hand jutsu (Rasengan/Chidori 5-second window)
        int handJutsuTicks = data.getActiveHandJutsuTicks();
        if (handJutsuTicks > 0) {
            handJutsuTicks--;
            data.setRawActiveHandJutsuTicks(handJutsuTicks);
            String handJutsu = data.getActiveHandJutsu();
            if (handJutsuTicks % 20 == 0) {
                if ("chidori".equals(handJutsu)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            NarutoSounds.CHIDORI_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
                } else if ("rasengan".equals(handJutsu)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            NarutoSounds.RASENGAN_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                }
            }
            if (handJutsuTicks == 0) {
                if ("chidori".equals(handJutsu)) {
                    player.removeEffect(MobEffects.SPEED);
                }
                data.clearActiveHandJutsu();
            }
        }

        tickScheduler.tick(player);

        boolean wearingKageHat = PlayerEvents.isKageHat(
                player.getItemBySlot(EquipmentSlot.HEAD).getItem());
        if (data.isKageHatEquipped() != wearingKageHat) {
            data.setKageHatEquipped(wearingKageHat);
        }

        ChakraStaminaLogic.tickRegen(data, player, staminaRegenInfo, chakraRegenInfo);

        Identifier currentlyChanneled = data.getCurrentlyChanneledAbility();
        if (currentlyChanneled != null) {
            Ability ability = NarutoAbilities.getAbility(currentlyChanneled);
            int ticksChanneled = data.getCurrentlyChanneledTicks();
            if (ability != null && ability.activationType() == Ability.ActivationType.CHANNELED) {
                if (ability.handleCost(player, data, ticksChanneled)) {
                    if (ability instanceof Ability.Channeled channeled) {
                        channeled.handleChannelling(player, data, ticksChanneled);
                    }
                } else {
                    if (ticksChanneled > 0) {
                        ability.performServer(player, data, ticksChanneled - 1);
                    }
                    setCurrentlyChanneledAbility(data, player, null);
                }
            } else {
                NarutoMod.LOGGER.error("Somehow non channeled ability has been set to ninja data {}", currentlyChanneled);
                setCurrentlyChanneledAbility(data, player, null);
            }
            data.setRawTicksChanneled(data.getCurrentlyChanneledTicks() + 1);
        }

        // Sync active abilities bitmask with ToggleAbilityData and active Sage Mode state
        ToggleAbilityData toggleData = data.getRawToggleAbilityData();
        int flags = computeActiveJutsusFlag(toggleData);
        if (SageModeHelper.isSageModeActive(player)) {
            flags |= INinjaData.JUTSU_FLAG_SAGE_MODE;
        }
        data.setActiveJutsusFlag(flags);
    }

    public static int computeActiveJutsusFlag(@Nullable ToggleAbilityData toggleAbilityData) {
        if (toggleAbilityData == null) {
            return 0;
        }
        var activeToggleAbilities = toggleAbilityData.getAbilitiesHashSet();
        if (activeToggleAbilities == null || activeToggleAbilities.isEmpty()) {
            return 0;
        }
        int flags = 0;
        for (Identifier id : activeToggleAbilities) {
            if (SHARINGAN_ID.equals(id)) {
                flags |= INinjaData.JUTSU_FLAG_SHARINGAN;
            } else if (BYAKUGAN_ID.equals(id)) {
                flags |= INinjaData.JUTSU_FLAG_BYAKUGAN;
            } else if (EIGHT_GATES_ID.equals(id)) {
                flags |= INinjaData.JUTSU_FLAG_EIGHT_GATES;
            } else if (EIGHT_GATES_DEATH_ID.equals(id)) {
                flags |= INinjaData.JUTSU_FLAG_EIGHT_GATES | INinjaData.JUTSU_FLAG_EIGHT_GATES_DEATH;
            } else if (WATER_WALK_ID.equals(id)) {
                flags |= INinjaData.JUTSU_FLAG_WATER_WALK;
            }
        }
        return flags;
    }

    public static int setJutsuActiveFlag(int currentFlag, int flag, boolean active) {
        return active ? (currentFlag | flag) : (currentFlag & ~flag);
    }

    // --- Equipped Jutsu Slot Helpers ---

    public static String[] parseEquippedSlots(@Nullable String equippedJutsus) {
        String[] slots = new String[INinjaData.MAX_JUTSU_SLOTS];
        Arrays.fill(slots, "");
        if (equippedJutsus != null && !equippedJutsus.isEmpty()) {
            String[] parts = equippedJutsus.split(";", -1);
            for (int i = 0; i < Math.min(INinjaData.MAX_JUTSU_SLOTS, parts.length); i++) {
                slots[i] = parts[i] != null ? parts[i].trim() : "";
            }
        }
        return slots;
    }

    public static String writeEquippedSlots(String[] slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < INinjaData.MAX_JUTSU_SLOTS; i++) {
            if (i > 0) sb.append(';');
            if (slots[i] != null && !slots[i].isEmpty()) {
                sb.append(slots[i]);
            }
        }
        return sb.toString();
    }

    @Nullable
    public static Identifier getEquippedJutsu(@Nullable String equippedJutsus, int slot) {
        if (slot < 0 || slot >= INinjaData.MAX_JUTSU_SLOTS) {
            return null;
        }
        String raw = parseEquippedSlots(equippedJutsus)[slot];
        if (raw.isEmpty()) {
            return null;
        }
        return Identifier.tryParse(raw);
    }

    public static String setEquippedJutsu(@Nullable String equippedJutsus, int slot, @Nullable Identifier abilityId) {
        if (slot < 0 || slot >= INinjaData.MAX_JUTSU_SLOTS) {
            return equippedJutsus != null ? equippedJutsus : ";;;;;;;";
        }
        String[] slots = parseEquippedSlots(equippedJutsus);
        String idStr = abilityId != null ? abilityId.toString() : "";
        if (!idStr.isEmpty()) {
            for (int i = 0; i < INinjaData.MAX_JUTSU_SLOTS; i++) {
                if (i != slot && idStr.equals(slots[i])) {
                    slots[i] = "";
                }
            }
        }
        slots[slot] = idStr;
        return writeEquippedSlots(slots);
    }

    public static List<Identifier> getEquippedJutsusList(@Nullable String equippedJutsus) {
        String[] slots = parseEquippedSlots(equippedJutsus);
        List<Identifier> list = new ArrayList<>(INinjaData.MAX_JUTSU_SLOTS);
        for (int i = 0; i < INinjaData.MAX_JUTSU_SLOTS; i++) {
            list.add(slots[i].isEmpty() ? null : Identifier.tryParse(slots[i]));
        }
        return Collections.unmodifiableList(list);
    }

    public static String normalizeEquippedJutsusString(@Nullable String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return ";;;;;;;";
        }
        String[] slots = new String[INinjaData.MAX_JUTSU_SLOTS];
        Arrays.fill(slots, "");
        String[] parts = serialized.split(";", -1);
        for (int i = 0; i < Math.min(INinjaData.MAX_JUTSU_SLOTS, parts.length); i++) {
            String trimmed = parts[i] != null ? parts[i].trim() : "";
            if (!trimmed.isEmpty() && Identifier.tryParse(trimmed) != null) {
                slots[i] = trimmed;
            }
        }
        return writeEquippedSlots(slots);
    }

    public static int clampSelectedJutsuSlot(int slot) {
        return Math.max(0, Math.min(INinjaData.MAX_JUTSU_SLOTS - 1, slot));
    }
}
