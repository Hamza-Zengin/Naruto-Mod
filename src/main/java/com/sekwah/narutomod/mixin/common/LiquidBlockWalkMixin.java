package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.abilities.utility.WaterWalkAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockWalkMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void narutomod$waterWalkFlowingCollision(BlockState state, BlockGetter level, BlockPos pos,
                                                     CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityCtx
                && entityCtx.getEntity() instanceof Player player
                && WaterWalkAbility.hasActiveWaterWalk(player)) {
            FluidState fluid = state.getFluidState();
            if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) {
                FluidState above = level.getFluidState(pos.above());
                if (!above.is(Fluids.WATER) && !above.is(Fluids.FLOWING_WATER)) {
                    double heightPixels = Math.max(2.0, fluid.getOwnHeight() * 16.0);
                    VoxelShape shape = Block.box(0.0, 0.0, 0.0, 16.0, heightPixels, 16.0);
                    if (context.isAbove(shape, pos, true)) {
                        cir.setReturnValue(shape);
                    }
                }
            }
        }
    }
}
