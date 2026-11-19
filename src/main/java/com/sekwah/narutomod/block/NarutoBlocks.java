package com.sekwah.narutomod.block;

import com.sekwah.narutomod.block.weapons.MakibishiBlock;
import com.sekwah.narutomod.block.weapons.PaperBombBlock;
import com.sekwah.narutomod.block.weapons.WireTrapBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<Block> PAPER_BOMB = BLOCKS.registerBlock("paper_bomb",
            props -> new PaperBombBlock(props
                    .mapColor(MapColor.FIRE)
                    .noCollision()
                    .instabreak()
                    .strength(0.5F)
                    .pushReaction(PushReaction.POPPED)
                    .sound(SoundType.CROP)));

    public static final DeferredItem<BlockItem> ITEM_PAPER_BOMB = BLOCK_ITEMS.registerSimpleBlockItem(PAPER_BOMB);

    public static final DeferredBlock<Block> BONSAI_TREE = BLOCKS.registerBlock("bonsai_tree",
            props -> new RotatableDecorativeBlock(props
                    .mapColor(MapColor.PLANT)
                    .noOcclusion()
                    .strength(0.5F)
                    .sound(SoundType.CROP),
                    Block.box(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D)));

    public static final DeferredItem<BlockItem> ITEM_BONSAI_TREE = BLOCK_ITEMS.registerSimpleBlockItem(BONSAI_TREE);

    public static final DeferredBlock<Block> TRAINING_DUMMY = BLOCKS.registerBlock("training_dummy",
            props -> new TrainingDummyBlock(props
                    .mapColor(MapColor.WOOD)
                    .noOcclusion()
                    .strength(3.0F, 18.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> ITEM_TRAINING_DUMMY = BLOCK_ITEMS.registerSimpleBlockItem(TRAINING_DUMMY);

    public static final DeferredBlock<Block> AMATERASU_FIRE = BLOCKS.registerBlock("amaterasu_fire",
            props -> new AmaterasuFireBlock(props
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> 7)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.POPPED)
                    .noLootTable()));

    public static final DeferredBlock<Block> CHAKRA_METAL_ORE = BLOCKS.registerBlock("chakra_metal_ore",
            props -> new DropExperienceBlock(UniformInt.of(1, 3), props
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)
                    .lightLevel(state -> 3)
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> ITEM_CHAKRA_METAL_ORE = BLOCK_ITEMS.registerSimpleBlockItem(CHAKRA_METAL_ORE);

    public static final DeferredBlock<Block> DEEPSLATE_CHAKRA_METAL_ORE = BLOCKS.registerBlock("deepslate_chakra_metal_ore",
            props -> new DropExperienceBlock(UniformInt.of(1, 3), props
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .lightLevel(state -> 3)
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredItem<BlockItem> ITEM_DEEPSLATE_CHAKRA_METAL_ORE = BLOCK_ITEMS.registerSimpleBlockItem(DEEPSLATE_CHAKRA_METAL_ORE);

    public static final DeferredBlock<Block> CHAKRA_METAL_BLOCK = BLOCKS.registerBlock("chakra_metal_block",
            props -> new Block(props
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ITEM_CHAKRA_METAL_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem(CHAKRA_METAL_BLOCK);

    public static final DeferredBlock<Block> MAKIBISHI = BLOCKS.registerBlock("makibishi",
            props -> new MakibishiBlock(props
                    .mapColor(MapColor.METAL)
                    .noCollision()
                    .noOcclusion()
                    .strength(0.8F)
                    .pushReaction(PushReaction.POPPED)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ITEM_MAKIBISHI = BLOCK_ITEMS.registerSimpleBlockItem(MAKIBISHI);

    public static final DeferredBlock<Block> WIRE_TRAP = BLOCKS.registerBlock("wire_trap",
            props -> new WireTrapBlock(props
                    .mapColor(MapColor.METAL)
                    .noCollision()
                    .noOcclusion()
                    .strength(0.6F)
                    .pushReaction(PushReaction.POPPED)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ITEM_WIRE_TRAP = BLOCK_ITEMS.registerSimpleBlockItem(WIRE_TRAP);

    public static final DeferredBlock<Block> WEAPON_RACK = BLOCKS.registerBlock("weapon_rack",
            props -> new WeaponRackBlock(props
                    .mapColor(MapColor.WOOD)
                    .noOcclusion()
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> ITEM_WEAPON_RACK = BLOCK_ITEMS.registerSimpleBlockItem(WEAPON_RACK);

    // --- Village building blocks (Phase C-1) ---
    public static final DeferredBlock<Block> TATAMI = BLOCKS.registerBlock("tatami",
            props -> new Block(props
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)));

    public static final DeferredItem<BlockItem> ITEM_TATAMI = BLOCK_ITEMS.registerSimpleBlockItem(TATAMI);

    public static final DeferredBlock<Block> TATAMI_SLAB = BLOCKS.registerBlock("tatami_slab",
            props -> new SlabBlock(props
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)));

    public static final DeferredItem<BlockItem> ITEM_TATAMI_SLAB = BLOCK_ITEMS.registerSimpleBlockItem(TATAMI_SLAB);

    public static final DeferredBlock<Block> SHOJI_PANEL = BLOCKS.registerBlock("shoji_panel",
            props -> new IronBarsBlock(props
                    .mapColor(MapColor.WOOD)
                    .strength(0.3F)
                    .noOcclusion()
                    .sound(SoundType.BAMBOO)));

    public static final DeferredItem<BlockItem> ITEM_SHOJI_PANEL = BLOCK_ITEMS.registerSimpleBlockItem(SHOJI_PANEL);

    private static final VoxelShape STONE_LANTERN_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 2.0D, 13.0D),
            Block.box(6.0D, 2.0D, 6.0D, 10.0D, 7.0D, 10.0D),
            Block.box(4.0D, 7.0D, 4.0D, 12.0D, 12.0D, 12.0D),
            Block.box(2.0D, 12.0D, 2.0D, 14.0D, 14.0D, 14.0D),
            Block.box(6.0D, 14.0D, 6.0D, 10.0D, 16.0D, 10.0D));

    public static final DeferredBlock<Block> STONE_LANTERN = BLOCKS.registerBlock("stone_lantern",
            props -> new RotatableDecorativeBlock(props
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F, 6.0F)
                    .noOcclusion()
                    .lightLevel(state -> 13)
                    .sound(SoundType.STONE),
                    STONE_LANTERN_SHAPE));

    public static final DeferredItem<BlockItem> ITEM_STONE_LANTERN = BLOCK_ITEMS.registerSimpleBlockItem(STONE_LANTERN);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
