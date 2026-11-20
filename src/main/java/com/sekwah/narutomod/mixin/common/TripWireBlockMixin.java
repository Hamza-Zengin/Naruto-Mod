package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.block.weapons.PaperBombBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TripWireBlock.class)
public class TripWireBlockMixin {

    @Unique
    private static final Direction[] narutomod$CHECK_DIRECTIONS = {Direction.SOUTH, Direction.WEST};

    @Inject(method = "shouldConnectTo", at = @At("HEAD"), cancellable = true)
    public void shouldConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> callback) {
        if(state.getBlock() == NarutoBlocks.PAPER_BOMB.get()) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "updateSource", at = @At(value = "RETURN"))
    public void notifyHook(Level worldIn, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!state.getValue(TripWireBlock.POWERED)) {
            return;
        }
        Block paperBomb = NarutoBlocks.PAPER_BOMB.get();
        for(Direction direction : narutomod$CHECK_DIRECTIONS) {
            mainCheckLoop: for(int i = 1; i < 42; ++i) {
                BlockPos blockpos = pos.relative(direction, i);
                BlockState blockstate = worldIn.getBlockState(blockpos);
                if (blockstate.is(paperBomb)) {
                    for(int j = 1; j < 42; ++j) {
                        BlockPos checkOpposite = pos.relative(direction, -j);
                        BlockState blockStateOpposite = worldIn.getBlockState(checkOpposite);
                        if (blockStateOpposite.is(paperBomb)) {
                            ((PaperBombBlock) paperBomb).spawnPaperbomb(blockstate, worldIn, blockpos, null, true);
                            worldIn.removeBlock(blockpos, false);

                            ((PaperBombBlock) paperBomb).spawnPaperbomb(blockStateOpposite, worldIn, checkOpposite, null, true);
                            worldIn.removeBlock(checkOpposite, false);
                        }
                        if (!blockStateOpposite.is((Block)(Object)this)) {
                            break mainCheckLoop;
                        }
                    }
                    break;
                }

                if (!blockstate.is((Block)(Object)this)) {
                    break;
                }
            }
        }
    }

}
