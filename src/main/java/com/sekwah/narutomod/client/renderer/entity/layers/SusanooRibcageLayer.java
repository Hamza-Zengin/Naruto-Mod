package com.sekwah.narutomod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderEvents;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.sekwah.sekclib.util.ColorUtil;
import com.sekwah.sekclib.util.RenderMathUtil;
import net.minecraft.resources.Identifier;

public class SusanooRibcageLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier SUSANOO_TEXTURE =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/entity/jutsu/susanoo_ribcage.png");
    private final SusanooRibcageModel ribcageModel;

    public SusanooRibcageLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.ribcageModel = new SusanooRibcageModel(modelSet.bakeLayer(NarutoRenderEvents.SUSANOO_RIBCAGE_LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        NarutoRenderEvents.NinjaRenderStateData data = state.getRenderData(NarutoRenderEvents.NINJA_RENDER_DATA_KEY);
        if (data == null || !data.hasSusanooRibcage()) {
            return;
        }

        poseStack.pushPose();
        // Imposing +18% scale to give Susanoo a majestic presence enveloping the entity
        poseStack.translate(0.0D, -0.06D, 0.0D);
        RenderMathUtil.scaleUniform(poseStack, 1.18F);

        int tintColor = ColorUtil.argb(0.55F, 1.0F, 0.55F, 0.25F);
        submitNodeCollector.submitModel(
                this.ribcageModel,
                state,
                poseStack,
                RenderTypes.entityTranslucent(SUSANOO_TEXTURE),
                RenderMathUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                tintColor,
                null,
                state.outlineColor
        );
        poseStack.popPose();
    }
}
