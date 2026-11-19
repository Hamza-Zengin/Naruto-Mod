package com.sekwah.narutomod.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.block.WeaponRackBlock;
import com.sekwah.narutomod.block.WeaponRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Renders the 2 stored weapon ItemStacks onto the 3D WeaponRack shelves using MC 26.3 render-state extraction.
 */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity, WeaponRackRenderer.WeaponRackRenderState> {

    private final ItemModelResolver itemModelResolver;

    public static class WeaponRackRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public final ItemStackRenderState upperItem = new ItemStackRenderState();
        public final ItemStackRenderState lowerItem = new ItemStackRenderState();
    }

    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public WeaponRackRenderState createRenderState() {
        return new WeaponRackRenderState();
    }

    @Override
    public void extractRenderState(WeaponRackBlockEntity blockEntity, WeaponRackRenderState renderState,
                                   float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);
        renderState.facing = blockEntity.getBlockState().hasProperty(WeaponRackBlock.FACING)
                ? blockEntity.getBlockState().getValue(WeaponRackBlock.FACING)
                : Direction.NORTH;

        Level level = blockEntity.getLevel();
        ItemStack upper = blockEntity.getItem(WeaponRackBlockEntity.SLOT_UPPER);
        ItemStack lower = blockEntity.getItem(WeaponRackBlockEntity.SLOT_LOWER);

        this.itemModelResolver.updateForTopItem(renderState.upperItem, upper, ItemDisplayContext.FIXED, level, null, 0);
        this.itemModelResolver.updateForTopItem(renderState.lowerItem, lower, ItemDisplayContext.FIXED, level, null, 1);
    }

    @Override
    public void submit(WeaponRackRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.upperItem.isEmpty() && renderState.lowerItem.isEmpty()) {
            return;
        }

        float yRot = switch (renderState.facing) {
            case SOUTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            case NORTH -> 0.0F;
            default -> 0.0F;
        };

        if (!renderState.upperItem.isEmpty()) {
            renderShelfItem(renderState.upperItem, 0.72D, yRot, poseStack, submitNodeCollector, renderState.lightCoords);
        }
        if (!renderState.lowerItem.isEmpty()) {
            renderShelfItem(renderState.lowerItem, 0.34D, yRot, poseStack, submitNodeCollector, renderState.lightCoords);
        }
    }

    private static void renderShelfItem(ItemStackRenderState itemState, double yOffset, float yRot,
                                        PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        poseStack.pushPose();
        poseStack.translate(0.5D, yOffset, 0.5D);
        poseStack.rotateDegrees(Axis.YP, yRot);
        poseStack.translate(0.0D, 0.0D, -0.06D);
        poseStack.rotateDegrees(Axis.ZP, -45.0F);
        poseStack.scale(0.68F, 0.68F, 0.68F);
        itemState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
