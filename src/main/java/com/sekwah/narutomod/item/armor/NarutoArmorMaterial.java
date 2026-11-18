package com.sekwah.narutomod.item.armor;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Balance against {@link net.minecraft.world.item.equipment.ArmorMaterials}
 */
public final class NarutoArmorMaterial {

    public static final ArmorMaterial ANBU_MAT = new ArmorMaterial(
            38,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 8
            ),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            2.5F,
            0.05F,
            ItemTags.REPAIRS_IRON_ARMOR,
            assetKey("anbu")
    );

    public static final ArmorMaterial HEADBAND = new ArmorMaterial(
            35,
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 5
            ),
            8,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            2.1F,
            0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR,
            assetKey("headband")
    );

    public static final ArmorMaterial CHUNIN_MAT = new ArmorMaterial(
            35,
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 6
            ),
            10,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            2.0F,
            0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR,
            assetKey("chunin")
    );

    public static final ArmorMaterial AKATSUKI_MAT = new ArmorMaterial(
            42,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 8
            ),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            3.0F,
            0.1F,
            ItemTags.REPAIRS_LEATHER_ARMOR,
            assetKey("akatsuki")
    );

    public static final ArmorMaterial JONIN_MAT = new ArmorMaterial(
            40,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 8
            ),
            14,
            SoundEvents.ARMOR_EQUIP_IRON,
            2.5F,
            0.05F,
            ItemTags.REPAIRS_IRON_ARMOR,
            assetKey("jonin")
    );

    public static final ArmorMaterial WARRING_STATES_MAT = new ArmorMaterial(
            44,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 9
            ),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.5F,
            0.15F,
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            assetKey("warring_states")
    );

    public static final ArmorMaterial KAGE_MAT = new ArmorMaterial(
            40,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.HELMET, 4,
                    ArmorType.BODY, 7
            ),
            18,
            SoundEvents.ARMOR_EQUIP_GOLD,
            2.5F,
            0.10F,
            ItemTags.REPAIRS_GOLD_ARMOR,
            assetKey("kage")
    );

    private static ResourceKey<EquipmentAsset> assetKey(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, name));
    }

    private NarutoArmorMaterial() {
    }
}
