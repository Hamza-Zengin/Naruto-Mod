package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FumaShurikenRenderer extends EntityRenderer<FumaShurikenEntity, FumaShurikenRenderer.FumaRenderState> {

    private final ItemModelResolver itemModelResolver;
    private ItemStack fumaStack;

    public static class FumaRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float xRot;
        public float spin;
    }

    public FumaShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
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
        if (this.fumaStack == null) {
            this.fumaStack = new ItemStack(NarutoItems.FUMA_SHURIKEN.get());
        }
        this.itemModelResolver.updateForNonLiving(state.item, this.fumaStack, ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(FumaRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.22D, 0.0D);
        poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.xRot);
        poseStack.rotateDegrees(Axis.XP, 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.spin);
        poseStack.scale(1.15F, 1.15F, 1.15F);

        state.item.submit(
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
