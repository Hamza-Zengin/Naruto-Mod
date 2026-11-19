package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.SmokeBombEntity;
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

public class SmokeBombRenderer extends EntityRenderer<SmokeBombEntity, SmokeBombRenderer.SmokeBombRenderState> {

    private final ItemModelResolver itemModelResolver;
    private ItemStack smokeBombStack;

    public static class SmokeBombRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float spin;
    }

    public SmokeBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public SmokeBombRenderState createRenderState() {
        return new SmokeBombRenderState();
    }

    @Override
    public void extractRenderState(SmokeBombEntity entity, SmokeBombRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.spin = (entity.tickCount + partialTicks) * 28.0F;
        if (this.smokeBombStack == null) {
            this.smokeBombStack = new ItemStack(NarutoItems.SMOKE_BOMB.get());
        }
        this.itemModelResolver.updateForNonLiving(state.item, this.smokeBombStack, ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(SmokeBombRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
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
