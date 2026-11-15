package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoLootEvents {

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceKey<LootTable> key = event.getKey();

        if (key.equals(BuiltInLootTables.SIMPLE_DUNGEON) ||
                key.equals(BuiltInLootTables.DESERT_PYRAMID) ||
                key.equals(BuiltInLootTables.JUNGLE_TEMPLE) ||
                key.equals(BuiltInLootTables.ABANDONED_MINESHAFT) ||
                key.equals(BuiltInLootTables.VILLAGE_WEAPONSMITH) ||
                key.equals(BuiltInLootTables.NETHER_BRIDGE) ||
                key.equals(BuiltInLootTables.STRONGHOLD_CORRIDOR)) {

            LootPool.Builder ninjaLootPool = LootPool.lootPool()
                    .name("narutomod_ninja_loot")
                    .setRolls(ContextIntProviders.between(1, 2))
                    .add(LootItem.lootTableItem(NarutoItems.SMOKE_BOMB.get()).setWeight(20)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6))))
                    .add(LootItem.lootTableItem(NarutoItems.MILITARY_RATIONS_PILL.get()).setWeight(20)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4))))
                    .add(LootItem.lootTableItem(NarutoItems.KUNAI.get()).setWeight(25)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(4, 12))))
                    .add(LootItem.lootTableItem(NarutoItems.SHURIKEN.get()).setWeight(25)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(4, 16))))
                    .add(LootItem.lootTableItem(NarutoItems.FUMA_SHURIKEN.get()).setWeight(10)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3))))
                    .add(LootItem.lootTableItem(NarutoItems.ICHIRAKU_RAMEN.get()).setWeight(15)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3))))
                    .add(LootItem.lootTableItem(NarutoItems.CHAKRA_METAL.get()).setWeight(14)
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4))))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_RASENGAN.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_CHIDORI.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_SHADOW_CLONE.get()).setWeight(5))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_KIRIN.get()).setWeight(3))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_EIGHT_GATES.get()).setWeight(2))
                    .add(LootItem.lootTableItem(NarutoItems.SCROLL_RASENSHURIKEN.get()).setWeight(2));

            event.getTable().addPool(ninjaLootPool.build());
        }
    }
}
