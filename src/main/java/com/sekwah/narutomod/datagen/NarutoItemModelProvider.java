package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public class NarutoItemModelProvider extends ModelProvider {

    public NarutoItemModelProvider(PackOutput output) {
        super(output, NarutoMod.MOD_ID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.empty();
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // --- Jutsu Scrolls ---
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_RASENGAN.get(), "item/scrolls/scroll_rasengan");
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_CHIDORI.get(), "item/scrolls/scroll_chidori");
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_SHADOW_CLONE.get(), "item/scrolls/scroll_shadow_clone");
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_KIRIN.get(), "item/scrolls/scroll_kirin");
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_EIGHT_GATES.get(), "item/scrolls/scroll_eight_gates");
        simpleItemWithTexture(itemModels, NarutoItems.SCROLL_RASENSHURIKEN.get(), "item/scrolls/scroll_rasenshuriken");

        // --- Basic Items & Weapons (2D items) ---
        simpleItemWithTexture(itemModels, NarutoItems.CHAKRA_METAL.get(), "item/weapons/chakra_metal");
        simpleItemWithTexture(itemModels, NarutoItems.ICHIRAKU_RAMEN.get(), "item/weapons/ichiraku_ramen");
        simpleItemWithTexture(itemModels, NarutoItems.MILITARY_RATIONS_PILL.get(), "item/weapons/military_rations_pill");
        simpleItemWithTexture(itemModels, NarutoItems.SMOKE_BOMB.get(), "item/weapons/smoke_bomb");
        simpleItemWithTexture(itemModels, NarutoBlocks.ITEM_PAPER_BOMB.get(), "item/weapons/paperbomb");
        simpleItemWithTexture(itemModels, NarutoItems.LONELY_MARCH.get(), "item/lonely_march");

        // --- Armor & Masks ---
        simpleItemWithTexture(itemModels, NarutoItems.CHUNIN_VEST.get(), "item/armor/chunin_vest");
        simpleItemWithTexture(itemModels, NarutoItems.ANBU_ARMOR.get(), "item/armor/anbu_armor");
        simpleItemWithTexture(itemModels, NarutoItems.AKATSUKI_CLOAK.get(), "item/armor/akatsuki_cloak");
        simpleItemWithTexture(itemModels, NarutoItems.SHINOBI_LEGGINGS.get(), "item/armor/shinobi_leggings");
        simpleItemWithTexture(itemModels, NarutoItems.SHINOBI_SANDALS.get(), "item/armor/shinobi_sandals");
        simpleItemWithTexture(itemModels, NarutoItems.ANBU_LEGGINGS.get(), "item/armor/anbu_leggings");
        simpleItemWithTexture(itemModels, NarutoItems.ANBU_BOOTS.get(), "item/armor/anbu_boots");
        simpleItemWithTexture(itemModels, NarutoItems.WARRING_STATES_LEGGINGS.get(), "item/armor/warring_states_leggings");
        simpleItemWithTexture(itemModels, NarutoItems.WARRING_STATES_BOOTS.get(), "item/armor/warring_states_boots");
        simpleItemWithTexture(itemModels, NarutoItems.RED_ANBU_MASK.get(), "item/armor/red_anbu_mask");
        simpleItemWithTexture(itemModels, NarutoItems.YELLOW_ANBU_MASK.get(), "item/armor/yellow_anbu_mask");
        simpleItemWithTexture(itemModels, NarutoItems.GREEN_ANBU_MASK.get(), "item/armor/green_anbu_mask");
        simpleItemWithTexture(itemModels, NarutoItems.BLUE_ANBU_MASK.get(), "item/armor/blue_anbu_mask");
        simpleItemWithTexture(itemModels, NarutoItems.MIST_ANBU_MASK.get(), "item/armor/mist_anbu_mask");

        // --- Spawn Eggs ---
        Identifier spawnEggTemplate = Identifier.withDefaultNamespace("item/egg");
        itemModels.itemModelOutput.accept(NarutoItems.ROGUE_NINJA_SPAWN_EGG.get(), ItemModelUtils.plainModel(spawnEggTemplate));
        itemModels.itemModelOutput.accept(NarutoItems.AKATSUKI_NINJA_SPAWN_EGG.get(), ItemModelUtils.plainModel(spawnEggTemplate));

        // --- Block Items ---
        itemModels.itemModelOutput.accept(NarutoBlocks.ITEM_BONSAI_TREE.get(),
                ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/bonsai_tree")));
        itemModels.itemModelOutput.accept(NarutoBlocks.ITEM_TRAINING_DUMMY.get(),
                ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/training_dummy")));

        // --- Headbands (Blank / Custom) ---
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_BLACK.get(), "item/armor/headbands/headband_black_blank");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_BLUE.get(), "item/armor/headbands/headband_blue_blank");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_RED.get(), "item/armor/headbands/headband_red_blank");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_LAVA.get(), "item/armor/headbands/headband_lava");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_YOUTUBE.get(), "item/armor/headbands/headband_youtube");

        // --- Headbands (Villages) ---
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_LEAF.get(), "item/armor/headbands/headband_blue");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_LEAF_BLACK.get(), "item/armor/headbands/headband_black");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_ROCK.get(), "item/armor/headbands/headband_rock");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_SOUND.get(), "item/armor/headbands/headband_sound");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_SAND.get(), "item/armor/headbands/headband_black_rev");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_CLOUD.get(), "item/armor/headbands/headband_black_block");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_GRASS.get(), "item/armor/headbands/headband_black_block");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_MIST.get(), "item/armor/headbands/headband_black_block");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_RAIN.get(), "item/armor/headbands/headband_black_block");
        simpleItemWithTexture(itemModels, NarutoItems.HEADBAND_WATERFALL.get(), "item/armor/headbands/headband_black_block");

        // --- Headbands (Scratched Variants) ---
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_LEAF_SCRATCHED.get(), NarutoItems.HEADBAND_LEAF.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_LEAF_BLACK_SCRATCHED.get(), NarutoItems.HEADBAND_LEAF_BLACK.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_ROCK_SCRATCHED.get(), NarutoItems.HEADBAND_ROCK.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_SAND_SCRATCHED.get(), NarutoItems.HEADBAND_SAND.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_SOUND_SCRATCHED.get(), NarutoItems.HEADBAND_SOUND.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_MIST_SCRATCHED.get(), NarutoItems.HEADBAND_MIST.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_WATERFALL_SCRATCHED.get(), NarutoItems.HEADBAND_WATERFALL.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_CLOUD_SCRATCHED.get(), NarutoItems.HEADBAND_CLOUD.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_RAIN_SCRATCHED.get(), NarutoItems.HEADBAND_RAIN.get());
        scratchedHeadband(itemModels, NarutoItems.HEADBAND_GRASS_SCRATCHED.get(), NarutoItems.HEADBAND_GRASS.get());
    }

    private void simpleItemWithTexture(ItemModelGenerators itemModels, Item item, String texturePath) {
        Identifier modelId = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, texturePath))),
                itemModels.modelOutput
        );
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(modelId));
    }

    private void scratchedHeadband(ItemModelGenerators itemModels, Item item, Item baseItem) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(baseItem)));
    }
}
