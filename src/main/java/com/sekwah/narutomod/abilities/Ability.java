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
            for (net.minecraftforge.registries.RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
                if (entry.isPresent() && entry.get() == this) {
                    this.registryName = entry.getId();
                    break;
                }
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

    public abstract ActivationType activationType();

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

    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, getChakraCost(), getStaminaCost(), 30);
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
        this.performServer(player, ninjaData, 0);
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

        default boolean checkCooldown(Player player, INinjaData ninjaData, String translationKey) {
            if (getCooldown() > 0 && ninjaData.getCooldownEvents().containsKey(translationKey)) {
                player.displayClientMessage(Component.translatable("jutsu.fail.cooldown",
                        Component.translatable(translationKey).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf((int) Math.ceil(ninjaData.getCooldownEvents().get(translationKey).ticks / 20f))).withStyle(ChatFormatting.YELLOW)
                ), true);
                return true;
            }
            return false;
        }

        default void registerCooldown(INinjaData ninjaData, String translationKey) {
            if (getCooldown() > 0) {
                ninjaData.getCooldownEvents().put(translationKey, new CooldownTickEvent(getCooldown()));
            }
        }
    }
}
