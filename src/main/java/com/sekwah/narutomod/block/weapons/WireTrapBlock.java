package com.sekwah.narutomod.block.weapons;

import com.sekwah.narutomod.block.RotatableDecorativeBlock;
import com.sekwah.narutomod.util.WorldProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Shinobi Wire Trap (tripwire ambush).
 * Roots non-sneaking LivingEntities with Slowness VI for 2.5 seconds,
 * emits a metallic alarm and sparks, ignites any PaperBombBlocks within 3x3x3
 * with a short fuse, and destroys itself upon triggering.
 */
public class WireTrapBlock extends RotatableDecorativeBlock {

    public static final int ROOT_DURATION_TICKS = 50;
    public static final int ROOT_SLOWNESS_AMPLIFIER = 5;
    public static final int PAPER_BOMB_SCAN_RADIUS = 1;

    private static final VoxelShape SHAPE_EW = Block.box(0.0D, 0.0D, 6.5D, 16.0D, 4.0D, 9.5D);
    private static final VoxelShape SHAPE_NS = Block.box(6.5D, 0.0D, 0.0D, 9.5D, 4.0D, 16.0D);

    public WireTrapBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE_EW);
    }

    public static boolean shouldTriggerForEntity(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSteppingCarefully();
    }

    public static boolean isPaperBombState(@Nullable BlockState state) {
        return state != null && state.getBlock() instanceof PaperBombBlock;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        return this.canSurvive(state, level, pos)
                ? super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random)
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !shouldTriggerForEntity(entity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;

        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ROOT_DURATION_TICKS, ROOT_SLOWNESS_AMPLIFIER));
        Vec3 motion = living.getDeltaMovement();
        living.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        living.hurtMarked = true;

        serverLevel.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.35F);
        serverLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, 1.85F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                10, 0.35D, 0.15D, 0.35D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                8, 0.3D, 0.1D, 0.3D, 0.08D);

        triggerAdjacentPaperBombs(serverLevel, pos, living);
        serverLevel.destroyBlock(pos, false);
    }

    private static void triggerAdjacentPaperBombs(ServerLevel serverLevel, BlockPos origin, LivingEntity triggerEntity) {
        for (BlockPos scanPos : BlockPos.betweenClosed(
                origin.offset(-PAPER_BOMB_SCAN_RADIUS, -PAPER_BOMB_SCAN_RADIUS, -PAPER_BOMB_SCAN_RADIUS),
                origin.offset(PAPER_BOMB_SCAN_RADIUS, PAPER_BOMB_SCAN_RADIUS, PAPER_BOMB_SCAN_RADIUS))) {
            if (scanPos.equals(origin)) {
                continue;
            }
            BlockState adjState = serverLevel.getBlockState(scanPos);
            if (adjState.getBlock() instanceof PaperBombBlock paperBomb
                    && WorldProtectionHelper.canModifyBlock(serverLevel, scanPos, triggerEntity)) {
                BlockPos targetPos = scanPos.immutable();
                paperBomb.spawnPaperbomb(adjState, serverLevel, targetPos, triggerEntity, true);
                serverLevel.removeBlock(targetPos, false);
            }
        }
    }
}
