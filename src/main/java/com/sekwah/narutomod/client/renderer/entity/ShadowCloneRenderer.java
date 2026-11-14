package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Renders Shadow Clones with the owner player's custom skin, Steve (wide) or Alex (slim) model,
 * and equipped armor and held items.
 */
public class ShadowCloneRenderer extends HumanoidMobRenderer<ShadowCloneEntity, PlayerModel<ShadowCloneEntity>> {

    private final PlayerModel<ShadowCloneEntity> defaultModel;
    private final PlayerModel<ShadowCloneEntity> slimModel;

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.defaultModel = this.getModel();
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new com.sekwah.narutomod.client.renderer.entity.layers.SharinganEyesLayer<>(this));
        this.addLayer(new com.sekwah.narutomod.client.renderer.entity.layers.SusanooRibcageLayer<>(this, context.getModelSet()));
    }

    public boolean isSlimSkin(ShadowCloneEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID != null) {
            Player p = entity.level().getPlayerByUUID(ownerUUID);
            if (p instanceof AbstractClientPlayer clientPlayer) {
                return "slim".equals(clientPlayer.getModelName());
            }
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(ownerUUID);
                if (info != null) {
                    return "slim".equals(info.getModelName());
                }
            }
        }
        return entity.isSlim();
    }

    @Override
    public void render(ShadowCloneEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        boolean isSlim = isSlimSkin(entity);
        this.model = isSlim ? this.slimModel : this.defaultModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID != null) {
            Player p = entity.level().getPlayerByUUID(ownerUUID);
            if (p instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkinTextureLocation();
            }
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(ownerUUID);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }
            String ownerName = entity.getOwnerName();
            if (ownerName != null && !ownerName.isEmpty()) {
                return Minecraft.getInstance().getSkinManager()
                        .getInsecureSkinLocation(new com.mojang.authlib.GameProfile(ownerUUID, ownerName));
            }
            return DefaultPlayerSkin.getDefaultSkin(ownerUUID);
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }
}
