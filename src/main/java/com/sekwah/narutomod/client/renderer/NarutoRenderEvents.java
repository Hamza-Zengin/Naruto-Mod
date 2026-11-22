package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.block.NarutoBlockEntities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.model.item.model.AnbuMaskModel;
import com.sekwah.narutomod.client.model.item.model.HeadbandModel;
import com.sekwah.narutomod.client.model.item.model.ShinobiArmorModel;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.FumaShurikenModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels;
import com.sekwah.narutomod.client.model.jutsu.RasenshurikenModel;
import com.sekwah.narutomod.client.renderer.blockentity.WeaponRackRenderer;
import com.sekwah.narutomod.client.renderer.entity.*;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.*;
import com.sekwah.narutomod.client.renderer.entity.layers.JutsuHandAuraLayer;
import com.sekwah.narutomod.client.renderer.entity.layers.SharinganEyesLayer;
import com.sekwah.narutomod.client.renderer.entity.layers.SusanooRibcageLayer;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID)
public class NarutoRenderEvents {

    public static final ContextKey<NinjaRenderStateData> NINJA_RENDER_DATA_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ninja_render_data"));

    public record NinjaRenderStateData(
            boolean hasSusanooRibcage,
            boolean hasSharingan,
            Identifier eyeTexture,
            boolean hasByakugan,
            String activeHandJutsu,
            boolean hasEightGates,
            boolean hasEightGatesDeath,
            boolean hasWaterWalk
    ) {
        public static NinjaRenderStateData fromPlayer(Player player) {
            INinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
            boolean hasSusanoo = toggleData != null
                    && toggleData.hasToggleAbility(NarutoAbilities.SUSANOO_RIBCAGE.getId());
            boolean hasSharingan = ninjaData.isSharinganActive();
            boolean hasByakugan = ninjaData.isByakuganActive();
            Identifier eyeTexture = hasSharingan
                    ? SharinganAbility.getStage(ninjaData).getTexture()
                    : null;
            String activeHand = ninjaData.getActiveHandJutsu();
            boolean hasEightGatesDeath = ninjaData.isEighthGateActive()
                    || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.EIGHT_GATES_DEATH.getId()));
            boolean hasEightGates = ninjaData.isEightGatesActive() || hasEightGatesDeath
                    || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.EIGHT_GATES.getId()));
            boolean hasWaterWalk = ninjaData.isWaterWalkActive()
                    || (toggleData != null && toggleData.hasToggleAbility(NarutoAbilities.WATER_WALK.getId()));
            return new NinjaRenderStateData(
                    hasSusanoo,
                    hasSharingan,
                    eyeTexture,
                    hasByakugan,
                    activeHand == null ? "" : activeHand,
                    hasEightGates,
                    hasEightGatesDeath,
                    hasWaterWalk
            );
        }
    }

    public static final ModelLayerLocation ANBU_MASK_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anbu_mask"), "main");

    public static final ModelLayerLocation ANBU_MASK_WITHOUT_EARS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anbu_mask_without_ears"), "main");

    public static final ModelLayerLocation HEADBAND_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "headband"), "main");

    public static final ModelLayerLocation FIREBALL_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball"), "main");

    public static final ModelLayerLocation RASENSHURIKEN_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "rasenshuriken"), "main");

    public static final ModelLayerLocation FUMA_SHURIKEN_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fuma_shuriken"), "main");

    public static final ModelLayerLocation CHUNIN_VEST_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chunin_vest"), "main");

    public static final ModelLayerLocation ANBU_ARMOR_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anbu_armor"), "main");

    public static final ModelLayerLocation AKATSUKI_CLOAK_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "akatsuki_cloak"), "main");

    public static final ModelLayerLocation JONIN_VEST_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "jonin_vest"), "main");

    public static final ModelLayerLocation WARRING_STATES_ARMOR_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "warring_states_armor"), "main");

    public static final ModelLayerLocation KAGE_HAT_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "kage_hat"), "main");

    public static final ModelLayerLocation SAGE_CLOAK_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sage_cloak"), "main");

    public static final ModelLayerLocation SHINOBI_LEGGINGS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi_leggings"), "main");
    public static final ModelLayerLocation SHINOBI_SANDALS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi_sandals"), "main");
    public static final ModelLayerLocation ANBU_LEGGINGS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anbu_leggings"), "main");
    public static final ModelLayerLocation ANBU_BOOTS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anbu_boots"), "main");
    public static final ModelLayerLocation WARRING_STATES_LEGGINGS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "warring_states_leggings"), "main");
    public static final ModelLayerLocation WARRING_STATES_BOOTS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "warring_states_boots"), "main");

    public static final ModelLayerLocation SUSANOO_RIBCAGE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "susanoo_ribcage"), "main");

    public static final ModelLayerLocation KIRIN_DRAGON_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "kirin_dragon"), "main");
    public static final ModelLayerLocation WOOD_PRISON_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "wood_prison"), "main");
    public static final ModelLayerLocation BONE_FOREST_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "bone_forest"), "main");
    public static final ModelLayerLocation RASENGAN_SPHERE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "rasengan_sphere"), "main");
    public static final ModelLayerLocation CHIDORI_BURST_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chidori_burst"), "main");
    public static final ModelLayerLocation WATER_VORTEX_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_vortex"), "main");
    public static final ModelLayerLocation WIND_SHOCKWAVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "wind_shockwave"), "main");
    public static final ModelLayerLocation AMATERASU_FLAME_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "amaterasu_flame"), "main");
    public static final ModelLayerLocation SHADOW_BIND_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shadow_bind"), "main");
    public static final ModelLayerLocation EARTH_WALL_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "earth_wall"), "main");
    public static final ModelLayerLocation STONE_SPIKES_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "stone_spikes"), "main");
    public static final ModelLayerLocation ADAMANTINE_CHAINS_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "adamantine_chains"), "main");
    public static final ModelLayerLocation EIGHT_GATES_AURA_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "eight_gates_aura"), "main");
    public static final ModelLayerLocation ASH_CLOUD_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ash_cloud"), "main");
    public static final ModelLayerLocation WATER_WALL_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_wall"), "main");
    public static final ModelLayerLocation CHAKRA_SCALPEL_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_scalpel"), "main");

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NarutoEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EXPLOSIVE_KUNAI.get(), ExplosiveKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SENBON.get(), SenbonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHURIKEN.get(), ShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAPER_BOMB.get(), PaperBombRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SMOKE_BOMB.get(), SmokeBombRenderer::new);

        event.registerEntityRenderer(NarutoEntities.FIREBALL_JUTSU.get(), FireballJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENSHURIKEN.get(), RasenshurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_BULLET.get(), WaterBulletRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EARTH_BULLET.get(), EarthBulletRenderer::new);
        event.registerEntityRenderer(NarutoEntities.VACUUM_SPHERE.get(), VacuumSphereRenderer::new);
        event.registerEntityRenderer(NarutoEntities.MUD_WALL.get(), MudWallRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WOOD_PRISON.get(), WoodPrisonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.JUTSU_VISUAL_EFFECT.get(), JutsuVisualEffectRenderer::new);

        event.registerEntityRenderer(NarutoEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);

        event.registerEntityRenderer(NarutoEntities.ROGUE_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "rogue_ninja"));
        event.registerEntityRenderer(NarutoEntities.AKATSUKI_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "akatsuki_ninja"));
        event.registerEntityRenderer(NarutoEntities.CHUNIN_CHAMPION.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "konoha_ninja"));
        event.registerEntityRenderer(NarutoEntities.VILLAGE_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "village_ninja"));
        event.registerEntityRenderer(NarutoEntities.KONOHA_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "konoha_ninja"));
        event.registerEntityRenderer(NarutoEntities.SUNA_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "suna_ninja"));
        event.registerEntityRenderer(NarutoEntities.KIRI_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "kiri_ninja"));
        event.registerEntityRenderer(NarutoEntities.KUMO_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "kumo_ninja"));
        event.registerEntityRenderer(NarutoEntities.IWA_NINJA.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "iwa_ninja"));
        event.registerEntityRenderer(NarutoEntities.KONOHA_VILLAGER.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "konoha_villager"));
        event.registerEntityRenderer(NarutoEntities.SUNA_VILLAGER.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "suna_villager"));
        event.registerEntityRenderer(NarutoEntities.KIRI_VILLAGER.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "kiri_villager"));
        event.registerEntityRenderer(NarutoEntities.KUMO_VILLAGER.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "kumo_villager"));
        event.registerEntityRenderer(NarutoEntities.IWA_VILLAGER.get(), (ctx) -> new ShinobiMobRenderer<>(ctx, "iwa_villager"));

        event.registerBlockEntityRenderer(NarutoBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
    }

    @SubscribeEvent
    public static void reloadListener(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "armor_models"), new NarutoRenderer());
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ANBU_MASK_LAYER, () -> AnbuMaskModel.createLayer(true));
        event.registerLayerDefinition(ANBU_MASK_WITHOUT_EARS_LAYER, () -> AnbuMaskModel.createLayer(false));
        event.registerLayerDefinition(HEADBAND_LAYER, HeadbandModel::createLayer);
        event.registerLayerDefinition(FIREBALL_LAYER, FireballJutsuModel::createLayer);
        event.registerLayerDefinition(RASENSHURIKEN_LAYER, RasenshurikenModel::createLayer);
        event.registerLayerDefinition(FUMA_SHURIKEN_LAYER, FumaShurikenModel::createLayer);
        event.registerLayerDefinition(CHUNIN_VEST_LAYER, ShinobiArmorModel::createChuninVestLayer);
        event.registerLayerDefinition(ANBU_ARMOR_LAYER, ShinobiArmorModel::createAnbuArmorLayer);
        event.registerLayerDefinition(AKATSUKI_CLOAK_LAYER, ShinobiArmorModel::createAkatsukiCloakLayer);
        event.registerLayerDefinition(JONIN_VEST_LAYER, ShinobiArmorModel::createJoninVestLayer);
        event.registerLayerDefinition(WARRING_STATES_ARMOR_LAYER, ShinobiArmorModel::createWarringStatesArmorLayer);
        event.registerLayerDefinition(KAGE_HAT_LAYER, com.sekwah.narutomod.client.model.item.model.KageHatModel::createLayer);
        event.registerLayerDefinition(SAGE_CLOAK_LAYER, ShinobiArmorModel::createSageCloakLayer);
        event.registerLayerDefinition(SHINOBI_LEGGINGS_LAYER, ShinobiArmorModel::createShinobiLeggingsLayer);
        event.registerLayerDefinition(SHINOBI_SANDALS_LAYER, ShinobiArmorModel::createShinobiSandalsLayer);
        event.registerLayerDefinition(ANBU_LEGGINGS_LAYER, ShinobiArmorModel::createAnbuLeggingsLayer);
        event.registerLayerDefinition(ANBU_BOOTS_LAYER, ShinobiArmorModel::createAnbuBootsLayer);
        event.registerLayerDefinition(WARRING_STATES_LEGGINGS_LAYER, ShinobiArmorModel::createWarringStatesLeggingsLayer);
        event.registerLayerDefinition(WARRING_STATES_BOOTS_LAYER, ShinobiArmorModel::createWarringStatesBootsLayer);
        event.registerLayerDefinition(SUSANOO_RIBCAGE_LAYER, SusanooRibcageModel::createLayer);

        event.registerLayerDefinition(KIRIN_DRAGON_LAYER, JutsuVisualModels.KirinDragonModel::createLayer);
        event.registerLayerDefinition(WOOD_PRISON_LAYER, JutsuVisualModels.WoodPrisonModel::createLayer);
        event.registerLayerDefinition(BONE_FOREST_LAYER, JutsuVisualModels.BoneForestModel::createLayer);
        event.registerLayerDefinition(RASENGAN_SPHERE_LAYER, JutsuVisualModels.RasenganSphereModel::createLayer);
        event.registerLayerDefinition(CHIDORI_BURST_LAYER, JutsuVisualModels.ChidoriBurstModel::createLayer);
        event.registerLayerDefinition(WATER_VORTEX_LAYER, JutsuVisualModels.WaterVortexModel::createLayer);
        event.registerLayerDefinition(WIND_SHOCKWAVE_LAYER, JutsuVisualModels.WindShockwaveModel::createLayer);
        event.registerLayerDefinition(AMATERASU_FLAME_LAYER, JutsuVisualModels.AmaterasuFlameModel::createLayer);
        event.registerLayerDefinition(SHADOW_BIND_LAYER, JutsuVisualModels.ShadowBindModel::createLayer);
        event.registerLayerDefinition(EARTH_WALL_LAYER, JutsuVisualModels.EarthWallModel::createLayer);
        event.registerLayerDefinition(STONE_SPIKES_LAYER, JutsuVisualModels.StoneSpikesModel::createLayer);
        event.registerLayerDefinition(ADAMANTINE_CHAINS_LAYER, JutsuVisualModels.AdamantineChainsModel::createLayer);
        event.registerLayerDefinition(EIGHT_GATES_AURA_LAYER, JutsuVisualModels.EightGatesAuraModel::createLayer);
        event.registerLayerDefinition(ASH_CLOUD_LAYER, JutsuVisualModels.AshCloudModel::createLayer);
        event.registerLayerDefinition(WATER_WALL_LAYER, JutsuVisualModels.WaterWallModel::createLayer);
        event.registerLayerDefinition(CHAKRA_SCALPEL_LAYER, JutsuVisualModels.ChakraScalpelModel::createLayer);
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new SusanooRibcageLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new SharinganEyesLayer(renderer));
                renderer.addLayer(new JutsuHandAuraLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
                if (entity instanceof Player player) {
                    state.setRenderData(NINJA_RENDER_DATA_KEY, NinjaRenderStateData.fromPlayer(player));
                }
            }
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions armorExtensions = new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
                if (itemStack.getItem() instanceof NarutoArmorItem armorItem && armorItem.getArmorModel() instanceof HumanoidModel<?> armorModel) {
                    return armorModel;
                }
                return IClientItemExtensions.super.getHumanoidArmorModel(itemStack, layerType, original);
            }

            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
                if (stack.getItem() instanceof NarutoArmorItem armorItem && armorItem.getArmorTexture() != null) {
                    return armorItem.getArmorTexture();
                }
                return defaultTexture;
            }
        };

        Item[] armorItems = NarutoItems.ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(NarutoArmorItem.class::isInstance)
                .toArray(Item[]::new);
        if (armorItems.length > 0) {
            event.registerItem(armorExtensions, armorItems);
        }
    }
}
