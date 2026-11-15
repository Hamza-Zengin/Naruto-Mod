package com.sekwah.narutomod.item.armor;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NarutoArmorItem extends Item implements IShouldHideNameplate {

    @Nullable
    private Object armorModel = null;
    @Nullable
    private Identifier armorTexture = null;
    private boolean forceHideName = false;
    @Nullable
    private String tooltipKey = null;

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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        if (this.tooltipKey != null) {
            tooltip.accept(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GOLD));
        }
    }
}
