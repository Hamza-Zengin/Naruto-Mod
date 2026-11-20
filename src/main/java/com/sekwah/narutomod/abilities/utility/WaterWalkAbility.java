package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Water Walking ability
 */
public class WaterWalkAbility extends Ability implements Ability.Toggled {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.WATER;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.ACADEMY_STUDENT;
    }

    @Override
    public long defaultCombo() {
        return 3L;
    }
    private final int CHARKA_COOLDOWN = 15;

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        WaterChecks checks = this.checkSteadyNormalFastPush(player);

        float fullCost = 0;
        if (checks.pushUpFast) {
            fullCost += 1f;
        } else if (checks.steadyCheck) {
            fullCost += 0.12F;
        }
        if (fullCost > 0 && ninjaData != null) {
            int mastery = ninjaData.getAbilityMastery(this);
            fullCost = Math.max(0.01f, fullCost * getChakraCostMultiplier(mastery));
        }
        if(ninjaData.getChakra() < fullCost) {
            player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)));
            return false;
        }
        ninjaData.useChakra(fullCost, CHARKA_COOLDOWN);
        return true;
    }

    public SoundEvent castingSound() {
        return null;
    }

    public record WaterChecks(boolean steadyCheck, boolean pushUpFast, boolean pushUpNormal) {}

    public WaterChecks checkSteadyNormalFastPush(Player player) {
        int blockX = (int)Math.floor(player.getX());
        int blockZ = (int)Math.floor(player.getZ());
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        final int block1 = (int) Math.round(player.getY() - 0.56f);
        boolean steadyCheck = triggerWaterWalk(player.level(), mutablePos.set(blockX, block1, blockZ));

        int block2 = (int) Math.round(player.getY());
        boolean pushUpFast = triggerWaterWalk(player.level(), mutablePos.set(blockX, block2, blockZ));

        int block3 = (int) Math.round(player.getY() - 0.47f);
        boolean pushUpNormal = triggerWaterWalk(player.level(), mutablePos.set(blockX, block3, blockZ));

        return new WaterChecks(steadyCheck, pushUpFast, pushUpNormal);
    }

    public static boolean hasActiveWaterWalk(Player player) {
        if (player == null || player.isSpectator() || player.isShiftKeyDown()) {
            return false;
        }
        try {
            INinjaData data = player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
            if (data == null) {
                return false;
            }
            if (data.isWaterWalkActive()) {
                return true;
            }
            var toggleData = data.getToggleAbilityData();
            return toggleData != null
                    && toggleData.hasToggleAbility(com.sekwah.narutomod.abilities.NarutoAbilities.WATER_WALK.getId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isStandingOnWaterSurface(Player player) {
        if (player == null) return false;
        Level level = player.level();
        int blockX = (int) Math.floor(player.getX());
        int blockZ = (int) Math.floor(player.getZ());
        int blockY = (int) Math.floor(player.getY() - 0.1D);
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
    }

    private WaterChecks updatePlayerMovement(Player player, INinjaData ninjaData) {
        WaterChecks checks = this.checkSteadyNormalFastPush(player);

        Vec3 vec = player.getDeltaMovement();
        double resultingYSpeed = vec.y();
        if (checks.pushUpFast) {
            resultingYSpeed += 0.2D;
            if (resultingYSpeed > 0.6D) {
                resultingYSpeed = 0.6D;
            }
        }
        else if (checks.pushUpNormal) {
            resultingYSpeed += 0.1D;
            if (resultingYSpeed > 0.2D) {
                resultingYSpeed = 0.2D;
            }
        } else if (checks.steadyCheck && resultingYSpeed <= 0.0D) {
            resultingYSpeed = 0.0D;
            player.resetFallDistance();
            player.setOnGround(true);
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            if(player.isFallFlying()) {
                player.stopFallFlying();
            }
        }
        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        double masteryBonus = 1.0 + (getPowerMultiplier(mastery) - 1.0) * 0.25;
        double speedMult = ((checks.steadyCheck && NarutoConfig.waterWalkSpeed > 0) ? NarutoConfig.waterWalkSpeed : 1.0D) * masteryBonus;
        player.setDeltaMovement(vec.x() * speedMult, resultingYSpeed, vec.z() * speedMult);
        return checks;
    }

    public boolean triggerWaterWalk(Level level, BlockPos blockPos) {
        FluidState fluidState = level.getFluidState(blockPos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        WaterChecks checks = updatePlayerMovement(player, ninjaData);
        if (ninjaData != null && player.tickCount % 180 == 0) {
            if ((checks.steadyCheck || checks.pushUpFast || checks.pushUpNormal)
                    && player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                net.minecraft.resources.Identifier reg = this.getRegistryName();
                if (reg != null) {
                    ninjaData.addMastery(reg, 1);
                }
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        updatePlayerMovement(player, ninjaData);
    }
}
