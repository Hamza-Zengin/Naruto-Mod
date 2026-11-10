package com.sekwah.narutomod.item.armor;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NarutoArmorItem extends ArmorItem implements IShouldHideNameplate, ICurioItem {

    private HumanoidModel armorModel = null;
    private String armorTexture = null;
    private boolean forceHideName = false;

    public NarutoArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return armorTexture;
    }

    public NarutoArmorItem setArmorModel(HumanoidModel<? extends LivingEntity> armorModel) {
        this.armorModel = armorModel;
        return this;
    }

    public NarutoArmorItem setShouldHideNameplate(boolean shouldHide) {
        this.forceHideName = shouldHide;
        return this;
    }

    public NarutoArmorItem setArmorTexture(String armorTexture) {
        this.armorTexture = NarutoMod.MOD_ID + ":" + armorTexture;
        return this;
    }

    @Override
    public boolean shouldHideNameplate(Entity entity) {
        return this.forceHideName;
    }

    private String tooltipKey = null;

    public NarutoArmorItem setTooltipKey(String tooltipKey) {
        this.tooltipKey = tooltipKey;
        return this;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (this.tooltipKey != null) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(this.tooltipKey).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
                return armorModel != null ? armorModel : _default;
            }
        });
    }
}
