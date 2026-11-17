package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.client.renderer.entity.layers.JutsuHandAuraLayer;
import com.sekwah.narutomod.client.renderer.entity.layers.SharinganEyesLayer;
import com.sekwah.narutomod.client.renderer.entity.layers.SusanooRibcageLayer;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/**
 * Renders Shadow Clones with the owner player's custom skin, Steve (wide) or Alex (slim) model,
 * and equipped armor, held items, Sharingan/Byakugan eyes, Susanoo ribcage, and hand/aura effects.
 */
public class ShadowCloneRenderer extends HumanoidMobRenderer<ShadowCloneEntity, AvatarRenderState, PlayerModel> {

    private final PlayerModel defaultModel;
    private final PlayerModel slimModel;

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.defaultModel = this.getModel();
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()
        ));
        this.addLayer(new SharinganEyesLayer(this));
        this.addLayer(new SusanooRibcageLayer(this, context.getModelSet()));
        this.addLayer(new JutsuHandAuraLayer(this, context.getModelSet()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(ShadowCloneEntity entity, AvatarRenderState state, float partialTick) {
        if (entity == null || state == null) {
            return;
        }
        super.extractRenderState(entity, state, partialTick);
        state.skin = resolveSkin(entity);

        boolean sharingan = entity.hasActiveAbility("sharingan") || entity.getSharinganStage() > 0;
        boolean byakugan = entity.hasActiveAbility("byakugan");
        Identifier eyeTexture = null;
        if (sharingan) {
            SharinganAbility.SharinganStage[] stages = SharinganAbility.SharinganStage.values();
            int stageIdx = Math.max(0, Math.min(stages.length - 1, entity.getSharinganStage() - 1));
            eyeTexture = stages[stageIdx].getTexture();
        }
        boolean susanoo = entity.hasActiveAbility("susanoo_ribcage");
        String activeHandJutsu = entity.getActiveHandJutsu();
        boolean hasEightGatesDeath = entity.hasActiveAbility("eight_gates_death") || entity.getHachimonGate() >= 8;
        boolean hasEightGates = entity.hasActiveAbility("eight_gates") || hasEightGatesDeath || entity.getHachimonGate() > 0;
        boolean hasWaterWalk = entity.hasActiveAbility("water_walk");

        state.setRenderData(
                NarutoRenderEvents.NINJA_RENDER_DATA_KEY,
                new NarutoRenderEvents.NinjaRenderStateData(
                        susanoo,
                        sharingan,
                        eyeTexture,
                        byakugan,
                        activeHandJutsu != null ? activeHandJutsu : "",
                        hasEightGates,
                        hasEightGatesDeath,
                        hasWaterWalk
                )
        );
    }

    private PlayerSkin resolveSkin(ShadowCloneEntity entity) {
        if (entity == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID != null) {
            if (entity.level() != null) {
                Player p = entity.level().getPlayerByUUID(ownerUUID);
                if (p instanceof ClientAvatarEntity avatar && avatar.getSkin() != null) {
                    return avatar.getSkin();
                }
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(ownerUUID);
                if (info != null && info.getSkin() != null) {
                    return info.getSkin();
                }
            }
            PlayerSkin defaultSkin = DefaultPlayerSkin.get(ownerUUID);
            if (defaultSkin != null && entity.isSlim() && defaultSkin.model() != PlayerModelType.SLIM) {
                return new PlayerSkin(defaultSkin.body(), defaultSkin.cape(), defaultSkin.elytra(), PlayerModelType.SLIM, defaultSkin.secure());
            }
            if (defaultSkin != null) {
                return defaultSkin;
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state == null) {
            return;
        }
        boolean isSlim = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
        this.model = isSlim ? this.slimModel : this.defaultModel;
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        PlayerSkin skin = (state != null && state.skin != null) ? state.skin : DefaultPlayerSkin.getDefaultSkin();
        if (skin == null || skin.body() == null || skin.body().texturePath() == null) {
            return DefaultPlayerSkin.getDefaultSkin().body().texturePath();
        }
        return skin.body().texturePath();
    }
}
