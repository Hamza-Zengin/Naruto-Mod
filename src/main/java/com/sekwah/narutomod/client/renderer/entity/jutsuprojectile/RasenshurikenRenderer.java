package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.jutsu.RasenshurikenModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
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

public class RasenshurikenRenderer extends EntityRenderer<RasenshurikenEntity, RasenshurikenRenderer.RasenshurikenRenderState> {

    public static final Identifier RASENSHURIKEN_TEX =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/projectiles/rasenshuriken.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(RASENSHURIKEN_TEX);
    private final RasenshurikenModel model;

    public static class RasenshurikenRenderState extends EntityRenderState {
        public float age;
        public float scale = 1.25F;
    }

    public RasenshurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RasenshurikenModel(context.bakeLayer(NarutoRenderEvents.RASENSHURIKEN_LAYER));
    }

    @Override
    public RasenshurikenRenderState createRenderState() {
        return new RasenshurikenRenderState();
    }

    @Override
    public void extractRenderState(RasenshurikenEntity entity, RasenshurikenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.time + partialTicks;
        state.scale = entity.isImpacted()
                ? 1.25F + (entity.getVortexTicks() + partialTicks) * 0.12F
                : 1.25F;
    }

    @Override
    public void submit(RasenshurikenRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.35D, 0.0D);
        poseStack.scale(state.scale, state.scale, state.scale);

        int color = ARGB.colorFromFloat(0.88F, 1.0F, 1.0F, 1.0F);
        submitNodeCollector.submitModel(
                this.model,
                new RasenshurikenModel.SpinState(state.age * 0.45F, state.age * 0.85F),
                poseStack,
                RENDER_TYPE,
                15728880,
                OverlayTexture.NO_OVERLAY,
                color,
                null,
                state.outlineColor
        );
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    protected int getBlockLightLevel(RasenshurikenEntity entity, BlockPos pos) {
        return 15;
    }
}
