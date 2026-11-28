package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HiraishinKunaiRenderer extends EntityRenderer<HiraishinKunaiEntity, HiraishinKunaiRenderer.HiraishinKunaiRenderState> {
    public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/hiraishin_kunai.png");

    private final ItemModelResolver itemModelResolver;
    private ItemStack hiraishinKunaiStack;

    public static class HiraishinKunaiRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float xRot;
    }

    public HiraishinKunaiRenderer(EntityRendererProvider.Context manager) {
        super(manager);
        this.itemModelResolver = manager.getItemModelResolver();
    }

    @Override
    public HiraishinKunaiRenderState createRenderState() {
        return new HiraishinKunaiRenderState();
    }

    @Override
    public void extractRenderState(HiraishinKunaiEntity entity, HiraishinKunaiRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        if (this.hiraishinKunaiStack == null) {
            this.hiraishinKunaiStack = new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
        }
        this.itemModelResolver.updateForNonLiving(state.item, this.hiraishinKunaiStack, ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(HiraishinKunaiRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.xRot - 90.0F);
        poseStack.scale(0.85F, 0.85F, 0.85F);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
