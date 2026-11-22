package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Katon: Hōsenka no Jutsu (Phoenix Flower / Phoenix Sage Fire)
 * Launches 4 small fanned mini fireballs simultaneously across a 90-degree horizontal arc (equal 30-degree intervals).
 */
public class PhoenixFlowerAbility extends Ability implements Ability.Cooldown {

    public static final float[] YAW_OFFSETS = new float[]{-45.0F, -15.0F, 15.0F, 45.0F};
    public static final double SPAWN_DISTANCE = 0.45D;

    public static Vec3 computeRotatedDirection(Vec3 baseLook, float yawOffsetDeg) {
        return baseLook.yRot((float) Math.toRadians(-yawOffsetDeg)).normalize();
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.FIRE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 131L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 26.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 100;
    }

    @Override
    public float getDefaultDamage() {
        return 7.5f;
    }

    private static final float[] MASTER_YAW_OFFSETS = new float[]{-45.0F, -22.5F, 0.0F, 22.5F, 45.0F};

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        final float perBallDamage = this.getDamage(ninjaData);
        final double speed = 1.45D * (1.0D + 0.25D * Ability.getMasteryProgress(mastery));
        float[] offsets = getMasteryTier(mastery) >= 4 ? MASTER_YAW_OFFSETS : YAW_OFFSETS;
        for (float yawDeg : offsets) {
            spawnMiniFireball(player, yawDeg, perBallDamage, speed);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                NarutoSounds.FIREBALL_CAST.get(), SoundSource.PLAYERS, 0.85f, 1.35f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.55f, 1.3f);
    }

    private static void spawnMiniFireball(Player player, float yawOffsetDeg, float perBallDamage, double speed) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) {
            return;
        }
        Vec3 dir = computeRotatedDirection(player.getLookAngle(), yawOffsetDeg);
        Vec3 vel = dir.scale(speed);
        FireballJutsuEntity miniFireball = new FireballJutsuEntity(player, vel.x, vel.y, vel.z);
        miniFireball.setPhoenixMini(perBallDamage);
        Vec3 spawnPos = player.getEyePosition().add(dir.scale(SPAWN_DISTANCE)).subtract(0.0D, 0.12D, 0.0D);
        miniFireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        player.level().addFreshEntity(miniFireball);
    }
}
