package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public abstract class Ability {

    private ResourceLocation registryName;

    public Ability() {
    }

    public ResourceLocation getRegistryName() {
        if (this.registryName == null) {
            try {
                for (net.minecraftforge.registries.RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
                    if (entry.isPresent() && entry.get() == this) {
                        this.registryName = entry.getId();
                        break;
                    }
                }
            } catch (Throwable ignored) {
                // Defensive guard for non-bootstrapped test environments
            }
        }
        return this.registryName;
    }

    public Ability setRegistryName(ResourceLocation registryName) {
        this.registryName = registryName;
        return this;
    }

    public NinjaRank getRequiredRank() {
        return NinjaRank.GENIN;
    }

    public java.util.List<ResourceLocation> getPrerequisites() {
        return java.util.Collections.emptyList();
    }

    public int getRequiredMastery() {
        return 0;
    }

    public boolean meetsPrerequisites(INinjaData ninjaData) {
        if (ninjaData == null) return false;
        java.util.List<ResourceLocation> prereqs = getPrerequisites();
        if (prereqs == null || prereqs.isEmpty()) {
            return true;
        }
        int reqMastery = getRequiredMastery();
        for (ResourceLocation prereq : prereqs) {
            boolean unlocked = (ninjaData.getUnlockedAbilities() != null && ninjaData.getUnlockedAbilities().contains(prereq))
                    || ninjaData.isAbilityUnlocked(prereq);
            if (!unlocked) {
                return false;
            }
            if (reqMastery > 0 && ninjaData.getMastery(prereq) < reqMastery) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether this ability can currently be activated by the player.
     */
    public boolean canActivate(Player player, INinjaData ninjaData) {
        return true;
    }

    public enum ActivationType {
        INSTANT,
        TOGGLE,
        CHANNELED
    }

    /**
     * For now the combo to register the ability with. May be overrideable in the future.
     */
    public long defaultCombo() {
        return -1;
    }

    /**
     * Hand seals required / expected for this jutsu combination.
     * By default, derives expected seals from digits of defaultCombo() (e.g. 121 -> [1, 2, 1]).
     */
    public byte[] getExpectedSeals() {
        long combo = defaultCombo();
        if (combo <= 0) {
            return new byte[0];
        }
        String s = Long.toString(combo);
        byte[] seals = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            seals[i] = (byte) Character.digit(s.charAt(i), 10);
        }
        return seals;
    }

    /**
     * Checks if the entered seal list matches the ability's expected seal sequence or defaultCombo digits.
     */
    public boolean matchesExpectedSeals(java.util.List<Byte> activeSeals) {
        if (activeSeals == null || activeSeals.isEmpty()) {
            return false;
        }
        byte[] expected = getExpectedSeals();
        if (matchesSequence(activeSeals, expected)) {
            return true;
        }
        long combo = defaultCombo();
        if (combo > 0) {
            String s = Long.toString(combo);
            byte[] comboBytes = new byte[s.length()];
            for (int i = 0; i < s.length(); i++) {
                comboBytes[i] = (byte) Character.digit(s.charAt(i), 10);
            }
            if (matchesSequence(activeSeals, comboBytes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesSequence(java.util.List<Byte> activeSeals, byte[] expected) {
        if (expected == null || expected.length == 0 || activeSeals.size() < expected.length) {
            return false;
        }
        int offset = activeSeals.size() - expected.length;
        for (int i = 0; i < expected.length; i++) {
            if (activeSeals.get(offset + i) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the effective chakra cost with a 25% discount if the entered seals match.
     */
    public float getEffectiveChakraCost(INinjaData ninjaData) {
        float cost = getChakraCost();
        if (ninjaData != null && matchesExpectedSeals(ninjaData.getActiveSeals())) {
            return cost * 0.75f;
        }
        return cost;
    }

    public abstract ActivationType activationType();

    public ChakraElement getChakraElement() {
        return ChakraElement.NONE;
    }

    public float getChakraCost() {
        ResourceLocation reg = getRegistryName();
        if (reg != null) {
            JutsuDataManager.JutsuData data = JutsuDataManager.get(reg);
            if (data != null && data.chakraCost() >= 0) {
                return data.chakraCost();
            }
        }
        return getDefaultChakraCost();
    }

    public float getStaminaCost() {
        ResourceLocation reg = getRegistryName();
        if (reg != null) {
            JutsuDataManager.JutsuData data = JutsuDataManager.get(reg);
            if (data != null && data.staminaCost() >= 0) {
                return data.staminaCost();
            }
        }
        return getDefaultStaminaCost();
    }

    public float getDamage() {
        ResourceLocation reg = getRegistryName();
        if (reg != null) {
            JutsuDataManager.JutsuData data = JutsuDataManager.get(reg);
            if (data != null && data.damage() >= 0) {
                return data.damage();
            }
        }
        return getDefaultDamage();
    }

    public float getDamage(INinjaData ninjaData) {
        float base = getDamage();
        if (ninjaData == null) return base;
        ResourceLocation reg = getRegistryName();
        int mastery = reg != null ? ninjaData.getMastery(reg) : 0;
        return base * getPowerMultiplier(mastery);
    }

    public float getDamage(int mastery) {
        return getDamage() * getPowerMultiplier(mastery);
    }

    public int getCooldown() {
        ResourceLocation reg = getRegistryName();
        if (reg != null) {
            JutsuDataManager.JutsuData data = JutsuDataManager.get(reg);
            if (data != null && data.cooldown() >= 0) {
                return data.cooldown();
            }
        }
        return getDefaultCooldown();
    }

    public int getCooldown(INinjaData ninjaData) {
        int base = getCooldown();
        if (ninjaData == null) return base;
        ResourceLocation reg = getRegistryName();
        int mastery = reg != null ? ninjaData.getMastery(reg) : 0;
        return getCooldown(mastery);
    }

    public int getCooldown(int mastery) {
        return Math.max(1, (int) (getCooldown() * getCooldownMultiplier(mastery)));
    }

    public float getDefaultChakraCost() {
        return 0f;
    }

    public float getDefaultStaminaCost() {
        return 0f;
    }

    public float getDefaultDamage() {
        return 0f;
    }

    public int getDefaultCooldown() {
        return 0;
    }

    public static int getMasteryTier(int mastery) {
        if (mastery < 50) return 1;
        if (mastery < 150) return 2;
        if (mastery < 350) return 3;
        if (mastery < 700) return 4;
        return 5;
    }

    public static float getChakraCostMultiplier(int mastery) {
        int tier = getMasteryTier(mastery);
        return switch (tier) {
            case 2 -> 0.90f;
            case 3 -> 0.80f;
            case 4 -> 0.70f;
            case 5 -> 0.60f;
            default -> 1.00f;
        };
    }

    public static float getPowerMultiplier(int mastery) {
        int tier = getMasteryTier(mastery);
        return switch (tier) {
            case 2 -> 1.12f;
            case 3 -> 1.25f;
            case 4 -> 1.40f;
            case 5 -> 1.60f;
            default -> 1.00f;
        };
    }

    public static float getCooldownMultiplier(int mastery) {
        int tier = getMasteryTier(mastery);
        return switch (tier) {
            case 2 -> 0.90f;
            case 3 -> 0.80f;
            case 4 -> 0.70f;
            case 5 -> 0.60f;
            default -> 1.00f;
        };
    }

    public float getScaledChakraCost(INinjaData ninjaData) {
        float baseCost = getEffectiveChakraCost(ninjaData);
        if (ninjaData == null) return baseCost;
        ResourceLocation reg = getRegistryName();
        int mastery = reg != null ? ninjaData.getMastery(reg) : 0;
        return Math.max(1.0f, baseCost * getChakraCostMultiplier(mastery));
    }

    public boolean requiresSealActivationDelay() {
        return this.defaultCombo() > 0 && this.getDefaultChakraCost() > 0;
    }

    public int getBaseActivationDelayTicks() {
        NinjaRank rank = this.getRequiredRank();
        if (rank == null) return 20;
        return switch (rank) {
            case JONIN, ANBU, KAGE -> 12;
            case CHUNIN -> 16;
            default -> 20;
        };
    }

    public int getScaledActivationDelay(int mastery) {
        if (!this.requiresSealActivationDelay()) {
            return 0;
        }
        int baseDelayTicks = this.getBaseActivationDelayTicks();
        int periods = Math.min(10, mastery / 100);
        double multiplier = 1.0 - (periods * 0.10);
        return Math.max(0, (int) Math.round(baseDelayTicks * multiplier));
    }

    public int getActivationDelayTicks(INinjaData ninjaData) {
        if (ninjaData == null || !this.requiresSealActivationDelay()) return 0;
        ResourceLocation reg = getRegistryName();
        int mastery = reg != null ? ninjaData.getMastery(reg) : 0;
        return getScaledActivationDelay(mastery);
    }

    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), getStaminaCost(), 30);
    }

    public boolean handleCost(Player player, INinjaData ninjaData) {
        return handleCost(player, ninjaData, 0);
    }

    /**
     * Reusable helper to check and consume chakra and stamina with standard user notifications.
     */
    public boolean tryConsume(Player player, INinjaData ninjaData, float chakraCost, float staminaCost, int regenDelay) {
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getStamina() < staminaCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        if (chakraCost > 0) ninjaData.useChakra(chakraCost, regenDelay);
        if (staminaCost > 0) ninjaData.useStamina(staminaCost, regenDelay);
        return true;
    }

    public boolean logInChat() {
        return true;
    }

    public SoundEvent castingSound() {
        return NarutoSounds.JUTSU_CAST.get();
    }

    public String getTranslationKey() {
        ResourceLocation reg = this.getRegistryName();
        return reg != null ? reg.toString() : "ability.unknown";
    }

    public abstract void performServer(Player player, INinjaData ninjaData, int ticksActive);

    public void performServer(Player player, INinjaData ninjaData) {
        this.perform(player, ninjaData, 0);
    }

    public void perform(Player player, INinjaData ninjaData, int ticksActive) {
        this.performServer(player, ninjaData, ticksActive);
        if (ninjaData != null && this.activationType() != ActivationType.TOGGLE) {
            ResourceLocation reg = this.getRegistryName();
            if (reg != null) {
                int currentMastery = ninjaData.getMastery(reg);
                int gain = (currentMastery < 150) ? 3 : (currentMastery < 350 ? 2 : 1);
                ninjaData.addMastery(reg, gain);
            }
        }
    }

    public void perform(Player player, INinjaData ninjaData) {
        this.perform(player, ninjaData, 0);
    }

    public interface Toggled {
        void performToggleClient(Player player, INinjaData ninjaData);
    }

    public interface Channeled {
        default boolean canActivateBelowMinCharge() {
            return true;
        }

        default boolean useChargedMessages() {
            return false;
        }

        default void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {}
    }

    public interface HandleEnded {
        void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive);
    }

    public interface Cooldown {
        default int getCooldown() {
            if (this instanceof Ability ability) {
                return ability.getCooldown();
            }
            return 0;
        }

        default int getCooldown(INinjaData ninjaData) {
            if (this instanceof Ability ability) {
                return ability.getCooldown(ninjaData);
            }
            return getCooldown();
        }

        default boolean checkCooldown(Player player, INinjaData ninjaData, String translationKey) {
            if (getCooldown(ninjaData) > 0 && ninjaData.getCooldownEvents().containsKey(translationKey)) {
                player.displayClientMessage(Component.translatable("jutsu.fail.cooldown",
                        Component.translatable(translationKey).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf((int) Math.ceil(ninjaData.getCooldownEvents().get(translationKey).ticks / 20f))).withStyle(ChatFormatting.YELLOW)
                ), true);
                return true;
            }
            return false;
        }

        default void registerCooldown(INinjaData ninjaData, String translationKey) {
            int cd = getCooldown(ninjaData);
            if (cd > 0) {
                ninjaData.getCooldownEvents().put(translationKey, new CooldownTickEvent(cd));
            }
        }
    }
}
