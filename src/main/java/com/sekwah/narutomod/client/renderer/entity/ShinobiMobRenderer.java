package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Renderer for Rogue Ninja and Akatsuki S-Rank Shinobi Mobs.
 * Supports held weapons (Kunai, Kusanagi, Samehada) and 3D armor/cloaks.
 */
public class ShinobiMobRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {

    private final ResourceLocation texture;

    public ShinobiMobRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.texture = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/mob/" + textureName + ".png");
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
