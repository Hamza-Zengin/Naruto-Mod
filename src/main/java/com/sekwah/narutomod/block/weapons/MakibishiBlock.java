package com.sekwah.narutomod.block.weapons;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shinobi Makibishi (Caltrops) tactical floor trap.
 * Holds up to 4 charges; inflicts cooldown-gated piercing damage and Slowness II
 * to non-sneaking LivingEntities stepping into the spikes.
 */
public class MakibishiBlock extends Block {

    public static final int MAX_CHARGES = 4;
    public static final int HIT_COOLDOWN_TICKS = 15;
    public static final float PIERCING_DAMAGE = 2.0F;
    public static final int SLOWNESS_DURATION_TICKS = 50;
    public static final int SLOWNESS_AMPLIFIER = 1;

    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 1, MAX_CHARGES);

    private static final String LAST_HIT_TICK_KEY = "narutomod_makibishi_hit_tick";
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.5D, 15.0D);

    public MakibishiBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGES, MAX_CHARGES));
    }

    public static boolean isHitCooldownReady(int currentTick, int lastHitTick) {
        return lastHitTick < 0 || currentTick < lastHitTick || (currentTick - lastHitTick) >= HIT_COOLDOWN_TICKS;
    }

    public static int computeRemainingCharges(int currentCharges) {
        return Math.max(0, currentCharges - 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSteppingCarefully()) {
            return;
        }

        int nowTick = living.tickCount;
        int lastTick = living.getPersistentData().getIntOr(LAST_HIT_TICK_KEY, -HIT_COOLDOWN_TICKS);
        if (!isHitCooldownReady(nowTick, lastTick)) {
            return;
        }
        living.getPersistentData().putInt(LAST_HIT_TICK_KEY, nowTick);

        living.hurtServer(serverLevel, serverLevel.damageSources().cactus(), PIERCING_DAMAGE);
        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER));

        serverLevel.playSound(null, pos, SoundEvents.IRON_GOLEM_HURT, SoundSource.BLOCKS, 0.45F, 1.65F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5D, pos.getY() + 0.15D, pos.getZ() + 0.5D,
                5, 0.25D, 0.08D, 0.25D, 0.05D);

        int remaining = computeRemainingCharges(state.getValue(CHARGES));
        if (remaining <= 0) {
            serverLevel.destroyBlock(pos, false);
        } else {
            serverLevel.setBlock(pos, state.setValue(CHARGES, remaining), Block.UPDATE_ALL);
        }
    }
}
