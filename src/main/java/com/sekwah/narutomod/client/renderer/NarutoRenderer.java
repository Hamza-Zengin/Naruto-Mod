package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.client.model.item.model.AnbuMaskModel;
import com.sekwah.narutomod.client.model.item.model.HeadbandModel;
import com.sekwah.narutomod.client.model.item.model.ShinobiArmorModel;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class NarutoRenderer implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();

        ShinobiArmorModel joninVestModel = new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.JONIN_VEST_LAYER));
        com.sekwah.narutomod.client.model.item.model.KageHatModel kageHatModel =
                new com.sekwah.narutomod.client.model.item.model.KageHatModel(modelSet.bakeLayer(NarutoRenderEvents.KAGE_HAT_LAYER));
        AnbuMaskModel anbuMaskModel = new AnbuMaskModel(modelSet.bakeLayer(NarutoRenderEvents.ANBU_MASK_LAYER));
        AnbuMaskModel anbuMaskWithoutEarsModel = new AnbuMaskModel(modelSet.bakeLayer(NarutoRenderEvents.ANBU_MASK_WITHOUT_EARS_LAYER));
        HeadbandModel headbandModel = new HeadbandModel(modelSet.bakeLayer(NarutoRenderEvents.HEADBAND_LAYER));

        ((NarutoArmorItem) NarutoItems.CHUNIN_VEST.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.CHUNIN_VEST_LAYER)))
                .setArmorTexture("textures/models/armor/chunin_vest.png");
        ((NarutoArmorItem) NarutoItems.ANBU_ARMOR.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.ANBU_ARMOR_LAYER)))
                .setArmorTexture("textures/models/armor/anbu_armor.png");
        ((NarutoArmorItem) NarutoItems.AKATSUKI_CLOAK.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.AKATSUKI_CLOAK_LAYER)))
                .setArmorTexture("textures/models/armor/akatsuki_cloak.png");
        ((NarutoArmorItem) NarutoItems.JONIN_VEST.get())
                .setArmorModel(joninVestModel)
                .setArmorTexture("textures/models/armor/jonin_vest.png");
        ((NarutoArmorItem) NarutoItems.WARRING_STATES_ARMOR.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.WARRING_STATES_ARMOR_LAYER)))
                .setArmorTexture("textures/models/armor/warring_states_armor.png");
        ((NarutoArmorItem) NarutoItems.KAGE_HAT.get())
                .setArmorModel(kageHatModel)
                .setArmorTexture("textures/models/armor/kage_hat.png");
        ((NarutoArmorItem) NarutoItems.KAZEKAGE_HAT.get())
                .setArmorModel(kageHatModel)
                .setArmorTexture("textures/models/armor/kazekage_hat.png");
        ((NarutoArmorItem) NarutoItems.MIZUKAGE_HAT.get())
                .setArmorModel(kageHatModel)
                .setArmorTexture("textures/models/armor/mizukage_hat.png");
        ((NarutoArmorItem) NarutoItems.RAIKAGE_HAT.get())
                .setArmorModel(kageHatModel)
                .setArmorTexture("textures/models/armor/raikage_hat.png");
        ((NarutoArmorItem) NarutoItems.TSUCHIKAGE_HAT.get())
                .setArmorModel(kageHatModel)
                .setArmorTexture("textures/models/armor/tsuchikage_hat.png");

        ((NarutoArmorItem) NarutoItems.SAND_JONIN_VEST.get())
                .setArmorModel(joninVestModel)
                .setArmorTexture("textures/models/armor/sand_jonin_vest.png");
        ((NarutoArmorItem) NarutoItems.MIST_JONIN_VEST.get())
                .setArmorModel(joninVestModel)
                .setArmorTexture("textures/models/armor/mist_jonin_vest.png");
        ((NarutoArmorItem) NarutoItems.CLOUD_JONIN_VEST.get())
                .setArmorModel(joninVestModel)
                .setArmorTexture("textures/models/armor/cloud_jonin_vest.png");
        ((NarutoArmorItem) NarutoItems.ROCK_JONIN_VEST.get())
                .setArmorModel(joninVestModel)
                .setArmorTexture("textures/models/armor/rock_jonin_vest.png");

        ((NarutoArmorItem) NarutoItems.SAGE_CLOAK.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.SAGE_CLOAK_LAYER)))
                .setArmorTexture("textures/models/armor/sage_cloak.png");

        ((NarutoArmorItem) NarutoItems.SHINOBI_LEGGINGS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.SHINOBI_LEGGINGS_LAYER)))
                .setArmorTexture("textures/models/armor/shinobi_leggings.png");
        ((NarutoArmorItem) NarutoItems.SHINOBI_SANDALS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.SHINOBI_SANDALS_LAYER)))
                .setArmorTexture("textures/models/armor/shinobi_sandals.png");
        ((NarutoArmorItem) NarutoItems.ANBU_LEGGINGS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.ANBU_LEGGINGS_LAYER)))
                .setArmorTexture("textures/models/armor/anbu_leggings.png");
        ((NarutoArmorItem) NarutoItems.ANBU_BOOTS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.ANBU_BOOTS_LAYER)))
                .setArmorTexture("textures/models/armor/anbu_boots.png");
        ((NarutoArmorItem) NarutoItems.WARRING_STATES_LEGGINGS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.WARRING_STATES_LEGGINGS_LAYER)))
                .setArmorTexture("textures/models/armor/warring_states_leggings.png");
        ((NarutoArmorItem) NarutoItems.WARRING_STATES_BOOTS.get())
                .setArmorModel(new ShinobiArmorModel(modelSet.bakeLayer(NarutoRenderEvents.WARRING_STATES_BOOTS_LAYER)))
                .setArmorTexture("textures/models/armor/warring_states_boots.png");

        ((NarutoArmorItem) NarutoItems.RED_ANBU_MASK.get())
                .setArmorModel(anbuMaskModel)
                .setArmorTexture("textures/models/armor/red_anbu_mask.png");
        ((NarutoArmorItem) NarutoItems.BLUE_ANBU_MASK.get())
                .setArmorModel(anbuMaskModel)
                .setArmorTexture("textures/models/armor/blue_anbu_mask.png");
        ((NarutoArmorItem) NarutoItems.GREEN_ANBU_MASK.get())
                .setArmorModel(anbuMaskWithoutEarsModel)
                .setArmorTexture("textures/models/armor/green_anbu_mask.png");
        ((NarutoArmorItem) NarutoItems.MIST_ANBU_MASK.get())
                .setArmorModel(anbuMaskWithoutEarsModel)
                .setArmorTexture("textures/models/armor/mist_anbu_mask.png");
        ((NarutoArmorItem) NarutoItems.YELLOW_ANBU_MASK.get())
                .setArmorModel(anbuMaskWithoutEarsModel)
                .setArmorTexture("textures/models/armor/yellow_anbu_mask.png");

        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_BLUE, "textures/models/armor/headband/headband_blue_blank.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_BLACK, "textures/models/armor/headband/headband_black_blank.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_RED, "textures/models/armor/headband/headband_red_blank.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_LEAF, "textures/models/armor/headband/headband_leafvillage.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_LEAF_SCRATCHED, "textures/models/armor/headband/headband_leafvillage_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_LEAF_BLACK, "textures/models/armor/headband/headband_leafvillage_black.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_LEAF_BLACK_SCRATCHED, "textures/models/armor/headband/headband_leafvillage_black_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_ROCK, "textures/models/armor/headband/headband_rock.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_ROCK_SCRATCHED, "textures/models/armor/headband/headband_rock_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_SAND, "textures/models/armor/headband/headband_sandblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_SAND_SCRATCHED, "textures/models/armor/headband/headband_sandblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_SOUND, "textures/models/armor/headband/headband_soundvillage.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_SOUND_SCRATCHED, "textures/models/armor/headband/headband_soundvillage_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_MIST, "textures/models/armor/headband/headband_mistblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_MIST_SCRATCHED, "textures/models/armor/headband/headband_mistblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_WATERFALL, "textures/models/armor/headband/headband_waterfallblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_WATERFALL_SCRATCHED, "textures/models/armor/headband/headband_waterfallblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_CLOUD, "textures/models/armor/headband/headband_cloudblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_CLOUD_SCRATCHED, "textures/models/armor/headband/headband_cloudblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_RAIN, "textures/models/armor/headband/headband_rainblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_RAIN_SCRATCHED, "textures/models/armor/headband/headband_rainblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_GRASS, "textures/models/armor/headband/headband_grassblack.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_GRASS_SCRATCHED, "textures/models/armor/headband/headband_grassblack_scratched.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_YOUTUBE, "textures/models/armor/headband/headband_youtube.png");
        setHeadbandRenderer(headbandModel, NarutoItems.HEADBAND_LAVA, "textures/models/armor/headband/headband_lavavillage.png");

        FirstPersonJutsuRenderer.resetModels();
        FirstPersonWorldRenderer.resetModels();
    }

    private void setHeadbandRenderer(HeadbandModel model, Supplier<? extends Item> item, String texture) {
        ((NarutoArmorItem) item.get())
                .setArmorModel(model)
                .setArmorTexture(texture);
    }
}
