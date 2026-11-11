package com.sekwah.narutomod.compat;

import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CuriosCompat {

    private static final ICurioItem DEFAULT_CURIO_ITEM = new ICurioItem() {};

    public static void sendIMC() {
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.HEAD.getMessageBuilder().build());
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.CHARM.getMessageBuilder().build());
    }

    public static void registerCurios() {
        for (RegistryObject<Item> entry : NarutoItems.ITEMS.getEntries()) {
            if (entry.isPresent()) {
                Item item = entry.get();
                if (item instanceof NarutoArmorItem) {
                    CuriosApi.registerCurio(item, DEFAULT_CURIO_ITEM);
                }
            }
        }
    }
}
