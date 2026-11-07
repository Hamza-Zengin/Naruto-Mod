package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
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

    public abstract boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount);

    public boolean handleCost(Player player, INinjaData ninjaData) {
        return handleCost(player, ninjaData, 0);
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
        int getCooldown();

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
