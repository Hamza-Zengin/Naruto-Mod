package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoLootEvents {

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();

        if (name.equals(BuiltInLootTables.SIMPLE_DUNGEON) ||
                name.equals(BuiltInLootTables.DESERT_PYRAMID) ||
                name.equals(BuiltInLootTables.JUNGLE_TEMPLE) ||
                name.equals(BuiltInLootTables.ABANDONED_MINESHAFT) ||
                name.equals(BuiltInLootTables.VILLAGE_WEAPONSMITH) ||
                name.equals(BuiltInLootTables.NETHER_BRIDGE) ||
                name.equals(BuiltInLootTables.STRONGHOLD_CORRIDOR)) {

            LootPool.Builder ninjaLootPool = LootPool.lootPool()
                    .name("narutomod_ninja_loot")
                    .setRolls(UniformGenerator.between(1.0F, 2.0F))
                    .add(LootItem.lootTableItem(NarutoItems.SMOKE_BOMB.get()).setWeight(20)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.MILITARY_RATIONS_PILL.get()).setWeight(20)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.KUNAI.get()).setWeight(25)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.SHURIKEN.get()).setWeight(25)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 16.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.FUMA_SHURIKEN.get()).setWeight(10)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.ICHIRAKU_RAMEN.get()).setWeight(15)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.CHAKRA_METAL.get()).setWeight(14)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_RASENGAN.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_CHIDORI.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_SHADOW_CLONE.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_SHADOW_POSSESSION.get()).setWeight(4))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_KIRIN.get()).setWeight(3))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_AMATERASU.get()).setWeight(2))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_SUSANOO.get()).setWeight(2))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_EIGHT_GATES.get()).setWeight(2))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_RASENSHURIKEN.get()).setWeight(2));

            event.getTable().addPool(ninjaLootPool.build());
        }
    }
}
