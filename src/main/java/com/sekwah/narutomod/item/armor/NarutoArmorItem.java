package com.sekwah.narutomod.item.armor;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NarutoArmorItem extends Item implements IShouldHideNameplate {

    public static final float ANBU_BOOTS_BACKSTAB_SYNERGY_BONUS = 0.15F;

    private static final EquipmentSlot[] ALL_ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] ACCESSORY_ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Nullable
    private Object armorModel = null;
    @Nullable
    private Identifier armorTexture = null;
    private boolean forceHideName = false;
    @Nullable
    private String tooltipKey = null;

    // Declarative passive stat bonuses
    private float staminaRegenBonus = 0.0F;
    private float chakraRegenBonus = 0.0F;
    private float fallDamageReduction = 0.0F;
    private float knockbackResistanceBonus = 0.0F;
    private float jutsuDamageReduction = 0.0F;
    private float movementSpeedBonus = 0.0F;
    private float stealthSpeedBonus = 0.0F;
    private boolean anbuStealthSynergy = false;

    public NarutoArmorItem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material, type));
    }

    @Nullable
    public Object getArmorModel() {
        return this.armorModel;
    }

    @Nullable
    public Identifier getArmorTexture() {
        return this.armorTexture;
    }

    public NarutoArmorItem setArmorModel(Object armorModel) {
        this.armorModel = armorModel;
        return this;
    }

    public NarutoArmorItem setShouldHideNameplate(boolean shouldHide) {
        this.forceHideName = shouldHide;
        return this;
    }

    public NarutoArmorItem setArmorTexture(String armorTexture) {
        this.armorTexture = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, armorTexture);
        return this;
    }

    @Override
    public boolean shouldHideNameplate(Entity entity) {
        return this.forceHideName;
    }

    public NarutoArmorItem setTooltipKey(String tooltipKey) {
        this.tooltipKey = tooltipKey;
        return this;
    }

    public NarutoArmorItem setStaminaRegenBonus(float staminaRegenBonus) {
        this.staminaRegenBonus = Math.max(0.0F, staminaRegenBonus);
        return this;
    }

    public NarutoArmorItem setChakraRegenBonus(float chakraRegenBonus) {
        this.chakraRegenBonus = Math.max(0.0F, chakraRegenBonus);
        return this;
    }

    public NarutoArmorItem setFallDamageReduction(float fallDamageReduction) {
        this.fallDamageReduction = Math.max(0.0F, Math.min(0.95F, fallDamageReduction));
        return this;
    }

    public NarutoArmorItem setKnockbackResistanceBonus(float knockbackResistanceBonus) {
        this.knockbackResistanceBonus = Math.max(0.0F, Math.min(0.95F, knockbackResistanceBonus));
        return this;
    }

    public NarutoArmorItem setJutsuDamageReduction(float jutsuDamageReduction) {
        this.jutsuDamageReduction = Math.max(0.0F, Math.min(0.95F, jutsuDamageReduction));
        return this;
    }

    public NarutoArmorItem setMovementSpeedBonus(float movementSpeedBonus) {
        this.movementSpeedBonus = Math.max(0.0F, movementSpeedBonus);
        return this;
    }

    public NarutoArmorItem setStealthSpeedBonus(float stealthSpeedBonus) {
        this.stealthSpeedBonus = Math.max(0.0F, stealthSpeedBonus);
        return this;
    }

    public NarutoArmorItem setAnbuStealthSynergy(boolean anbuStealthSynergy) {
        this.anbuStealthSynergy = anbuStealthSynergy;
        return this;
    }

    public float getStaminaRegenBonus() {
        return this.staminaRegenBonus;
    }

    public float getChakraRegenBonus() {
        return this.chakraRegenBonus;
    }

    public float getFallDamageReduction() {
        return this.fallDamageReduction;
    }

    public float getKnockbackResistanceBonus() {
        return this.knockbackResistanceBonus;
    }

    public float getJutsuDamageReduction() {
        return this.jutsuDamageReduction;
    }

    public float getMovementSpeedBonus() {
        return this.movementSpeedBonus;
    }

    public float getStealthSpeedBonus() {
        return this.stealthSpeedBonus;
    }

    public boolean hasAnbuStealthSynergy() {
        return this.anbuStealthSynergy;
    }

    public static float getAccessoryStaminaRegenBonus(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ACCESSORY_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getStaminaRegenBonus();
            }
        }
        return total;
    }

    public static float getAccessoryChakraRegenBonus(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ACCESSORY_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getChakraRegenBonus();
            }
        }
        return total;
    }

    public static float getTotalFallDamageReduction(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getFallDamageReduction();
            }
        }
        return Math.min(0.90F, total);
    }

    public static float getTotalKnockbackResistance(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getKnockbackResistanceBonus();
            }
        }
        return Math.min(0.85F, total);
    }

    public static float getTotalJutsuDamageReduction(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getJutsuDamageReduction();
            }
        }
        return Math.min(0.60F, total);
    }

    public static float getTotalMovementSpeedBonus(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getMovementSpeedBonus();
            }
        }
        return total;
    }

    public static float getTotalStealthSpeedBonus(Player player) {
        if (player == null) return 0.0F;
        float total = 0.0F;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor) {
                total += armor.getStealthSpeedBonus();
            }
        }
        return total;
    }

    public static boolean hasAnbuStealthSynergy(Player player) {
        if (player == null) return false;
        for (EquipmentSlot slot : ALL_ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof NarutoArmorItem armor && armor.hasAnbuStealthSynergy()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        if (this.tooltipKey != null) {
            tooltip.accept(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GOLD));
        }
    }
}
