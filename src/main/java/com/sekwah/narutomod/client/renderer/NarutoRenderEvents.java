package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.client.model.item.model.ShinobiArmorModel;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.FumaShurikenModel;
import com.sekwah.narutomod.client.model.jutsu.RasenshurikenModel;
import com.sekwah.narutomod.client.renderer.entity.*;
import com.sekwah.narutomod.client.model.item.model.AnbuMaskModel;
import com.sekwah.narutomod.client.model.item.model.HeadbandModel;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.FireballJutsuRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.RasenshurikenRenderer;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example of new entity render events
 * https://github.com/MinecraftForge/MinecraftForge/blob/1.17.x/src/test/java/net/minecraftforge/debug/client/rendering/EntityRendererEventsTest.java
 */
@Mod.EventBusSubscriber(value=Dist.CLIENT, modid=NarutoMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class NarutoRenderEvents {

    public static final BlockEntityWithoutLevelRenderer NARUTO_RENDERER = new NarutoRenderer();

    public static final ModelLayerLocation ANBU_MASK_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "anbu_mask"), "main");

    public static final ModelLayerLocation ANBU_MASK_WITHOUT_EARS_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "anbu_mask_without_ears"), "main");

    public static final ModelLayerLocation HEADBAND_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "headband"), "main");

    public static final ModelLayerLocation FIREBALL_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "fireball"), "main");

    public static final ModelLayerLocation RASENSHURIKEN_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "rasenshuriken"), "main");

    public static final ModelLayerLocation FUMA_SHURIKEN_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "fuma_shuriken"), "main");

    public static final ModelLayerLocation CHUNIN_VEST_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "chunin_vest"), "main");

    public static final ModelLayerLocation ANBU_ARMOR_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "anbu_armor"), "main");

    public static final ModelLayerLocation AKATSUKI_CLOAK_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "akatsuki_cloak"), "main");

    public static final ModelLayerLocation SUSANOO_RIBCAGE_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "susanoo_ribcage"), "main");

    public static final ModelLayerLocation KIRIN_DRAGON_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "kirin_dragon"), "main");
    public static final ModelLayerLocation WOOD_PRISON_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "wood_prison"), "main");
    public static final ModelLayerLocation BONE_FOREST_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "bone_forest"), "main");
    public static final ModelLayerLocation RASENGAN_SPHERE_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "rasengan_sphere"), "main");
    public static final ModelLayerLocation CHIDORI_BURST_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "chidori_burst"), "main");
    public static final ModelLayerLocation WATER_VORTEX_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "water_vortex"), "main");
    public static final ModelLayerLocation WIND_SHOCKWAVE_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "wind_shockwave"), "main");
    public static final ModelLayerLocation AMATERASU_FLAME_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "amaterasu_flame"), "main");
    public static final ModelLayerLocation SHADOW_BIND_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "shadow_bind"), "main");
    public static final ModelLayerLocation EARTH_WALL_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "earth_wall"), "main");
    public static final ModelLayerLocation EIGHT_GATES_AURA_LAYER =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "eight_gates_aura"), "main");

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NarutoEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EXPLOSIVE_KUNAI.get(), ExplosiveKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SENBON.get(), SenbonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHURIKEN.get(), ShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAPER_BOMB.get(), PaperBombRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SMOKE_BOMB.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        event.registerEntityRenderer(NarutoEntities.FIREBALL_JUTSU.get(), FireballJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENSHURIKEN.get(), RasenshurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_BULLET.get(), com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WaterBulletRenderer::new);
        event.registerEntityRenderer(NarutoEntities.MUD_WALL.get(), com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.MudWallRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WOOD_PRISON.get(), com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WoodPrisonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.JUTSU_VISUAL_EFFECT.get(), com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.JutsuVisualEffectRenderer::new);
        NarutoRenderer.registerEntityRenderers();

        event.registerEntityRenderer(NarutoEntities.SHADOW_CLONE.get(), com.sekwah.narutomod.client.renderer.entity.ShadowCloneRenderer::new);

        event.registerEntityRenderer(NarutoEntities.ROGUE_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "rogue_ninja"));
        event.registerEntityRenderer(NarutoEntities.AKATSUKI_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "akatsuki_ninja"));
    }

    public static void registerRenderTypes() {
        ItemBlockRenderTypes.setRenderLayer(NarutoBlocks.PAPER_BOMB.get(), RenderType.translucent());
    }

    @SubscribeEvent
    public static void reloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new NarutoRenderer());
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ANBU_MASK_LAYER, () -> AnbuMaskModel.createLayer(true));
        event.registerLayerDefinition(ANBU_MASK_WITHOUT_EARS_LAYER, () -> AnbuMaskModel.createLayer(false));
        event.registerLayerDefinition(HEADBAND_LAYER, HeadbandModel::createLayer);
        event.registerLayerDefinition(FIREBALL_LAYER, FireballJutsuModel::createLayer);
        event.registerLayerDefinition(RASENSHURIKEN_LAYER, RasenshurikenModel::createLayer);
        event.registerLayerDefinition(FUMA_SHURIKEN_LAYER, FumaShurikenModel::createLayer);
        event.registerLayerDefinition(CHUNIN_VEST_LAYER, ShinobiArmorModel::createChuninVestLayer);
        event.registerLayerDefinition(ANBU_ARMOR_LAYER, ShinobiArmorModel::createAnbuArmorLayer);
        event.registerLayerDefinition(AKATSUKI_CLOAK_LAYER, ShinobiArmorModel::createAkatsukiCloakLayer);
        event.registerLayerDefinition(SUSANOO_RIBCAGE_LAYER, com.sekwah.narutomod.client.model.entity.SusanooRibcageModel::createLayer);

        event.registerLayerDefinition(KIRIN_DRAGON_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.KirinDragonModel::createLayer);
        event.registerLayerDefinition(WOOD_PRISON_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WoodPrisonModel::createLayer);
        event.registerLayerDefinition(BONE_FOREST_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.BoneForestModel::createLayer);
        event.registerLayerDefinition(RASENGAN_SPHERE_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.RasenganSphereModel::createLayer);
        event.registerLayerDefinition(CHIDORI_BURST_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ChidoriBurstModel::createLayer);
        event.registerLayerDefinition(WATER_VORTEX_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WaterVortexModel::createLayer);
        event.registerLayerDefinition(WIND_SHOCKWAVE_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.WindShockwaveModel::createLayer);
        event.registerLayerDefinition(AMATERASU_FLAME_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.AmaterasuFlameModel::createLayer);
        event.registerLayerDefinition(SHADOW_BIND_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.ShadowBindModel::createLayer);
        event.registerLayerDefinition(EARTH_WALL_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EarthWallModel::createLayer);
        event.registerLayerDefinition(EIGHT_GATES_AURA_LAYER, com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.EightGatesAuraModel::createLayer);
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.AddLayers event)
    {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.layers.SusanooRibcageLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.layers.SharinganEyesLayer(renderer));
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.layers.JutsuHandAuraLayer(renderer, event.getEntityModels()));
            }
        }
    }
}
