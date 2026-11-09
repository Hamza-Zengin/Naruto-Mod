package com.sekwah.narutomod.item.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.util.LazyLoadedValue;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvents;

/**
 * Balance against {@link net.minecraft.world.item.ArmorMaterial}
 */
public enum NarutoArmorMaterial implements ArmorMaterial {

    ANBU_MAT("anbu", 38, new int[]{3, 8, 7, 3}, 12, SoundEvents.ARMOR_EQUIP_IRON, 2.5F, 0.05F, () -> Ingredient.EMPTY),
    HEADBAND("headband", 35, new int[]{2, 8, 5, 2}, 8, SoundEvents.ARMOR_EQUIP_LEATHER, 2.1F, 0.0F, () -> Ingredient.EMPTY),
    CHUNIN_MAT("chunin", 35, new int[]{2, 7, 6, 2}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.0F, () -> Ingredient.EMPTY),
    AKATSUKI_MAT("akatsuki", 42, new int[]{3, 8, 8, 3}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 3.0F, 0.1F, () -> Ingredient.EMPTY),
    ;

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    /**
     * May be Deprecated but is used in the vanilla materials
     */
    private final LazyLoadedValue<Ingredient> repairIngredient;

    NarutoArmorMaterial(String name, int durabilityMultiplier, int[] slotProtections, int enchantmentValue, SoundEvent soundEvent, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.slotProtections = slotProtections;
        this.enchantmentValue = enchantmentValue;
        this.sound = soundEvent;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = new LazyLoadedValue<>(repairIngredient);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.getSlot().getIndex()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.slotProtections[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
