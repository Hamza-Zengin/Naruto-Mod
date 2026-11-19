package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Stateless helper for chakra, stamina, experience scaling, and server-side regeneration.
 */
public final class ChakraStaminaLogic {

    public static class RegenInfo {
        public float regenRate;
        public int cooldown;

        public RegenInfo(float regenRate) {
            this.regenRate = regenRate;
        }

        public boolean canRegen() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return true;
        }
    }

    private ChakraStaminaLogic() {}

    public static final float KAGE_HAT_CHAKRA_BONUS = 60.0f;

    public static float computeMaxChakra(float baseMaxChakra, String rank, Identifier clan) {
        return computeMaxChakra(baseMaxChakra, rank, clan, false);
    }

    public static float computeMaxChakra(float baseMaxChakra, String rank, Identifier clan, boolean hasKageHat) {
        NinjaRank currentRank = NinjaRank.fromString(rank);
        float total = baseMaxChakra + currentRank.getChakraBonus();
        if (clan != null && "uzumaki".equals(clan.getPath())) {
            total *= 1.25f;
        }
        if (hasKageHat) {
            total += KAGE_HAT_CHAKRA_BONUS;
        }
        return total;
    }

    public static float computeMaxStamina(float baseMaxStamina, String rank) {
        NinjaRank currentRank = NinjaRank.fromString(rank);
        return baseMaxStamina + currentRank.getStaminaBonus();
    }

    public static void addChakraExp(NinjaData data, float amount) {
        float baseMax = data.getRawMaxChakra();
        if (baseMax >= 1000f) {
            data.setChakraExp(0f);
            return;
        }
        float exp = data.getChakraExp() + amount;
        float neededExp;
        if (baseMax < 150.0f) {
            neededExp = 30.0f + (baseMax - 100.0f) * 0.50f;
        } else {
            neededExp = 55.0f + (baseMax - 150.0f) * 1.10f;
        }
        if (exp >= neededExp && baseMax < 1000f) {
            exp -= neededExp;
            data.setRawMaxChakra(Math.min(baseMax + 5.0f, 1000.0f));
            if (data.getChakra() > data.getMaxChakra()) {
                data.setRawChakra(data.getMaxChakra());
            }
        }
        data.setRawChakraExp(exp);
    }

    public static void addStaminaExp(NinjaData data, float amount) {
        float baseMax = data.getRawMaxStamina();
        if (baseMax >= 800f) {
            data.setStaminaExp(0f);
            return;
        }
        float exp = data.getStaminaExp() + amount;
        float neededExp = 50.0f + (baseMax - 100.0f) * 1.10f;
        if (exp >= neededExp && baseMax < 800f) {
            exp -= neededExp;
            data.setRawMaxStamina(Math.min(baseMax + 5.0f, 800f));
            if (data.getStamina() > data.getMaxStamina()) {
                data.setRawStamina(data.getMaxStamina());
            }
        }
        data.setRawStaminaExp(exp);
    }

    public static void useChakra(NinjaData data, float amount, int cooldown, RegenInfo chakraRegenInfo) {
        if (!data.isCreativeMode()) {
            data.setRawChakra(Math.max(0f, data.getChakra() - amount));
        }
        chakraRegenInfo.cooldown = Math.max(cooldown, chakraRegenInfo.cooldown);
        addChakraExp(data, amount * 0.10f);
    }

    public static void useStamina(NinjaData data, float amount, int cooldown, RegenInfo staminaRegenInfo) {
        if (!data.isCreativeMode()) {
            data.setRawStamina(Math.max(0f, data.getStamina() - amount));
        }
        staminaRegenInfo.cooldown = Math.max(cooldown, staminaRegenInfo.cooldown);
        addStaminaExp(data, amount * 0.10f);
    }

    public static void addChakra(NinjaData data, float amount) {
        data.setRawChakra(Math.min(Math.max(data.getChakra() + amount, 0f), data.getMaxChakra()));
    }

    public static void addStamina(NinjaData data, float amount) {
        data.setRawStamina(Math.min(Math.max(data.getStamina() + amount, 0f), data.getMaxStamina()));
    }

    public static float getChestplateStaminaRegenMultiplier(boolean isChuninOrJoninVest, boolean isAnbuArmor) {
        if (isChuninOrJoninVest) {
            return 1.25f;
        }
        if (isAnbuArmor) {
            return 1.15f;
        }
        return 1.0f;
    }

    public static float getChestplateChakraRegenMultiplier(boolean isSageCloak, boolean isAkatsukiCloak, boolean isJoninVest, boolean isAnbuArmor) {
        if (isSageCloak) {
            return 1.50f;
        }
        if (isAkatsukiCloak) {
            return 1.35f;
        }
        if (isJoninVest) {
            return 1.20f;
        }
        if (isAnbuArmor) {
            return 1.15f;
        }
        return 1.0f;
    }

    public static float getAccessoryRegenMultiplier(float bonus) {
        return 1.0f + Math.max(0.0f, bonus);
    }

    public static void tickRegen(NinjaData data, Player player, RegenInfo staminaRegenInfo, RegenInfo chakraRegenInfo) {
        Item chestItem = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        boolean isJoninVest = com.sekwah.narutomod.events.PlayerEvents.isJoninVest(chestItem);
        boolean isChuninVest = chestItem == NarutoItems.CHUNIN_VEST.get();
        boolean isAnbuArmor = chestItem == NarutoItems.ANBU_ARMOR.get();
        boolean isAkatsukiCloak = chestItem == NarutoItems.AKATSUKI_CLOAK.get();
        boolean isSageCloak = chestItem == NarutoItems.SAGE_CLOAK.get();
        Identifier clan = data.getClan();

        if (staminaRegenInfo.canRegen()) {
            float staminaRegen = NarutoConfig.staminaRegenRate > 0 ? NarutoConfig.staminaRegenRate : staminaRegenInfo.regenRate;
            if (clan != null && "nara".equals(clan.getPath())) {
                staminaRegen *= 1.3f;
            }
            staminaRegen *= getChestplateStaminaRegenMultiplier(isChuninVest || isJoninVest, isAnbuArmor);
            staminaRegen *= getAccessoryRegenMultiplier(NarutoArmorItem.getAccessoryStaminaRegenBonus(player));
            data.setRawStamina(data.getStamina() + staminaRegen);
        }

        if (chakraRegenInfo.canRegen()) {
            float chakraRegen = NarutoConfig.chakraRegenRate > 0 ? NarutoConfig.chakraRegenRate : chakraRegenInfo.regenRate;
            if (clan != null && "uzumaki".equals(clan.getPath())) {
                chakraRegen *= 2.0f;
            } else if (clan != null && "senju".equals(clan.getPath())) {
                chakraRegen *= 1.4f;
            }
            chakraRegen *= getChestplateChakraRegenMultiplier(isSageCloak, isAkatsukiCloak, isJoninVest, isAnbuArmor);
            chakraRegen *= getAccessoryRegenMultiplier(NarutoArmorItem.getAccessoryChakraRegenBonus(player));
            data.setRawChakra(data.getChakra() + chakraRegen);
        }

        if (clan != null && "senju".equals(clan.getPath()) && player.tickCount % 80 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }

        float curMaxStamina = data.getMaxStamina();
        float curMaxChakra = data.getMaxChakra();
        data.setRawStamina(Math.min(Math.max(data.getStamina(), 0f), curMaxStamina));
        data.setRawChakra(Math.min(Math.max(data.getChakra(), 0f), curMaxChakra));
    }
}
