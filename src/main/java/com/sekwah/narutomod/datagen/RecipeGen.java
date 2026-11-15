package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeGen extends RecipeProvider {
    public RecipeGen(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
    }

    @Override
    protected void buildRecipes() {

        this.shaped(RecipeCategory.COMBAT, NarutoItems.RED_ANBU_MASK.get())
                .define('W', Items.DYED_TERRACOTTA.white())
                .define('R', Items.DYE.red())
                .pattern("WRW")
                .pattern("WWW")
                .pattern(" W ")
                .unlockedBy("has_red_dye", has(Items.DYE.red()))
                .unlockedBy("has_white_terracotta", has(Items.DYED_TERRACOTTA.white()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.YELLOW_ANBU_MASK.get())
                .define('W', Items.DYED_TERRACOTTA.white())
                .define('Y', Items.DYE.yellow())
                .pattern("WYW")
                .pattern("WWW")
                .pattern(" W ")
                .unlockedBy("has_yellow_dye", has(Items.DYE.yellow()))
                .unlockedBy("has_white_terracotta", has(Items.DYED_TERRACOTTA.white()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.BLUE_ANBU_MASK.get())
                .define('W', Items.DYED_TERRACOTTA.white())
                .define('B', Items.DYE.blue())
                .pattern("WBW")
                .pattern("WWW")
                .pattern(" W ")
                .unlockedBy("has_blue_dye", has(Items.DYE.blue()))
                .unlockedBy("has_white_terracotta", has(Items.DYED_TERRACOTTA.white()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.GREEN_ANBU_MASK.get())
                .define('W', Items.DYED_TERRACOTTA.white())
                .define('G', Items.DYE.green())
                .pattern("WGW")
                .pattern("WWW")
                .pattern(" W ")
                .unlockedBy("has_green_dye", has(Items.DYE.green()))
                .unlockedBy("has_white_terracotta", has(Items.DYED_TERRACOTTA.white()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.MIST_ANBU_MASK.get())
                .define('W', Items.DYED_TERRACOTTA.white())
                .define('R', Items.DYE.red())
                .pattern("WWW")
                .pattern("WRW")
                .pattern(" W ")
                .unlockedBy("has_red_dye", has(Items.DYE.red()))
                .unlockedBy("has_white_terracotta", has(Items.DYED_TERRACOTTA.white()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.KUNAI.get(), 8)
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .pattern(" I")
                .pattern("S ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.SHURIKEN.get(), 16)
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FLINT)
                .pattern(" F ")
                .pattern("FIF")
                .pattern(" F ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .unlockedBy("has_flint", has(Items.FLINT))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoItems.SENBON.get(), 16)
                .define('N', Items.IRON_NUGGET)
                .pattern("  N")
                .pattern(" N ")
                .pattern("N  ")
                .unlockedBy("has_iron_nugget", has(Items.IRON_NUGGET))
                .save(this.output);

        this.shapeless(RecipeCategory.COMBAT, NarutoItems.EXPLOSIVE_KUNAI.get())
                .requires(NarutoItems.KUNAI.get())
                .requires(NarutoBlocks.ITEM_PAPER_BOMB.get())
                .unlockedBy("has_paper_bomb", has(NarutoBlocks.ITEM_PAPER_BOMB.get()))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, NarutoBlocks.ITEM_PAPER_BOMB.get())
                .define('G', Items.GUNPOWDER)
                .define('P', Items.PAPER)
                .pattern("GGG")
                .pattern("PPP")
                .pattern("GGG")
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(this.output);
    }
}
