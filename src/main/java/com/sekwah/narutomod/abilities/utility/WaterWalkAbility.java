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
        return 3;
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
        if(ninjaData.getChakra() < fullCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)), true);
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

    private void updatePlayerMovement(Player player) {
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
        } else if (checks.steadyCheck && resultingYSpeed < 0.0D) {
            resultingYSpeed = 0.0D;
            player.resetFallDistance();
            player.setOnGround(true);
            if(player.isFallFlying()) {
                player.stopFallFlying();
            }
            float f = (float)Math.min(0.1D, player.getDeltaMovement().horizontalDistance());
            player.bob += (f - player.bob) * 0.4F;
        }
        double speedMult = (checks.steadyCheck && NarutoConfig.waterWalkSpeed > 0) ? NarutoConfig.waterWalkSpeed : 1.0D;
        player.setDeltaMovement(vec.x() * speedMult, resultingYSpeed, vec.z() * speedMult);
    }

    public boolean triggerWaterWalk(Level level, BlockPos blockPos) {
        FluidState fluidState = level.getFluidState(blockPos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        updatePlayerMovement(player);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        updatePlayerMovement(player);
    }
}
