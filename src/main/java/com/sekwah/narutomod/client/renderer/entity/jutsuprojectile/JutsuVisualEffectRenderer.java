package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels.*;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders all 10 custom 3D Jutsu Models for JutsuVisualEffectEntity with
 * full-bright chakra illumination, smooth animations, and translucent shaders.
 */
public class JutsuVisualEffectRenderer extends EntityRenderer<JutsuVisualEffectEntity> {

    public static final ResourceLocation TEX_KIRIN = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/kirin_dragon.png");
    public static final ResourceLocation TEX_WOOD = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/wood_prison.png");
    public static final ResourceLocation TEX_BONE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/bone_forest.png");
    public static final ResourceLocation TEX_RASENGAN = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/rasengan_sphere.png");
    public static final ResourceLocation TEX_CHIDORI = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/chidori_burst.png");
    public static final ResourceLocation TEX_WATER = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/water_vortex.png");
    public static final ResourceLocation TEX_WIND = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/wind_shockwave.png");
    public static final ResourceLocation TEX_AMATERASU = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/amaterasu_flame.png");
    public static final ResourceLocation TEX_SHADOW = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/shadow_bind.png");
    public static final ResourceLocation TEX_EARTH = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/earth_wall.png");

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
    public void render(JutsuVisualEffectEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float maxLife = Math.max(1.0F, (float) entity.getMaxLife());
        float progress = Mth.clamp(age / maxLife, 0.0F, 1.0F);
        int type = entity.getVisualType();

        ResourceLocation tex = getTextureLocation(entity);
        RenderType renderType = (type == JutsuVisualEffectEntity.TYPE_EARTH_WALL || type == JutsuVisualEffectEntity.TYPE_BONE_FOREST || type == JutsuVisualEffectEntity.TYPE_WOOD_PRISON)
                ? RenderType.entityCutoutNoCull(tex)
                : RenderType.entityTranslucentCull(tex);
        VertexConsumer consumer = buffer.getBuffer(renderType);

        poseStack.pushPose();
        try {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getEffectYaw()));

            float fadeOut = progress > 0.82F ? (1.0F - ((progress - 0.82F) / 0.18F)) : 1.0F;
            int fullBright = 15728880; // 0x00F000F0 (Block 15, Sky 15)
            int solidLight = Math.max(packedLight, 12583104); // Block 12, Sky 12 minimum ambient baseline

            switch (type) {
                case JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON -> {
                    float scale = 1.65F;
                    poseStack.scale(-scale, -scale, scale);
                    this.kirinModel.setupAnim(age, progress);
                    this.kirinModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.92F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_WOOD_PRISON -> {
                    float scale = 1.25F;
                    poseStack.scale(-scale, -scale, scale);
                    this.woodModel.setupAnim(age, progress);
                    this.woodModel.renderToBuffer(poseStack, consumer, solidLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                case JutsuVisualEffectEntity.TYPE_BONE_FOREST -> {
                    float scale = 1.2F;
                    poseStack.scale(-scale, -scale, scale);
                    this.boneModel.setupAnim(age);
                    this.boneModel.renderToBuffer(poseStack, consumer, solidLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                case JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 0.85F + progress * 1.35F;
                    poseStack.scale(-scale, -scale, scale);
                    this.rasenganModel.setupAnim(age);
                    this.rasenganModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.88F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_CHIDORI_BURST -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 1.05F + Mth.sin(age * 0.8F) * 0.18F;
                    poseStack.scale(-scale, -scale, scale);
                    this.chidoriModel.setupAnim(age);
                    this.chidoriModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.90F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_WATER_VORTEX -> {
                    poseStack.translate(0.0D, 0.9D, 0.0D);
                    float scale = 1.15F + (progress > 0.7F ? (progress - 0.7F) * 2.2F : 0.0F);
                    poseStack.scale(-scale, -scale, scale);
                    this.waterModel.setupAnim(age);
                    this.waterModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.86F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE -> {
                    poseStack.translate(0.0D, 1.0D, 0.0D);
                    float scale = 0.85F + progress * 1.65F;
                    poseStack.scale(-scale, -scale, scale);
                    this.windModel.setupAnim(age);
                    this.windModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.82F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR -> {
                    float scale = 1.15F;
                    poseStack.scale(-scale, -scale, scale);
                    this.amaterasuModel.setupAnim(age);
                    this.amaterasuModel.renderToBuffer(poseStack, consumer, fullBright, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.94F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_SHADOW_BIND -> {
                    float scale = 1.1F;
                    poseStack.scale(-scale, -scale, scale);
                    this.shadowModel.setupAnim(age, progress);
                    this.shadowModel.renderToBuffer(poseStack, consumer, solidLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.92F * fadeOut);
                }
                case JutsuVisualEffectEntity.TYPE_EARTH_WALL -> {
                    float scale = 1.0F;
                    poseStack.scale(-scale, -scale, scale);
                    this.earthModel.setupAnim(progress);
                    this.earthModel.renderToBuffer(poseStack, consumer, solidLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        } finally {
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
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

    @Override
    public ResourceLocation getTextureLocation(JutsuVisualEffectEntity entity) {
        return switch (entity.getVisualType()) {
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
