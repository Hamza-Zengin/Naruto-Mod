package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
            // Check every 30 seconds at night in the Overworld for natural Shinobi encounters
            if (player.tickCount % 600 == 0 && serverLevel.dimension() == Level.OVERWORLD && serverLevel.isNight() && !player.isCreative() && !player.isSpectator()) {
                if (serverLevel.random.nextFloat() < 0.22f) {
                    AABB checkArea = player.getBoundingBox().inflate(48.0D);
                    int nearbyNinjas = serverLevel.getEntitiesOfClass(RogueNinjaEntity.class, checkArea).size()
                            + serverLevel.getEntitiesOfClass(AkatsukiNinjaEntity.class, checkArea).size();
                    if (nearbyNinjas < 2) {
                        double angle = serverLevel.random.nextDouble() * Math.PI * 2.0D;
                        double dist = 20.0D + serverLevel.random.nextDouble() * 14.0D;
                        int sx = (int) (player.getX() + Math.cos(angle) * dist);
                        int sz = (int) (player.getZ() + Math.sin(angle) * dist);
                        int sy = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                        BlockPos spawnPos = new BlockPos(sx, sy, sz);

                        if (sy > serverLevel.getMinBuildHeight() + 5 && serverLevel.getBlockState(spawnPos).isAir()) {
                            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                                NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
                                if (rank.getLevel() >= NinjaRank.CHUNIN.getLevel() && serverLevel.random.nextFloat() < 0.18f) {
                                    AkatsukiNinjaEntity akatsuki = NarutoEntities.AKATSUKI_NINJA.get().create(serverLevel);
                                    if (akatsuki != null) {
                                        akatsuki.moveTo(sx + 0.5, sy, sz + 0.5, serverLevel.random.nextFloat() * 360F, 0F);
                                        akatsuki.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null, null);
                                        serverLevel.addFreshEntity(akatsuki);
                                    }
                                } else {
                                    RogueNinjaEntity rogue = NarutoEntities.ROGUE_NINJA.get().create(serverLevel);
                                    if (rogue != null) {
                                        rogue.moveTo(sx + 0.5, sy, sz + 0.5, serverLevel.random.nextFloat() * 360F, 0F);
                                        rogue.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null, null);
                                        serverLevel.addFreshEntity(rogue);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Kaguya Clan Osteoblast Bone Density Passives (+20% melee damage dealt, -15% physical damage taken)
        if (event.getSource().getEntity() instanceof Player attacker) {
            attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                    event.setAmount(event.getAmount() * 1.20F);
                }
            });
        }
        if (event.getEntity() instanceof Player defender) {
            defender.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                    event.setAmount(event.getAmount() * 0.85F);
                }
            });
        }
    }

    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player){
            float distance = event.getDistance();
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

}
