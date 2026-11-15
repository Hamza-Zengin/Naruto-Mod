package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.*;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Renders all 10 custom 3D Jutsu Models for JutsuVisualEffectEntity with
 * full-bright chakra illumination, smooth animations, and translucent shaders.
 */
public class JutsuVisualEffectRenderer extends EntityRenderer<JutsuVisualEffectEntity, JutsuVisualEffectRenderer.VisualEffectRenderState> {

    public static final Identifier TEX_KIRIN = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/kirin_dragon.png");
    public static final Identifier TEX_WOOD = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/wood_prison.png");
    public static final Identifier TEX_BONE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/bone_forest.png");
    public static final Identifier TEX_RASENGAN = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/rasengan_sphere.png");
    public static final Identifier TEX_CHIDORI = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/chidori_burst.png");
    public static final Identifier TEX_WATER = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/water_vortex.png");
    public static final Identifier TEX_WIND = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/wind_shockwave.png");
    public static final Identifier TEX_AMATERASU = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/amaterasu_flame.png");
    public static final Identifier TEX_SHADOW = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/shadow_bind.png");
    public static final Identifier TEX_EARTH = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/earth_wall.png");

    private final KirinDragonModel kirinModel;
    private final WoodPrisonModel woodModel;
    private final BoneForestModel boneModel;
    private final RasenganSphereModel rasenganModel;
    private final ChidoriBurstModel chidoriModel;
    private final WaterVortexModel waterModel;
    private final WindShockwaveModel windModel;
    private final AmaterasuFlameModel amaterasuModel;
    private final ShadowBindModel shadowModel;
    private final EarthWallModel earthModel;

    public static class VisualEffectRenderState extends EntityRenderState {
        public int visualType;
        public float effectYaw;
        public float age;
        public float progress;
    }

    public JutsuVisualEffectRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.kirinModel = new KirinDragonModel(ctx.bakeLayer(NarutoRenderEvents.KIRIN_DRAGON_LAYER));
        this.woodModel = new WoodPrisonModel(ctx.bakeLayer(NarutoRenderEvents.WOOD_PRISON_LAYER));
        this.boneModel = new BoneForestModel(ctx.bakeLayer(NarutoRenderEvents.BONE_FOREST_LAYER));
        this.rasenganModel = new RasenganSphereModel(ctx.bakeLayer(NarutoRenderEvents.RASENGAN_SPHERE_LAYER));
        this.chidoriModel = new ChidoriBurstModel(ctx.bakeLayer(NarutoRenderEvents.CHIDORI_BURST_LAYER));
        this.waterModel = new WaterVortexModel(ctx.bakeLayer(NarutoRenderEvents.WATER_VORTEX_LAYER));
        this.windModel = new WindShockwaveModel(ctx.bakeLayer(NarutoRenderEvents.WIND_SHOCKWAVE_LAYER));
        this.amaterasuModel = new AmaterasuFlameModel(ctx.bakeLayer(NarutoRenderEvents.AMATERASU_FLAME_LAYER));
        this.shadowModel = new ShadowBindModel(ctx.bakeLayer(NarutoRenderEvents.SHADOW_BIND_LAYER));
        this.earthModel = new EarthWallModel(ctx.bakeLayer(NarutoRenderEvents.EARTH_WALL_LAYER));
    }

    @Override
    public VisualEffectRenderState createRenderState() {
        return new VisualEffectRenderState();
    }

    @Override
    public void extractRenderState(JutsuVisualEffectEntity entity, VisualEffectRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        state.visualType = entity.getVisualType();
        state.effectYaw = entity.getEffectYaw();
        state.age = age;
        state.progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
    }

    @Override
    public void submit(VisualEffectRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        int type = state.visualType;
        Identifier tex = getTextureLocation(type);
        RenderType renderType = (type == JutsuVisualEffectEntity.TYPE_EARTH_WALL
                || type == JutsuVisualEffectEntity.TYPE_BONE_FOREST
                || type == JutsuVisualEffectEntity.TYPE_WOOD_PRISON)
                ? RenderTypes.entityCutout(tex)
                : RenderTypes.entityTranslucentCull(tex);

        poseStack.pushPose();
        try {
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.effectYaw);

            float fadeOut = state.progress > 0.82F ? (1.0F - ((state.progress - 0.82F) / 0.18F)) : 1.0F;
            int fullBright = 15728880; // 0x00F000F0 (Block 15, Sky 15)
            int solidLight = Math.max(state.lightCoords, 12583104); // Block 12, Sky 12 minimum ambient baseline
            VisualState visualState = new VisualState(state.age, state.progress);

            switch (type) {
                case JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON -> {
                    float scale = 1.65F;
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.92F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.kirinModel, visualState, poseStack, renderType, fullBright, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_WOOD_PRISON -> {
                    float scale = 1.25F;
                    poseStack.scale(-scale, -scale, scale);
                    submitNodeCollector.submitModel(this.woodModel, visualState, poseStack, renderType, solidLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_BONE_FOREST -> {
                    float scale = 1.2F;
                    poseStack.scale(-scale, -scale, scale);
                    submitNodeCollector.submitModel(this.boneModel, visualState, poseStack, renderType, solidLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 0.85F + state.progress * 1.35F;
                    poseStack.scale(-scale, -scale, scale);
                    float alpha = 0.88F * fadeOut;
                    submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vc) ->
                            this.rasenganModel.renderProcedural(pose, vc, state.age, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha));
                }
                case JutsuVisualEffectEntity.TYPE_CHIDORI_BURST -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 1.05F + Mth.sin(state.age * 0.8F) * 0.18F;
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.90F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.chidoriModel, visualState, poseStack, renderType, fullBright, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_WATER_VORTEX -> {
                    poseStack.translate(0.0D, 0.9D, 0.0D);
                    float scale = 1.15F + (state.progress > 0.7F ? (state.progress - 0.7F) * 2.2F : 0.0F);
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.86F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.waterModel, visualState, poseStack, renderType, fullBright, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 0.85F + state.progress * 1.65F;
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.82F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.windModel, visualState, poseStack, renderType, fullBright, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR -> {
                    float scale = 1.15F;
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.94F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.amaterasuModel, visualState, poseStack, renderType, fullBright, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_SHADOW_BIND -> {
                    float scale = 1.1F;
                    poseStack.scale(-scale, -scale, scale);
                    int color = ARGB.colorFromFloat(0.92F * fadeOut, 1.0F, 1.0F, 1.0F);
                    submitNodeCollector.submitModel(this.shadowModel, visualState, poseStack, renderType, solidLight, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
                }
                case JutsuVisualEffectEntity.TYPE_EARTH_WALL -> {
                    float scale = 1.0F;
                    poseStack.scale(-scale, -scale, scale);
                    submitNodeCollector.submitModel(this.earthModel, visualState, poseStack, renderType, solidLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
                }
            }
        } finally {
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    protected int getBlockLightLevel(JutsuVisualEffectEntity entity, BlockPos pos) {
        int type = entity.getVisualType();
        if (type == JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON
                || type == JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE
                || type == JutsuVisualEffectEntity.TYPE_CHIDORI_BURST
                || type == JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR
                || type == JutsuVisualEffectEntity.TYPE_EARTH_WALL) {
            return 15;
        }
        return super.getBlockLightLevel(entity, pos);
    }

    private static Identifier getTextureLocation(int type) {
        return switch (type) {
            case JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON -> TEX_KIRIN;
            case JutsuVisualEffectEntity.TYPE_WOOD_PRISON -> TEX_WOOD;
            case JutsuVisualEffectEntity.TYPE_BONE_FOREST -> TEX_BONE;
            case JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE -> TEX_RASENGAN;
            case JutsuVisualEffectEntity.TYPE_CHIDORI_BURST -> TEX_CHIDORI;
            case JutsuVisualEffectEntity.TYPE_WATER_VORTEX -> TEX_WATER;
            case JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE -> TEX_WIND;
            case JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR -> TEX_AMATERASU;
            case JutsuVisualEffectEntity.TYPE_SHADOW_BIND -> TEX_SHADOW;
            case JutsuVisualEffectEntity.TYPE_EARTH_WALL -> TEX_EARTH;
            default -> TEX_RASENGAN;
        };
    }
}
