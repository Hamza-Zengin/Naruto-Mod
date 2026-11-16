package com.sekwah.narutomod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/**
 * Data-driven Global Loot Modifier for injecting Naruto Mod items and jutsu scrolls
 * into target loot tables with configurable drop chance and stack count range.
 */
public class AddNarutoLootModifier extends LootModifier {

    public static final MapCodec<AddNarutoLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(inst.group(
                    Identifier.CODEC.listOf().optionalFieldOf("target_loot_tables", List.of()).forGetter(e -> e.targetLootTables),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(e -> e.chance),
                    Codec.intRange(1, 64).optionalFieldOf("min_count", 1).forGetter(e -> e.minCount),
                    Codec.intRange(1, 64).optionalFieldOf("max_count", 1).forGetter(e -> e.maxCount)
            )).apply(inst, AddNarutoLootModifier::new)
    );

    private final List<Identifier> targetLootTables;
    private final Item item;
    private final float chance;
    private final int minCount;
    private final int maxCount;

    public AddNarutoLootModifier(LootItemCondition[] conditions, int priority,
                                 List<Identifier> targetLootTables, Item item, float chance,
                                 int minCount, int maxCount) {
        super(conditions, priority);
        this.targetLootTables = targetLootTables;
        this.item = item;
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = Math.max(minCount, maxCount);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!targetLootTables.isEmpty() && !targetLootTables.contains(context.getQueriedLootTableId())) {
            return generatedLoot;
        }

        if (chance >= 1.0F || context.getRandom().nextFloat() < chance) {
            int count = minCount >= maxCount
                    ? minCount
                    : Mth.nextInt(context.getRandom(), minCount, maxCount);
            generatedLoot.add(new ItemStack(item, count));
        }

        return generatedLoot;
    }
}
