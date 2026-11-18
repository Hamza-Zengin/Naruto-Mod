package com.sekwah.narutomod.item;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import com.sekwah.narutomod.item.armor.NarutoArmorMaterial;
import com.sekwah.narutomod.item.weapons.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // Weapons
    public static final DeferredItem<Item> KUNAI = ITEMS.registerItem("kunai", KunaiItem::new);

    public static final DeferredItem<Item> SENBON = ITEMS.registerItem("senbon", SenbonItem::new);

    public static final DeferredItem<Item> EXPLOSIVE_KUNAI = ITEMS.registerItem("explosive_kunai",
            props -> new ExplosiveKunaiItem(props.stacksTo(16)));

    public static final DeferredItem<Item> SHURIKEN = ITEMS.registerItem("shuriken", ShurikenItem::new);

    public static final DeferredItem<Item> FUMA_SHURIKEN = ITEMS.registerItem("fuma_shuriken", FumaShurikenItem::new);

    public static final DeferredItem<Item> KUSANAGI_SWORD = ITEMS.registerItem("kusanagi_sword", KusanagiSwordItem::new);

    public static final DeferredItem<Item> SAMEHADA = ITEMS.registerItem("samehada", SamehadaItem::new);

    public static final DeferredItem<Item> KUBIKIRIBOCHO = ITEMS.registerItem("kubikiribocho", KubikiribochoItem::new);

    public static final DeferredItem<Item> BONE_SWORD = ITEMS.registerItem("bone_sword", BoneSwordItem::new);

    public static final DeferredItem<Item> CHAKRA_BLADES = ITEMS.registerItem("chakra_blades", ChakraBladesItem::new);

    public static final DeferredItem<Item> ANBU_TANTO = ITEMS.registerItem("anbu_tanto", AnbuTantoItem::new);

    public static final DeferredItem<Item> SMOKE_BOMB = ITEMS.registerItem("smoke_bomb",
            props -> new SmokeBombItem(props.stacksTo(16)));

    public static final DeferredItem<Item> MILITARY_RATIONS_PILL = ITEMS.registerItem("military_rations_pill",
            props -> new MilitaryRationsPillItem(props.stacksTo(16)));

    public static final DeferredItem<Item> ICHIRAKU_RAMEN = ITEMS.registerItem("ichiraku_ramen", IchirakuRamenItem::new);

    public static final DeferredItem<Item> CHAKRA_METAL = ITEMS.registerSimpleItem("chakra_metal",
            props -> props.rarity(Rarity.UNCOMMON));

    // Jutsu Scrolls
    public static final DeferredItem<Item> SCROLL_RASENGAN = ITEMS.registerItem("scroll_rasengan",
            props -> new JutsuScrollItem(props.stacksTo(1), NarutoAbilities.RASENGAN.getId()));

    public static final DeferredItem<Item> SCROLL_CHIDORI = ITEMS.registerItem("scroll_chidori",
            props -> new JutsuScrollItem(props.stacksTo(1), NarutoAbilities.CHIDORI.getId()));

    public static final DeferredItem<Item> SCROLL_SHADOW_CLONE = ITEMS.registerItem("scroll_shadow_clone",
            props -> new JutsuScrollItem(props.stacksTo(1), NarutoAbilities.SHADOW_CLONE.getId()));

    public static final DeferredItem<Item> SCROLL_KIRIN = ITEMS.registerItem("scroll_kirin",
            props -> new JutsuScrollItem(props.stacksTo(1), NarutoAbilities.KIRIN.getId()));

    public static final DeferredItem<Item> SCROLL_EIGHT_GATES = ITEMS.registerItem("scroll_eight_gates",
            props -> new JutsuScrollItem(props.stacksTo(1), NarutoAbilities.EIGHT_GATES.getId()));

    public static final DeferredItem<Item> SCROLL_RASENSHURIKEN = ITEMS.registerItem("scroll_rasenshuriken",
            props -> new JutsuScrollItem(props.stacksTo(1).rarity(Rarity.EPIC), NarutoAbilities.RASENSHURIKEN.getId()));

    public static final DeferredItem<Item> SCROLL_STONE_SPIKES = ITEMS.registerItem("scroll_stone_spikes",
            props -> new JutsuScrollItem(props.stacksTo(1).rarity(Rarity.RARE), NarutoAbilities.STONE_SPIKES.getId()));

    public static final DeferredItem<Item> SCROLL_WATER_DRAGON = ITEMS.registerItem("scroll_water_dragon",
            props -> new JutsuScrollItem(props.stacksTo(1).rarity(Rarity.RARE), NarutoAbilities.WATER_DRAGON.getId()));

    public static final DeferredItem<Item> SCROLL_CHIDORI_STREAM = ITEMS.registerItem("scroll_chidori_stream",
            props -> new JutsuScrollItem(props.stacksTo(1).rarity(Rarity.RARE), NarutoAbilities.CHIDORI_STREAM.getId()));

    public static final DeferredItem<Item> SEALED_AMMO_SCROLL = ITEMS.registerItem("sealed_ammo_scroll",
            SealedAmmoScrollItem::new);

    public static final DeferredItem<Item> BLANK_JUTSU_SCROLL = ITEMS.registerItem("blank_jutsu_scroll",
            props -> new BlankJutsuScrollItem(props.stacksTo(4).rarity(Rarity.UNCOMMON)));

    // Spawn Eggs
    public static final DeferredItem<Item> ROGUE_NINJA_SPAWN_EGG = ITEMS.registerItem("rogue_ninja_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(NarutoEntities.ROGUE_NINJA.get())));

    public static final DeferredItem<Item> AKATSUKI_NINJA_SPAWN_EGG = ITEMS.registerItem("akatsuki_ninja_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(NarutoEntities.AKATSUKI_NINJA.get())));

    public static final DeferredItem<Item> VILLAGE_NINJA_SPAWN_EGG = ITEMS.registerItem("village_ninja_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(NarutoEntities.VILLAGE_NINJA.get())));

    // Armor
    public static final DeferredItem<Item> RED_ANBU_MASK = ITEMS.registerItem("red_anbu_mask",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.HELMET, props)
                    .setShouldHideNameplate(true));

    public static final DeferredItem<Item> YELLOW_ANBU_MASK = ITEMS.registerItem("yellow_anbu_mask",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.HELMET, props)
                    .setShouldHideNameplate(true));

    public static final DeferredItem<Item> GREEN_ANBU_MASK = ITEMS.registerItem("green_anbu_mask",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.HELMET, props)
                    .setShouldHideNameplate(true));

    public static final DeferredItem<Item> BLUE_ANBU_MASK = ITEMS.registerItem("blue_anbu_mask",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.HELMET, props)
                    .setShouldHideNameplate(true));

    public static final DeferredItem<Item> MIST_ANBU_MASK = ITEMS.registerItem("mist_anbu_mask",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.HELMET, props)
                    .setShouldHideNameplate(true));

    public static final DeferredItem<Item> CHUNIN_VEST = ITEMS.registerItem("chunin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.CHUNIN_MAT, ArmorType.CHESTPLATE, props)
                    .setTooltipKey("item.narutomod.chunin_vest.desc"));

    public static final DeferredItem<Item> ANBU_ARMOR = ITEMS.registerItem("anbu_armor",
            props -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.CHESTPLATE, props)
                    .setTooltipKey("item.narutomod.anbu_armor.desc"));

    public static final DeferredItem<Item> AKATSUKI_CLOAK = ITEMS.registerItem("akatsuki_cloak",
            props -> new NarutoArmorItem(NarutoArmorMaterial.AKATSUKI_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.akatsuki_cloak.desc"));

    public static final DeferredItem<Item> JONIN_VEST = ITEMS.registerItem("jonin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.JONIN_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.jonin_vest.desc"));

    public static final DeferredItem<Item> SAND_JONIN_VEST = ITEMS.registerItem("sand_jonin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.JONIN_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.sand_jonin_vest.desc"));

    public static final DeferredItem<Item> MIST_JONIN_VEST = ITEMS.registerItem("mist_jonin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.JONIN_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.mist_jonin_vest.desc"));

    public static final DeferredItem<Item> CLOUD_JONIN_VEST = ITEMS.registerItem("cloud_jonin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.JONIN_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.cloud_jonin_vest.desc"));

    public static final DeferredItem<Item> ROCK_JONIN_VEST = ITEMS.registerItem("rock_jonin_vest",
            props -> new NarutoArmorItem(NarutoArmorMaterial.JONIN_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.RARE))
                    .setTooltipKey("item.narutomod.rock_jonin_vest.desc"));

    public static final DeferredItem<Item> SAGE_CLOAK = ITEMS.registerItem("sage_cloak",
            props -> new NarutoArmorItem(NarutoArmorMaterial.SAGE_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.sage_cloak.desc"));

    public static final DeferredItem<Item> WARRING_STATES_ARMOR = ITEMS.registerItem("warring_states_armor",
            props -> new NarutoArmorItem(NarutoArmorMaterial.WARRING_STATES_MAT, ArmorType.CHESTPLATE, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.warring_states_armor.desc"));

    public static final DeferredItem<Item> KAGE_HAT = ITEMS.registerItem("kage_hat",
            props -> new NarutoArmorItem(NarutoArmorMaterial.KAGE_MAT, ArmorType.HELMET, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.kage_hat.desc"));

    public static final DeferredItem<Item> KAZEKAGE_HAT = ITEMS.registerItem("kazekage_hat",
            props -> new NarutoArmorItem(NarutoArmorMaterial.KAGE_MAT, ArmorType.HELMET, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.kazekage_hat.desc"));

    public static final DeferredItem<Item> MIZUKAGE_HAT = ITEMS.registerItem("mizukage_hat",
            props -> new NarutoArmorItem(NarutoArmorMaterial.KAGE_MAT, ArmorType.HELMET, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.mizukage_hat.desc"));

    public static final DeferredItem<Item> RAIKAGE_HAT = ITEMS.registerItem("raikage_hat",
            props -> new NarutoArmorItem(NarutoArmorMaterial.KAGE_MAT, ArmorType.HELMET, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.raikage_hat.desc"));

    public static final DeferredItem<Item> TSUCHIKAGE_HAT = ITEMS.registerItem("tsuchikage_hat",
            props -> new NarutoArmorItem(NarutoArmorMaterial.KAGE_MAT, ArmorType.HELMET, props.rarity(Rarity.EPIC))
                    .setTooltipKey("item.narutomod.tsuchikage_hat.desc"));

    public static final DeferredItem<Item> LONELY_MARCH = ITEMS.registerSimpleItem("lonely_march",
            props -> props.stacksTo(1).rarity(Rarity.RARE)
                    .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(MOD_ID, "lonely_march"))));

    // Ninja Headbands
    private static DeferredItem<Item> createHeadband(String itemName) {
        return ITEMS.registerItem(itemName,
                props -> new NarutoArmorItem(NarutoArmorMaterial.HEADBAND, ArmorType.HELMET, props));
    }

    public static final DeferredItem<Item> HEADBAND_BLUE = createHeadband("headband_blue");
    public static final DeferredItem<Item> HEADBAND_BLACK = createHeadband("headband_black");
    public static final DeferredItem<Item> HEADBAND_RED = createHeadband("headband_red");

    public static final DeferredItem<Item> HEADBAND_LEAF = createHeadband("headband_leaf");
    public static final DeferredItem<Item> HEADBAND_LEAF_SCRATCHED = createHeadband("headband_leaf_scratched");
    public static final DeferredItem<Item> HEADBAND_LEAF_BLACK = createHeadband("headband_leaf_black");
    public static final DeferredItem<Item> HEADBAND_LEAF_BLACK_SCRATCHED = createHeadband("headband_leaf_black_scratched");

    public static final DeferredItem<Item> HEADBAND_ROCK = createHeadband("headband_rock");
    public static final DeferredItem<Item> HEADBAND_ROCK_SCRATCHED = createHeadband("headband_rock_scratched");

    public static final DeferredItem<Item> HEADBAND_SAND = createHeadband("headband_sand");
    public static final DeferredItem<Item> HEADBAND_SAND_SCRATCHED = createHeadband("headband_sand_scratched");

    public static final DeferredItem<Item> HEADBAND_SOUND = createHeadband("headband_sound");
    public static final DeferredItem<Item> HEADBAND_SOUND_SCRATCHED = createHeadband("headband_sound_scratched");

    public static final DeferredItem<Item> HEADBAND_MIST = createHeadband("headband_mist");
    public static final DeferredItem<Item> HEADBAND_MIST_SCRATCHED = createHeadband("headband_mist_scratched");

    public static final DeferredItem<Item> HEADBAND_WATERFALL = createHeadband("headband_waterfall");
    public static final DeferredItem<Item> HEADBAND_WATERFALL_SCRATCHED = createHeadband("headband_waterfall_scratched");

    public static final DeferredItem<Item> HEADBAND_CLOUD = createHeadband("headband_cloud");
    public static final DeferredItem<Item> HEADBAND_CLOUD_SCRATCHED = createHeadband("headband_cloud_scratched");

    public static final DeferredItem<Item> HEADBAND_RAIN = createHeadband("headband_rain");
    public static final DeferredItem<Item> HEADBAND_RAIN_SCRATCHED = createHeadband("headband_rain_scratched");

    public static final DeferredItem<Item> HEADBAND_GRASS = createHeadband("headband_grass");
    public static final DeferredItem<Item> HEADBAND_GRASS_SCRATCHED = createHeadband("headband_grass_scratched");

    public static final DeferredItem<Item> HEADBAND_YOUTUBE = createHeadband("headband_youtube");
    public static final DeferredItem<Item> HEADBAND_LAVA = createHeadband("headband_lava");

    // Creative Tabs
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NINJA_WEAPONS_TAB = CREATIVE_MODE_TABS.register("narutomod_weapons", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_weapons"))
                    .icon(() -> new ItemStack(KUSANAGI_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(KUNAI.get());
                        output.accept(SENBON.get());
                        output.accept(EXPLOSIVE_KUNAI.get());
                        output.accept(SHURIKEN.get());
                        output.accept(FUMA_SHURIKEN.get());
                        output.accept(KUSANAGI_SWORD.get());
                        output.accept(SAMEHADA.get());
                        output.accept(KUBIKIRIBOCHO.get());
                        output.accept(BONE_SWORD.get());
                        output.accept(CHAKRA_BLADES.get());
                        output.accept(ANBU_TANTO.get());
                        output.accept(SMOKE_BOMB.get());
                        output.accept(MILITARY_RATIONS_PILL.get());
                        output.accept(ICHIRAKU_RAMEN.get());
                        output.accept(CHAKRA_METAL.get());
                        output.accept(SCROLL_RASENGAN.get());
                        output.accept(SCROLL_CHIDORI.get());
                        output.accept(SCROLL_SHADOW_CLONE.get());
                        output.accept(SCROLL_KIRIN.get());
                        output.accept(SCROLL_EIGHT_GATES.get());
                        output.accept(SCROLL_RASENSHURIKEN.get());
                        output.accept(SCROLL_STONE_SPIKES.get());
                        output.accept(SCROLL_WATER_DRAGON.get());
                        output.accept(SCROLL_CHIDORI_STREAM.get());
                        output.accept(SEALED_AMMO_SCROLL.get());
                        output.accept(BLANK_JUTSU_SCROLL.get());
                        output.accept(ROGUE_NINJA_SPAWN_EGG.get());
                        output.accept(AKATSUKI_NINJA_SPAWN_EGG.get());
                        output.accept(VILLAGE_NINJA_SPAWN_EGG.get());
                        output.accept(NarutoBlocks.ITEM_PAPER_BOMB.get());
                        output.accept(NarutoBlocks.ITEM_BONSAI_TREE.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NINJA_ARMOR_TAB = CREATIVE_MODE_TABS.register("narutomod_armor", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_armor"))
                    .icon(() -> new ItemStack(RED_ANBU_MASK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(CHUNIN_VEST.get());
                        output.accept(JONIN_VEST.get());
                        output.accept(SAND_JONIN_VEST.get());
                        output.accept(MIST_JONIN_VEST.get());
                        output.accept(CLOUD_JONIN_VEST.get());
                        output.accept(ROCK_JONIN_VEST.get());
                        output.accept(ANBU_ARMOR.get());
                        output.accept(AKATSUKI_CLOAK.get());
                        output.accept(SAGE_CLOAK.get());
                        output.accept(WARRING_STATES_ARMOR.get());
                        output.accept(KAGE_HAT.get());
                        output.accept(KAZEKAGE_HAT.get());
                        output.accept(MIZUKAGE_HAT.get());
                        output.accept(RAIKAGE_HAT.get());
                        output.accept(TSUCHIKAGE_HAT.get());
                        output.accept(RED_ANBU_MASK.get());
                        output.accept(YELLOW_ANBU_MASK.get());
                        output.accept(GREEN_ANBU_MASK.get());
                        output.accept(BLUE_ANBU_MASK.get());
                        output.accept(MIST_ANBU_MASK.get());
                        output.accept(LONELY_MARCH.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HEADBANDS_TAB = CREATIVE_MODE_TABS.register("narutomod_headbands", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.narutomod_headbands"))
                    .icon(() -> new ItemStack(HEADBAND_BLUE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HEADBAND_BLUE.get());
                        output.accept(HEADBAND_BLACK.get());
                        output.accept(HEADBAND_RED.get());
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
                        output.accept(HEADBAND_YOUTUBE.get());
                        output.accept(HEADBAND_LAVA.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
