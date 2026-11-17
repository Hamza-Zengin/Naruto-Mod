package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.loot.AddNarutoLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * NeoForge 26.3 DataGen provider for Global Loot Modifiers.
 * Outputs directly to data/narutomod/loot_modifiers/*.json without a legacy global_loot_modifiers.json index.
 */
public class NarutoGlobalLootModifierProvider extends GlobalLootModifierProvider {

    private static final List<Identifier> CHEST_LOOT_TABLES = List.of(
            Identifier.withDefaultNamespace("chests/simple_dungeon"),
            Identifier.withDefaultNamespace("chests/desert_pyramid"),
            Identifier.withDefaultNamespace("chests/jungle_temple"),
            Identifier.withDefaultNamespace("chests/abandoned_mineshaft"),
            Identifier.withDefaultNamespace("chests/village/village_weaponsmith"),
            Identifier.withDefaultNamespace("chests/nether_bridge"),
            Identifier.withDefaultNamespace("chests/stronghold_corridor")
    );

    public NarutoGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, NarutoMod.MOD_ID);
    }

    @Override
    protected void start() {
        addChestLoot("chest_kunai", NarutoItems.KUNAI.get(), 0.25F, 4, 12);
        addChestLoot("chest_shuriken", NarutoItems.SHURIKEN.get(), 0.25F, 4, 16);
        addChestLoot("chest_smoke_bomb", NarutoItems.SMOKE_BOMB.get(), 0.2F, 2, 6);
        addChestLoot("chest_military_rations_pill", NarutoItems.MILITARY_RATIONS_PILL.get(), 0.2F, 1, 4);
        addChestLoot("chest_ichiraku_ramen", NarutoItems.ICHIRAKU_RAMEN.get(), 0.15F, 1, 3);
        addChestLoot("chest_chakra_metal", NarutoItems.CHAKRA_METAL.get(), 0.14F, 1, 4);
        addChestLoot("chest_fuma_shuriken", NarutoItems.FUMA_SHURIKEN.get(), 0.1F, 1, 3);
        addChestLoot("chest_scroll_rasengan", NarutoItems.SCROLL_RASENGAN.get(), 0.05F, 1, 1);
        addChestLoot("chest_scroll_chidori", NarutoItems.SCROLL_CHIDORI.get(), 0.05F, 1, 1);
        addChestLoot("chest_scroll_shadow_clone", NarutoItems.SCROLL_SHADOW_CLONE.get(), 0.05F, 1, 1);
        addChestLoot("chest_scroll_kirin", NarutoItems.SCROLL_KIRIN.get(), 0.03F, 1, 1);
        addChestLoot("chest_scroll_eight_gates", NarutoItems.SCROLL_EIGHT_GATES.get(), 0.02F, 1, 1);
        addChestLoot("chest_scroll_rasenshuriken", NarutoItems.SCROLL_RASENSHURIKEN.get(), 0.02F, 1, 1);
    }

    private void addChestLoot(String name, Item item, float chance, int minCount, int maxCount) {
        add(name, new AddNarutoLootModifier(
                Optional.empty(),
                IGlobalLootModifier.DEFAULT_PRIORITY,
                CHEST_LOOT_TABLES,
                item,
                chance,
                minCount,
                maxCount
        ));
    }
}
