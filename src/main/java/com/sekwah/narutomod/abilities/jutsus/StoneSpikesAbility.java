package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Doton: Stone Spikes (Doryūsō - 土遁·土流槍)
 * High-level Earth Release jutsu that erupts jagged stone spears beneath a targeted area.
 */
public class StoneSpikesAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.EARTH;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mud_wall"));
    }

    @Override
    public int getRequiredMastery() {
        return 150;
    }

    @Override
    public long defaultCombo() {
        return 2312;
    }

    @Override
    public float getDefaultChakraCost() {
        return 45.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 200;
    }

    @Override
    public float getDefaultDamage() {
        return 22.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = resolveEruptionOrigin(serverLevel, player, 14.0D);
        float damage = this.getDamage(ninjaData);

        serverLevel.playSound(null, BlockPos.containing(targetPos), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.PLAYERS, 1.5F, 0.85F);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.6F, 0.7F);

        JutsuVisualEffectEntity spikes = JutsuVisualEffectEntity.create(
                serverLevel, targetPos.x, targetPos.y, targetPos.z,
                JutsuVisualEffectEntity.TYPE_STONE_SPIKES, 50, player.getYRot());
        spikes.setCasterUUID(player.getUUID());
        spikes.setEffectDamage(damage);
        serverLevel.addFreshEntity(spikes);
    }

    private static Vec3 resolveEruptionOrigin(ServerLevel level, Player player, double maxRange) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxRange));

        HitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 anchor = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : eye.add(look.scale(8.0D));

        // Snap onto closest enemy near the aim ray if one is in line
        AABB rayBox = new AABB(eye, anchor).inflate(2.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, rayBox,
                e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e));
        LivingEntity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity c : candidates) {
            double dSq = c.position().distanceToSqr(anchor);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                bestTarget = c;
            }
        }
        if (bestTarget != null) {
            return bestTarget.position();
        }

        // Otherwise ground below anchor
        BlockPos pos = BlockPos.containing(anchor.x, anchor.y, anchor.z);
        for (int dy = 0; dy <= 6; dy++) {
            BlockPos check = pos.below(dy);
            if (!level.getBlockState(check).isAir()) {
                return new Vec3(anchor.x, check.getY() + 1.0D, anchor.z);
            }
        }
        return new Vec3(anchor.x, player.getY(), anchor.z);
    }
}
