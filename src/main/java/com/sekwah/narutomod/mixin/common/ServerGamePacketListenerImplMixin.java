package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.abilities.utility.WaterWalkAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private int lastSupportedBlocksSend;

    @Shadow
    private int tickCount;

    @Inject(method = "forceSendPlayerSupportBlocks", at = @At("HEAD"), cancellable = true)
    private void narutomod$preventFalseStandingOnAir(CallbackInfo ci) {
        if (this.player == null) {
            return;
        }
        // 1. Player is standing on water with active Suimen Hoko
        if (WaterWalkAbility.hasActiveWaterWalk(this.player) && WaterWalkAbility.isStandingOnWaterSurface(this.player)) {
            this.lastSupportedBlocksSend = this.tickCount;
            ci.cancel();
            return;
        }

        // 2. Player actually has solid support blocks directly underneath (e.g. immediately after Kawarimi teleport)
        Level level = this.player.level();
        AABB box = this.player.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);
        int checkY = (int) Math.floor(box.minY - 0.05D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, checkY, z);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                    this.lastSupportedBlocksSend = this.tickCount;
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
