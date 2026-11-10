package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class NarutoItemModelProvider extends ItemModelProvider {

    public NarutoItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, NarutoMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // --- Jutsu Scrolls ---
        simpleItemWithTexture("scroll_rasengan", "item/scrolls/scroll_rasengan");
        simpleItemWithTexture("scroll_chidori", "item/scrolls/scroll_chidori");
        simpleItemWithTexture("scroll_shadow_clone", "item/scrolls/scroll_shadow_clone");
        simpleItemWithTexture("scroll_kirin", "item/scrolls/scroll_kirin");
        simpleItemWithTexture("scroll_amaterasu", "item/scrolls/scroll_amaterasu");
        simpleItemWithTexture("scroll_susanoo", "item/scrolls/scroll_susanoo");
        simpleItemWithTexture("scroll_eight_gates", "item/scrolls/scroll_eight_gates");
        simpleItemWithTexture("scroll_rasenshuriken", "item/scrolls/scroll_rasenshuriken");
        simpleItemWithTexture("scroll_shadow_possession", "item/scrolls/scroll_shadow_possession");
        simpleItemWithTexture("jutsu_scroll", "item/scrolls/scroll_rasengan");

        // --- Basic Items & Weapons (2D items) ---
        simpleItemWithTexture("chakra_metal", "item/weapons/chakra_metal");
        simpleItemWithTexture("ichiraku_ramen", "item/weapons/ichiraku_ramen");
        simpleItemWithTexture("military_rations_pill", "item/weapons/military_rations_pill");
        simpleItemWithTexture("smoke_bomb", "item/weapons/smoke_bomb");
        simpleItemWithTexture("paper_bomb", "item/weapons/paperbomb");
        simpleItemWithTexture("lonely_march", "item/lonely_march");

        // --- Armor & Masks ---
        simpleItemWithTexture("chunin_vest", "item/armor/chunin_vest");
        simpleItemWithTexture("anbu_armor", "item/armor/anbu_armor");
        simpleItemWithTexture("akatsuki_cloak", "item/armor/akatsuki_cloak");
        simpleItemWithTexture("red_anbu_mask", "item/armor/red_anbu_mask");
        simpleItemWithTexture("yellow_anbu_mask", "item/armor/yellow_anbu_mask");
        simpleItemWithTexture("green_anbu_mask", "item/armor/green_anbu_mask");
        simpleItemWithTexture("blue_anbu_mask", "item/armor/blue_anbu_mask");
        simpleItemWithTexture("mist_anbu_mask", "item/armor/mist_anbu_mask");

        // --- Spawn Eggs ---
        withExistingParent("rogue_ninja_spawn_egg", mcLoc("item/template_spawn_egg"));
        withExistingParent("akatsuki_ninja_spawn_egg", mcLoc("item/template_spawn_egg"));

        // --- Block Items ---
        withExistingParent("bonsai_tree", modLoc("block/bonsai_tree"));

        // --- Headbands (Blank / Custom) ---
        simpleItemWithTexture("headband_black", "item/armor/headbands/headband_black_blank");
        simpleItemWithTexture("headband_blue", "item/armor/headbands/headband_blue_blank");
        simpleItemWithTexture("headband_red", "item/armor/headbands/headband_red_blank");
        simpleItemWithTexture("headband_custard", "item/armor/headbands/headband_custard");
        simpleItemWithTexture("headband_lava", "item/armor/headbands/headband_lava");
        simpleItemWithTexture("headband_pride", "item/armor/headbands/headband_rainbow");
        simpleItemWithTexture("headband_youtube", "item/armor/headbands/headband_youtube");

        // --- Headbands (Villages) ---
        simpleItemWithTexture("headband_leaf", "item/armor/headbands/headband_blue");
        simpleItemWithTexture("headband_leaf_black", "item/armor/headbands/headband_black");
        simpleItemWithTexture("headband_rock", "item/armor/headbands/headband_rock");
        simpleItemWithTexture("headband_sound", "item/armor/headbands/headband_sound");
        simpleItemWithTexture("headband_sand", "item/armor/headbands/headband_black_rev");
        simpleItemWithTexture("headband_cloud", "item/armor/headbands/headband_black_block");
        simpleItemWithTexture("headband_grass", "item/armor/headbands/headband_black_block");
        simpleItemWithTexture("headband_mist", "item/armor/headbands/headband_black_block");
        simpleItemWithTexture("headband_rain", "item/armor/headbands/headband_black_block");
        simpleItemWithTexture("headband_waterfall", "item/armor/headbands/headband_black_block");

        // --- Headbands (Scratched Variants) ---
        scratchedHeadband("headband_leaf_scratched", "headband_leaf");
        scratchedHeadband("headband_leaf_black_scratched", "headband_leaf_black");
        scratchedHeadband("headband_rock_scratched", "headband_rock");
        scratchedHeadband("headband_sand_scratched", "headband_sand");
        scratchedHeadband("headband_sound_scratched", "headband_sound");
        scratchedHeadband("headband_mist_scratched", "headband_mist");
        scratchedHeadband("headband_waterfall_scratched", "headband_waterfall");
        scratchedHeadband("headband_cloud_scratched", "headband_cloud");
        scratchedHeadband("headband_rain_scratched", "headband_rain");
        scratchedHeadband("headband_grass_scratched", "headband_grass");

        // NOTE: Hand-crafted 3D weapon models using forge:separate_transforms are intentionally
        // preserved in src/main/resources/assets/narutomod/models/item/ and NOT generated here:
        // - kunai, senbon, explosive_kunai, shuriken, fuma_shuriken, kusanagi_sword, samehada, bone_sword
    }

    private ItemModelBuilder simpleItemWithTexture(String name, String texturePath) {
        return withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc(texturePath));
    }

    private ItemModelBuilder scratchedHeadband(String name, String baseModelName) {
        return withExistingParent(name, modLoc("item/" + baseModelName));
    }
}
