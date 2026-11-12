package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shikotsumyaku: Sawarabi no Mai (Dance of the Seedling Fern)
 * Kaguya Clan Kekkei Genkai: Erupts calcified bone spikes in 3 expanding shockwaves around the caster
 * and materializes a Kaguya Bone Sword in the user's hand if they don't already hold one.
 */
public class BoneDanceAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.GENIN;
    }

    @Override
    public long defaultCombo() {
        return 132; // C - B - V
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public float getDefaultChakraCost() {
        return 32.0f;
    }

    @Override
    public float getDefaultDamage() {
        return 7.5f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("kaguya")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Kaguya").withStyle(ChatFormatting.WHITE)), true);
            return false;
        }
        if (checkCooldown(player, ninjaData, this.getTranslationKey())) {
            return false;
        }
        if (!tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), 0, 45)) {
            return false;
        }
        registerCooldown(ninjaData, this.getTranslationKey());
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        int resistTicks = (int) (160 * powerMult);
        int resistAmp = getMasteryTier(mastery) >= 4 ? 2 : 1;

        // Grant temporary calcified bone armor resistance and materialize Kaguya Bone Sword if empty handed
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistTicks, resistAmp));
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand( net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(NarutoItems.BONE_SWORD.get()));
        }

        Vec3 origin = player.position();
        BlockParticleOption boneParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());

        // Erupt 3D Calcified Bone Spire Forest in 3 concentric shockwave rings
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity boneForest =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, origin.x, origin.y, origin.z,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_BONE_FOREST,
                        65, player.getYRot());
        serverLevel.addFreshEntity(boneForest);

        float spikeDmg = this.getDamage(ninjaData);
        for (int ring = 1; ring <= 3; ring++) {
            final int rIndex = ring;
            ninjaData.scheduleDelayedTickEvent(p -> {
                if (!(p.level() instanceof ServerLevel sLevel)) return;
                double radius = rIndex * (2.2D + (getMasteryTier(mastery) >= 3 ? 0.3D : 0.0D));

                sLevel.playSound(null, p.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.5f, 0.55f);
                sLevel.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.5f, 0.8f);

                int spikes = 10 + rIndex * 4;
                for (int i = 0; i < spikes; i++) {
                    double angle = (i * 2.0 * Math.PI) / spikes;
                    double sx = origin.x + Math.cos(angle) * radius;
                    double sz = origin.z + Math.sin(angle) * radius;
                    for (double h = 0.0; h <= 2.2; h += 0.45) {
                        sLevel.sendParticles(boneParticle, sx, origin.y + h, sz, 3, 0.08, 0.15, 0.08, 0.02);
                    }
                    sLevel.sendParticles(ParticleTypes.CRIT, sx, origin.y + 1.8, sz, 2, 0.05, 0.1, 0.05, 0.02);
                }

                AABB hitBox = new AABB(origin.x - radius - 1.2, origin.y - 1.0, origin.z - radius - 1.2,
                        origin.x + radius + 1.2, origin.y + 3.0, origin.z + radius + 1.2);
                List<LivingEntity> enemies = sLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> e.isAlive() && e != p);

                for (LivingEntity enemy : enemies) {
                    double dist = Math.sqrt(enemy.distanceToSqr(origin.x, origin.y, origin.z));
                    if (Math.abs(dist - radius) <= 1.8D) {
                        enemy.hurt(p.damageSources().playerAttack(p), spikeDmg);
                        enemy.setDeltaMovement(enemy.getDeltaMovement().x, 0.52D, enemy.getDeltaMovement().z);
                        enemy.hurtMarked = true;
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    }
                }
            }, (ring - 1) * 8);
        }
    }
}
