package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.FumaShurikenModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FumaShurikenRenderer extends EntityRenderer<FumaShurikenEntity, FumaShurikenRenderer.FumaRenderState> {

    public static final Identifier FUMA_TEX =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/projectiles/fuma_shuriken.png");
    private final FumaShurikenModel model;

    public static class FumaRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float spin;
    }

    public FumaShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FumaShurikenModel(context.bakeLayer(NarutoRenderEvents.FUMA_SHURIKEN_LAYER));
    }

    @Override
    public FumaRenderState createRenderState() {
        return new FumaRenderState();
    }

    @Override
    public void extractRenderState(FumaShurikenEntity entity, FumaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.spin = Mth.lerp(partialTicks, entity.getPrevRotateTicks(), entity.getRotateTicks()) * 42.0F;
    }

    @Override
    public void submit(FumaRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.15D, 0.0D);
        poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.xRot);
        poseStack.rotateDegrees(Axis.YP, state.spin);
        poseStack.scale(0.95F, 0.95F, 0.95F);

        submitNodeCollector.submitModel(
                this.model,
                state,
                poseStack,
                RenderTypes.entityCutout(FUMA_TEX),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
