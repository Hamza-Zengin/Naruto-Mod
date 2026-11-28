package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.FlashBombEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FlashBombRenderer extends EntityRenderer<FlashBombEntity, FlashBombRenderer.FlashBombRenderState> {

    private final ItemModelResolver itemModelResolver;
    private ItemStack flashBombStack;

    public static class FlashBombRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float spin;
    }

    public FlashBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public FlashBombRenderState createRenderState() {
        return new FlashBombRenderState();
    }

    @Override
    public void extractRenderState(FlashBombEntity entity, FlashBombRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.spin = (entity.tickCount + partialTicks) * 28.0F;
        if (this.flashBombStack == null) {
            this.flashBombStack = new ItemStack(NarutoItems.FLASH_BOMB.get());
        }
        this.itemModelResolver.updateForNonLiving(state.item, this.flashBombStack, ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(FlashBombRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D, 0.0D);
        poseStack.rotateDegrees(Axis.YP, state.spin);
        poseStack.rotateDegrees(Axis.XP, state.spin * 0.7F);
        poseStack.scale(0.9F, 0.9F, 0.9F);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
