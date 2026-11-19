package com.sekwah.narutomod.block;

import com.sekwah.narutomod.block.weapons.MakibishiBlock;
import com.sekwah.narutomod.block.weapons.PaperBombBlock;
import com.sekwah.narutomod.block.weapons.WireTrapBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ITEM_MAKIBISHI = BLOCK_ITEMS.registerSimpleBlockItem(MAKIBISHI);

    public static final DeferredBlock<Block> WIRE_TRAP = BLOCKS.registerBlock("wire_trap",
            props -> new WireTrapBlock(props
                    .mapColor(MapColor.METAL)
                    .noCollision()
                    .noOcclusion()
                    .strength(0.6F)
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ITEM_WIRE_TRAP = BLOCK_ITEMS.registerSimpleBlockItem(WIRE_TRAP);

    public static final DeferredBlock<Block> WEAPON_RACK = BLOCKS.registerBlock("weapon_rack",
            props -> new WeaponRackBlock(props
                    .mapColor(MapColor.WOOD)
                    .noOcclusion()
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> ITEM_WEAPON_RACK = BLOCK_ITEMS.registerSimpleBlockItem(WEAPON_RACK);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
