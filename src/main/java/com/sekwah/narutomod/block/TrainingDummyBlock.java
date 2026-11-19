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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Functional 3D Shinobi Training Dummy (Kakashis / Konoha Training Ground Scarecrow).
 * Measures melee, thrown weapon, and jutsu projectile hits while granting
 * cooldown-gated Stamina EXP, Chakra EXP, and Jutsu Mastery.
 */
public class TrainingDummyBlock extends RotatableDecorativeBlock {

    public static final int MELEE_EXP_COOLDOWN_TICKS = 15;
    public static final int PROJECTILE_EXP_COOLDOWN_TICKS = 20;
    public static final float MELEE_STAMINA_EXP = 1.5F;
    public static final float JUTSU_CHAKRA_EXP = 2.0F;
    public static final int JUTSU_MASTERY_GAIN = 1;

    private static final String LAST_MELEE_TICK_KEY = "narutomod_dummy_melee_tick";
    private static final String LAST_PROJ_TICK_KEY = "narutomod_dummy_proj_tick";

    private static final VoxelShape DUMMY_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 2.0D, 13.0D),
            Block.box(6.5D, 2.0D, 6.5D, 9.5D, 5.0D, 9.5D),
            Block.box(4.0D, 5.0D, 4.5D, 12.0D, 12.5D, 11.5D),
            Block.box(4.5D, 12.0D, 4.5D, 11.5D, 16.0D, 11.5D)
    );

    public TrainingDummyBlock(BlockBehaviour.Properties properties) {
        super(properties, DUMMY_SHAPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        return DUMMY_SHAPE;
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

        emitHitEffects(serverLevel, pos, crit);
        player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.hit_melee",
                String.format("%.1f", dmg))
                .withStyle(crit ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.9F, 1.25F);
            player.sendOverlayMessage(Component.translatable("block.narutomod.training_dummy.use")
                    .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        boolean isJutsu = projectile instanceof JutsuProjectile;
        float estDamage = estimateProjectileDamage(projectile);
        emitHitEffects(serverLevel, pos, isJutsu);

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

    private static void emitHitEffects(ServerLevel serverLevel, BlockPos pos, boolean intense) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.65D;
        double cz = pos.getZ() + 0.5D;
        serverLevel.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, intense ? 0.9F : 1.15F);
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()),
                cx, cy, cz, intense ? 14 : 7, 0.25D, 0.28D, 0.25D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        if (intense) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    cx, cy + 0.2D, cz, 8, 0.25D, 0.25D, 0.25D, 0.12D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }
}
