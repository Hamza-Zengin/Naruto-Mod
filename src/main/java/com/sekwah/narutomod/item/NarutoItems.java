package com.sekwah.narutomod.item;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import com.sekwah.narutomod.item.armor.NarutoArmorMaterial;
import com.sekwah.narutomod.item.weapons.ExplosiveKunaiItem;
import com.sekwah.narutomod.item.weapons.KunaiItem;
import com.sekwah.narutomod.item.weapons.SenbonItem;
import com.sekwah.narutomod.item.weapons.ShurikenItem;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // Weapons
    public static final RegistryObject<Item> KUNAI = ITEMS.register("kunai", ()
            -> new KunaiItem(new Item.Properties()));

    public static final RegistryObject<Item> SENBON = ITEMS.register("senbon", ()
            -> new SenbonItem(new Item.Properties()));

    public static final RegistryObject<Item> EXPLOSIVE_KUNAI = ITEMS.register("explosive_kunai", ()
            -> new ExplosiveKunaiItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SHURIKEN = ITEMS.register("shuriken", ()
            -> new ShurikenItem(new Item.Properties()));

    public static final RegistryObject<Item> SMOKE_BOMB = ITEMS.register("smoke_bomb", ()
            -> new com.sekwah.narutomod.item.weapons.SmokeBombItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MILITARY_RATIONS_PILL = ITEMS.register("military_rations_pill", ()
            -> new MilitaryRationsPillItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SCROLL_RASENGAN = ITEMS.register("scroll_rasengan", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.RASENGAN.getId()));

    public static final RegistryObject<Item> SCROLL_CHIDORI = ITEMS.register("scroll_chidori", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.CHIDORI.getId()));

    public static final RegistryObject<Item> SCROLL_SHADOW_CLONE = ITEMS.register("scroll_shadow_clone", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.SHADOW_CLONE.getId()));

    public static final RegistryObject<Item> SCROLL_KIRIN = ITEMS.register("scroll_kirin", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.KIRIN.getId()));

    public static final RegistryObject<Item> SCROLL_AMATERASU = ITEMS.register("scroll_amaterasu", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.AMATERASU.getId(), "uchiha"));

    public static final RegistryObject<Item> SCROLL_SUSANOO = ITEMS.register("scroll_susanoo", ()
            -> new JutsuScrollItem(new Item.Properties().stacksTo(1), NarutoAbilities.SUSANOO_RIBCAGE.getId(), "uchiha"));

    // Armor
    public static final RegistryObject<Item> RED_ANBU_MASK = ITEMS.register("red_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> YELLOW_ANBU_MASK = ITEMS.register("yellow_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> GREEN_ANBU_MASK = ITEMS.register("green_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> BLUE_ANBU_MASK = ITEMS.register("blue_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> MIST_ANBU_MASK = ITEMS.register("mist_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> LONELY_MARCH = ITEMS.register("lonely_march", ()
            -> new RecordItem(14, NarutoSounds.LONELY_MARCH, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 41 * 20));

    // Ninja Headbands
    private static RegistryObject<Item> createHeadband(String itemName) {
        return ITEMS.register(itemName, ()
                -> new NarutoArmorItem(NarutoArmorMaterial.HEADBAND, ArmorItem.Type.HELMET, new Item.Properties()));
    }

    public static final RegistryObject<Item> HEADBAND_BLUE = createHeadband("headband_blue");
    public static final RegistryObject<Item> HEADBAND_BLACK = createHeadband("headband_black");
    public static final RegistryObject<Item> HEADBAND_RED = createHeadband("headband_red");
    public static final RegistryObject<Item> HEADBAND_CUSTARD = createHeadband("headband_custard");

    public static final RegistryObject<Item> HEADBAND_LEAF = createHeadband("headband_leaf");
    public static final RegistryObject<Item> HEADBAND_LEAF_SCRATCHED = createHeadband("headband_leaf_scratched");
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK = createHeadband("headband_leaf_black");
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK_SCRATCHED = createHeadband("headband_leaf_black_scratched");

    public static final RegistryObject<Item> HEADBAND_ROCK = createHeadband("headband_rock");
    public static final RegistryObject<Item> HEADBAND_ROCK_SCRATCHED = createHeadband("headband_rock_scratched");

    public static final RegistryObject<Item> HEADBAND_SAND = createHeadband("headband_sand");
    public static final RegistryObject<Item> HEADBAND_SAND_SCRATCHED = createHeadband("headband_sand_scratched");

    public static final RegistryObject<Item> HEADBAND_SOUND = createHeadband("headband_sound");
    public static final RegistryObject<Item> HEADBAND_SOUND_SCRATCHED = createHeadband("headband_sound_scratched");

    public static final RegistryObject<Item> HEADBAND_MIST = createHeadband("headband_mist");
    public static final RegistryObject<Item> HEADBAND_MIST_SCRATCHED = createHeadband("headband_mist_scratched");

    public static final RegistryObject<Item> HEADBAND_WATERFALL = createHeadband("headband_waterfall");
    public static final RegistryObject<Item> HEADBAND_WATERFALL_SCRATCHED = createHeadband("headband_waterfall_scratched");

    public static final RegistryObject<Item> HEADBAND_CLOUD = createHeadband("headband_cloud");
    public static final RegistryObject<Item> HEADBAND_CLOUD_SCRATCHED = createHeadband("headband_cloud_scratched");

    public static final RegistryObject<Item> HEADBAND_RAIN = createHeadband("headband_rain");
    public static final RegistryObject<Item> HEADBAND_RAIN_SCRATCHED = createHeadband("headband_rain_scratched");

    public static final RegistryObject<Item> HEADBAND_GRASS = createHeadband("headband_grass");
    public static final RegistryObject<Item> HEADBAND_GRASS_SCRATCHED = createHeadband("headband_grass_scratched");

    public static final RegistryObject<Item> HEADBAND_PRIDE = createHeadband("headband_pride");
    public static final RegistryObject<Item> HEADBAND_YOUTUBE = createHeadband("headband_youtube");
    public static final RegistryObject<Item> HEADBAND_LAVA = createHeadband("headband_lava");

    // Creative Tabs
    public static final RegistryObject<CreativeModeTab> NINJA_WEAPONS_TAB = CREATIVE_MODE_TABS.register("narutomod_weapons", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_weapons"))
                    .icon(() -> new ItemStack(KUNAI.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(KUNAI.get());
                        output.accept(SENBON.get());
                        output.accept(EXPLOSIVE_KUNAI.get());
                        output.accept(SHURIKEN.get());
                        output.accept(SMOKE_BOMB.get());
                        output.accept(MILITARY_RATIONS_PILL.get());
                        output.accept(SCROLL_RASENGAN.get());
                        output.accept(SCROLL_CHIDORI.get());
                        output.accept(SCROLL_SHADOW_CLONE.get());
                        output.accept(SCROLL_KIRIN.get());
                        output.accept(SCROLL_AMATERASU.get());
                        output.accept(SCROLL_SUSANOO.get());
                        output.accept(NarutoBlocks.ITEM_PAPER_BOMB.get());
                        output.accept(NarutoBlocks.ITEM_BONSAI_TREE.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> NINJA_ARMOR_TAB = CREATIVE_MODE_TABS.register("narutomod_armor", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_armor"))
                    .icon(() -> new ItemStack(RED_ANBU_MASK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RED_ANBU_MASK.get());
                        output.accept(YELLOW_ANBU_MASK.get());
                        output.accept(GREEN_ANBU_MASK.get());
                        output.accept(BLUE_ANBU_MASK.get());
                        output.accept(MIST_ANBU_MASK.get());
                        output.accept(LONELY_MARCH.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> HEADBANDS_TAB = CREATIVE_MODE_TABS.register("narutomod_headbands", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_headbands"))
                    .icon(() -> new ItemStack(HEADBAND_BLUE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HEADBAND_BLUE.get());
                        output.accept(HEADBAND_BLACK.get());
                        output.accept(HEADBAND_RED.get());
                        output.accept(HEADBAND_CUSTARD.get());
                        output.accept(HEADBAND_LEAF.get());
                        output.accept(HEADBAND_LEAF_SCRATCHED.get());
                        output.accept(HEADBAND_LEAF_BLACK.get());
                        output.accept(HEADBAND_LEAF_BLACK_SCRATCHED.get());
                        output.accept(HEADBAND_ROCK.get());
                        output.accept(HEADBAND_ROCK_SCRATCHED.get());
                        output.accept(HEADBAND_SAND.get());
                        output.accept(HEADBAND_SAND_SCRATCHED.get());
                        output.accept(HEADBAND_SOUND.get());
                        output.accept(HEADBAND_SOUND_SCRATCHED.get());
                        output.accept(HEADBAND_MIST.get());
                        output.accept(HEADBAND_MIST_SCRATCHED.get());
                        output.accept(HEADBAND_WATERFALL.get());
                        output.accept(HEADBAND_WATERFALL_SCRATCHED.get());
                        output.accept(HEADBAND_CLOUD.get());
                        output.accept(HEADBAND_CLOUD_SCRATCHED.get());
                        output.accept(HEADBAND_RAIN.get());
                        output.accept(HEADBAND_RAIN_SCRATCHED.get());
                        output.accept(HEADBAND_GRASS.get());
                        output.accept(HEADBAND_GRASS_SCRATCHED.get());
                        output.accept(HEADBAND_PRIDE.get());
                        output.accept(HEADBAND_YOUTUBE.get());
                        output.accept(HEADBAND_LAVA.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
