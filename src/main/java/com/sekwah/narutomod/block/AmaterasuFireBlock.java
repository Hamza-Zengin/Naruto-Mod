package com.sekwah.narutomod.block;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Amaterasu Black Flame Block (Enton).
 * Derived from {@link BaseFireBlock} (NOT {@link net.minecraft.world.level.block.FireBlock})
 * so it never spreads uncontrollably to adjacent blocks or forests.
 * Inflicts brief searing damage and dark particles to entities stepping on it,
 * and self-extinguishes automatically after {@link NarutoConfig#amaterasuDurationTicks}.
 */
public class AmaterasuFireBlock extends BaseFireBlock {

    private static final float CONTACT_DAMAGE = 2.5F;

    public AmaterasuFireBlock(BlockBehaviour.Properties properties) {
        super(properties, CONTACT_DAMAGE);
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
        return this.canSurvive(state, level, pos) ? this.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        // Never spread to or burn surrounding blocks/forests
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            if (!state.canSurvive(level, pos)) {
                level.removeBlock(pos, false);
                return;
            }
            if (!level.isClientSide()) {
                level.scheduleTick(pos, this, Math.max(1, NarutoConfig.amaterasuDurationTicks));
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Self-extinguish once amaterasuDurationTicks elapses
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                6, 0.2D, 0.15D, 0.2D, 0.02D);
        level.removeBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
        if (entity instanceof LivingEntity living && !living.fireImmune()) {
            living.igniteForSeconds(3.0F);
            if (level instanceof ServerLevel serverLevel) {
                if (living.tickCount % 10 == 0) {
                    living.hurtServer(serverLevel, NarutoDamageSource.amaterasu(serverLevel, null, null), CONTACT_DAMAGE);
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                            living.getX(), living.getY() + 0.3D, living.getZ(),
                            4, 0.2D, 0.2D, 0.2D, 0.02D);
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            living.getX(), living.getY() + 0.4D, living.getZ(),
                            3, 0.15D, 0.2D, 0.15D, 0.01D);
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(32) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F + random.nextFloat() * 0.4F,
                    random.nextFloat() * 0.4F + 0.3F,
                    false
            );
        }

        for (int i = 0; i < 2; i++) {
            double xx = pos.getX() + random.nextDouble();
            double yy = pos.getY() + random.nextDouble() * 0.55D + 0.1D;
            double zz = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, xx, yy, zz, 0.0D, 0.03D, 0.0D);
        }

        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SQUID_INK,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 0.2D + random.nextDouble() * 0.4D,
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.025D, 0.0D);
        }
    }
}
