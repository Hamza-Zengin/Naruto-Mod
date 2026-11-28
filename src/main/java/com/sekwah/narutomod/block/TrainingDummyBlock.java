package com.sekwah.narutomod.block;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuProjectile;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Functional 2-Block-Tall 3D Shinobi Training Dummy (Konoha Training Ground Post).
 * Measures melee, thrown weapon, and jutsu projectile hits on either half while granting
 * cooldown-gated Stamina EXP, Chakra EXP, and Jutsu Mastery.
 */
public class TrainingDummyBlock extends RotatableDecorativeBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public static final int MELEE_EXP_COOLDOWN_TICKS = 15;
    public static final int PROJECTILE_EXP_COOLDOWN_TICKS = 20;
    public static final float MELEE_STAMINA_EXP = 1.5F;
    public static final float JUTSU_CHAKRA_EXP = 2.0F;
    public static final int JUTSU_MASTERY_GAIN = 1;

    private static final String LAST_MELEE_TICK_KEY = "narutomod_dummy_melee_tick";
    private static final String LAST_PROJ_TICK_KEY = "narutomod_dummy_proj_tick";
    private static final String LAST_PROJ_ENTITY_TICK_KEY = "narutomod_dummy_proj_hit_tick";

    private static final VoxelShape LOWER_NORTH = Shapes.or(
            Block.box(2.0D, 0.0D, 6.5D, 14.0D, 1.8D, 9.5D),
            Block.box(6.5D, 0.0D, 2.0D, 9.5D, 1.8D, 14.0D),
            Block.box(5.5D, 1.8D, 5.5D, 10.5D, 4.2D, 10.5D),
            Block.box(6.75D, 4.2D, 6.75D, 9.25D, 12.5D, 9.25D),
            Block.box(4.55D, 12.5D, 4.8D, 11.45D, 16.0D, 11.2D)
    );

    private static final VoxelShape UPPER_NORTH = Shapes.or(
            Block.box(4.3D, 0.0D, 4.55D, 11.7D, 8.25D, 11.45D),
            Block.box(5.0D, 2.1D, 4.0D, 11.0D, 7.9D, 4.75D),
            Block.box(0.5D, 5.15D, 6.65D, 15.5D, 7.75D, 9.35D),
            Block.box(4.8D, 8.25D, 4.8D, 11.2D, 15.25D, 11.2D)
    );

    private static final Map<Direction, VoxelShape> LOWER_SHAPES = buildHorizontalShapes(LOWER_NORTH);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = buildHorizontalShapes(UPPER_NORTH);

    private static final class Particles {
        static final BlockParticleOption HAY = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState());
    }

    public TrainingDummyBlock(BlockBehaviour.Properties properties) {
        super(properties, LOWER_NORTH);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    private static Map<Direction, VoxelShape> buildHorizontalShapes(VoxelShape northShape) {
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            map.put(dir, rotateShapeFromNorth(dir, northShape));
        }
        return map;
    }

    private static VoxelShape rotateShapeFromNorth(Direction facing, VoxelShape northShape) {
        if (facing == Direction.NORTH) {
            return northShape;
        }
        VoxelShape[] rotated = new VoxelShape[]{Shapes.empty()};
        northShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case EAST -> Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX);
                case SOUTH -> Shapes.box(1.0D - maxX, minY, 1.0D - maxZ, 1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(minZ, minY, 1.0D - maxX, maxZ, maxY, 1.0D - minX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
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
        DoubleBlockHalf half = state.getValue(HALF);
        if (directionToNeighbour.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (directionToNeighbour == Direction.UP)) {
            if (neighbourState.is(this) && neighbourState.getValue(HALF) != half) {
                return state.setValue(FACING, neighbourState.getValue(FACING));
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))) {
            preventDropFromBottomPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void preventDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState replacement = belowState.getFluidState().is(Fluids.WATER)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(belowPos, replacement, 35);
                level.levelEvent(player, 2001, belowPos, Block.getId(belowState));
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        Map<Direction, VoxelShape> map = state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES : LOWER_SHAPES;
        return map.getOrDefault(facing, LOWER_NORTH);
    }

    public static BlockPos getBasePos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /**
     * Pure helper for computing cooldown-gated training progress eligibility.
     */
    public static boolean isTrainingCooldownReady(int currentTick, int lastRewardTick, int cooldownTicks) {
        return lastRewardTick <= 0 || currentTick < lastRewardTick || (currentTick - lastRewardTick) >= cooldownTicks;
    }

    /**
     * Pure helper for calculating displayed melee training damage.
     */
    public static float computeMeleeTrainingDamage(double baseAttackDamage, boolean critical) {
        double safe = Math.max(1.0D, baseAttackDamage);
        return (float) (critical ? safe * 1.5D : safe);
    }

    @Nullable
    public static Identifier resolveJutsuAbilityId(@Nullable Projectile projectile) {
        if (projectile instanceof FireballJutsuEntity fireball) {
            return Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, fireball.isPhoenixMini() ? "phoenix_flower" : "fireball");
        }
        if (projectile instanceof WaterBulletEntity) {
            return Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_bullet");
        }
        if (projectile instanceof EarthBulletEntity) {
            return Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "earth_dragon_bullet");
        }
        if (projectile instanceof VacuumSphereEntity) {
            return Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "vacuum_spheres");
        }
        if (projectile instanceof RasenshurikenEntity) {
            return Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "rasenshuriken");
        }
        return null;
    }

    public static float estimateProjectileDamage(@Nullable Projectile projectile) {
        if (projectile instanceof FireballJutsuEntity fireball) {
            return fireball.isPhoenixMini() ? 7.0F : 14.0F;
        }
        if (projectile instanceof RasenshurikenEntity) {
            return 28.0F;
        }
        if (projectile instanceof WaterBulletEntity) {
            return 10.0F;
        }
        if (projectile instanceof EarthBulletEntity) {
            return 11.0F;
        }
        if (projectile instanceof VacuumSphereEntity) {
            return 8.0F;
        }
        if (projectile instanceof ExplosiveKunaiEntity) {
            return 12.0F;
        }
        if (projectile instanceof FumaShurikenEntity) {
            return 8.5F;
        }
        if (projectile instanceof KunaiEntity) {
            return 5.0F;
        }
        if (projectile instanceof ShurikenEntity) {
            return 4.0F;
        }
        if (projectile instanceof SenbonEntity) {
            return 3.0F;
        }
        return 4.0F;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        handleMeleeHit(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(state, level, pos, player);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        handleProjectileHit(level, state, hit.getBlockPos(), projectile, hit.getLocation());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof Projectile projectile) {
            handleProjectileHit(level, state, pos, projectile, entity.position());
        }
    }

    private static void handleMeleeHit(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int nowTick = player.tickCount;
        int lastTick = player.getPersistentData().getIntOr(LAST_MELEE_TICK_KEY, -MELEE_EXP_COOLDOWN_TICKS);
        if (!isTrainingCooldownReady(nowTick, lastTick, MELEE_EXP_COOLDOWN_TICKS)) {
            return;
        }
        player.getPersistentData().putInt(LAST_MELEE_TICK_KEY, nowTick);

        boolean crit = !player.onGround() && player.fallDistance > 0.0F;
        double attackVal = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float dmg = computeMeleeTrainingDamage(attackVal, crit);

        var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        ninjaData.addStaminaExp(MELEE_STAMINA_EXP);

        emitHitEffects(serverLevel, pos, state, null, crit);
        player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.hit_melee",
                String.format("%.1f", dmg))
                .withStyle(crit ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
    }

    private static InteractionResult handleUse(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos basePos = getBasePos(state, pos);
            serverLevel.playSound(null, basePos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.85F, 1.25F);
            serverLevel.playSound(null, basePos, SoundEvents.GRASS_HIT, SoundSource.BLOCKS, 0.55F, 1.15F);
            player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.use")
                    .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    private static void handleProjectileHit(Level level, BlockState state, BlockPos pos,
                                            Projectile projectile, @Nullable Vec3 hitLocation) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int lastEntityHitTick = projectile.getPersistentData().getIntOr(LAST_PROJ_ENTITY_TICK_KEY, -1);
        if (lastEntityHitTick == projectile.tickCount) {
            return;
        }
        projectile.getPersistentData().putInt(LAST_PROJ_ENTITY_TICK_KEY, projectile.tickCount);

        boolean isJutsu = projectile instanceof JutsuProjectile;
        float estDamage = estimateProjectileDamage(projectile);
        emitHitEffects(serverLevel, pos, state, hitLocation, isJutsu);

        if (projectile.getOwner() instanceof Player player) {
            int nowTick = player.tickCount;
            int lastTick = player.getPersistentData().getIntOr(LAST_PROJ_TICK_KEY, -PROJECTILE_EXP_COOLDOWN_TICKS);
            boolean canReward = isTrainingCooldownReady(nowTick, lastTick, PROJECTILE_EXP_COOLDOWN_TICKS);

            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (canReward) {
                player.getPersistentData().putInt(LAST_PROJ_TICK_KEY, nowTick);
                if (isJutsu) {
                    ninjaData.addChakraExp(JUTSU_CHAKRA_EXP);
                    Identifier abilityId = resolveJutsuAbilityId(projectile);
                    if (abilityId != null) {
                        ninjaData.addMastery(abilityId, JUTSU_MASTERY_GAIN);
                    }
                } else {
                    ninjaData.addStaminaExp(MELEE_STAMINA_EXP);
                }
            }

            if (projectile instanceof JutsuProjectile jutsuProj) {
                ChakraElement element = jutsuProj.getElement();
                String elementLabel = element != null ? element.name() : "NONE";
                player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.hit_jutsu",
                        String.format("%.1f", estDamage), elementLabel)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.hit_melee",
                        String.format("%.1f", estDamage))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static void emitHitEffects(ServerLevel serverLevel, BlockPos pos, BlockState state,
                                       @Nullable Vec3 hitLocation, boolean intense) {
        double cx = hitLocation != null ? hitLocation.x : pos.getX() + 0.5D;
        double cy = hitLocation != null
                ? hitLocation.y
                : pos.getY() + (state.getValue(HALF) == DoubleBlockHalf.UPPER ? 0.45D : 0.80D);
        double cz = hitLocation != null ? hitLocation.z : pos.getZ() + 0.5D;

        serverLevel.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, intense ? 0.9F : 1.15F);
        serverLevel.playSound(null, pos, SoundEvents.GRASS_HIT, SoundSource.BLOCKS, 0.65F, intense ? 0.95F : 1.15F);
        serverLevel.sendParticles(Particles.HAY,
                cx, cy, cz, intense ? 14 : 7, 0.22D, 0.25D, 0.22D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        if (intense) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    cx, cy + 0.15D, cz, 8, 0.22D, 0.22D, 0.22D, 0.12D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }
}
