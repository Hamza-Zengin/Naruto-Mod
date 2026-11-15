package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class PlayerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            // Check every 30 seconds at night in the Overworld for natural Shinobi encounters
            if (player.tickCount % 600 == 0 && serverLevel.dimension() == Level.OVERWORLD && serverLevel.isDarkOutside() && !player.isCreative() && !player.isSpectator()) {
                if (serverLevel.getRandom().nextFloat() < 0.22f) {
                    AABB checkArea = player.getBoundingBox().inflate(48.0D);
                    int nearbyNinjas = serverLevel.getEntitiesOfClass(RogueNinjaEntity.class, checkArea).size()
                            + serverLevel.getEntitiesOfClass(AkatsukiNinjaEntity.class, checkArea).size();
                    if (nearbyNinjas < 2) {
                        double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2.0D;
                        double dist = 20.0D + serverLevel.getRandom().nextDouble() * 14.0D;
                        int sx = (int) (player.getX() + Math.cos(angle) * dist);
                        int sz = (int) (player.getZ() + Math.sin(angle) * dist);
                        int sy = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                        BlockPos spawnPos = new BlockPos(sx, sy, sz);

                        if (sy > serverLevel.getMinY() + 5 && serverLevel.getBlockState(spawnPos).isAir()) {
                            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
                            NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
                            if (rank.getLevel() >= NinjaRank.CHUNIN.getLevel() && serverLevel.getRandom().nextFloat() < 0.18f) {
                                AkatsukiNinjaEntity akatsuki = NarutoEntities.AKATSUKI_NINJA.get().create(serverLevel, EntitySpawnReason.NATURAL);
                                if (akatsuki != null) {
                                    akatsuki.snapTo(sx + 0.5, sy, sz + 0.5, serverLevel.getRandom().nextFloat() * 360F, 0F);
                                    akatsuki.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
                                    serverLevel.addFreshEntity(akatsuki);
                                }
                            } else {
                                RogueNinjaEntity rogue = NarutoEntities.ROGUE_NINJA.get().create(serverLevel, EntitySpawnReason.NATURAL);
                                if (rogue != null) {
                                    rogue.snapTo(sx + 0.5, sy, sz + 0.5, serverLevel.getRandom().nextFloat() * 360F, 0F);
                                    rogue.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
                                    serverLevel.addFreshEntity(rogue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(event.getEntity(), event.getSource().getEntity())) {
            event.setCanceled(true);
            return;
        }
        // Kaguya Clan Osteoblast Bone Density Passives (+20% melee damage dealt, -15% physical damage taken)
        if (event.getSource().getEntity() instanceof Player attacker) {
            var ninjaData = attacker.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                event.setAmount(event.getAmount() * 1.20F);
            }
        }
        if (event.getEntity() instanceof Player defender) {
            var ninjaData = defender.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                event.setAmount(event.getAmount() * 0.85F);
            }
        }
    }

    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player){
            double distance = event.getDistance();
            if(distance < 9){
                distance *= 0.3f;
            }
            if(distance > 3) {
                distance -= 5f;
                distance *= 0.6f;
            }
            event.setDistance(distance);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.isCanceled()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        if (ninjaData.isByakuganActive() && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
            com.sekwah.narutomod.abilities.jutsus.ByakuganAbility.applyChakraLockStrike(player, livingTarget);
        }
    }

}
