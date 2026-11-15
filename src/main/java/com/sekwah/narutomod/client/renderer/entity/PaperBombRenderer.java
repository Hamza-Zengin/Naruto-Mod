package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.entity.item.PaperBombEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PaperBombRenderer extends EntityRenderer<PaperBombEntity, PaperBombRenderer.PaperBombRenderState> {

    public static class PaperBombRenderState extends EntityRenderState {
        public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
        public AttachFace face = AttachFace.FLOOR;
        public Vec3i dir = Vec3i.ZERO;
    }

    public PaperBombRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public PaperBombRenderState createRenderState() {
        return new PaperBombRenderState();
    }

    @Override
    public void extractRenderState(PaperBombEntity entity, PaperBombRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockPos blockPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.dir = entity.renderBlockState.getValue(BlockStateProperties.HORIZONTAL_FACING).getUnitVec3i();
        state.face = entity.renderBlockState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        state.movingBlockRenderState.randomSeedPos = entity.getOrigin();
        state.movingBlockRenderState.blockPos = blockPos;
        state.movingBlockRenderState.blockState = entity.renderBlockState;
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(blockPos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(PaperBombRenderState state, PoseStack matrixStackIn, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        matrixStackIn.pushPose();

        matrixStackIn.translate(-0.5D, -0.25D, -0.5D);

        switch (state.face) {
            case FLOOR:
                matrixStackIn.translate(0, 0.25D, 0);
                break;
            case CEILING:
                matrixStackIn.translate(0, -0.25D, 0);
                break;
            case WALL:
                matrixStackIn.translate(0.25D * state.dir.getX(), 0, 0.25D * state.dir.getZ());
                break;
        }

        submitNodeCollector.submitMovingBlock(matrixStackIn, state.movingBlockRenderState, state.outlineColor);

        matrixStackIn.popPose();
        super.submit(state, matrixStackIn, submitNodeCollector, cameraRenderState);
    }
}
