package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuProjectile;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.item.weapons.AnbuTantoItem;
import com.sekwah.narutomod.item.weapons.KubikiribochoItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class PlayerEvents {

    public static final float SAGE_CLOAK_DAMAGE_MULTIPLIER = 1.12F;

    public static boolean isKageHat(Item item) {
        return item == NarutoItems.KAGE_HAT.get()
                || item == NarutoItems.KAZEKAGE_HAT.get()
                || item == NarutoItems.MIZUKAGE_HAT.get()
                || item == NarutoItems.RAIKAGE_HAT.get()
                || item == NarutoItems.TSUCHIKAGE_HAT.get();
    }

    public static boolean isJoninVest(Item item) {
        return item == NarutoItems.JONIN_VEST.get()
                || item == NarutoItems.SAND_JONIN_VEST.get()
                || item == NarutoItems.MIST_JONIN_VEST.get()
                || item == NarutoItems.CLOUD_JONIN_VEST.get()
                || item == NarutoItems.ROCK_JONIN_VEST.get();
    }

    public static ChakraElement getKageHatElement(Item item) {
        if (item == NarutoItems.KAGE_HAT.get()) return ChakraElement.FIRE;
        if (item == NarutoItems.KAZEKAGE_HAT.get()) return ChakraElement.WIND;
        if (item == NarutoItems.MIZUKAGE_HAT.get()) return ChakraElement.WATER;
        if (item == NarutoItems.RAIKAGE_HAT.get()) return ChakraElement.LIGHTNING;
        if (item == NarutoItems.TSUCHIKAGE_HAT.get()) return ChakraElement.EARTH;
        return ChakraElement.NONE;
    }

    public static float applySageCloakDamageMultiplier(float amount, boolean wearingSageCloak) {
        return wearingSageCloak ? amount * SAGE_CLOAK_DAMAGE_MULTIPLIER : amount;
    }

    public static float applyWarringStatesReduction(float amount, boolean wearingWarringStatesArmor, boolean isJutsuOrExplosion) {
        return (wearingWarringStatesArmor && isJutsuOrExplosion) ? amount * 0.85F : amount;
    }

    public static float applyVillageArmorDamageReduction(
            float amount,
            ChakraElement hatAffinity,
            boolean isSandVest,
            boolean isCloudVest,
            boolean isRockVest,
            boolean isFire,
            boolean isLightning,
            boolean isExplosion,
            boolean isProjectile
    ) {
        float result = amount;
        if (hatAffinity == ChakraElement.WIND && isProjectile) {
            result *= 0.85F;
        }
        if (isSandVest && isFire) {
            result *= 0.75F;
        }
        if (isCloudVest && isLightning) {
            result *= 0.75F;
        }
        if (isRockVest && (isExplosion || isProjectile)) {
            result *= 0.80F;
        }
        return result;
    }

    public static boolean isWarringStatesReducedDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return true;
        }
        if (source.getDirectEntity() instanceof JutsuProjectile) {
            return true;
        }
        String ns = source.typeHolder().unwrapKey()
                .map(k -> k.identifier().getNamespace())
                .orElse("");
        return NarutoMod.MOD_ID.equals(ns);
    }

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

            if (player.tickCount % 20 == 0) {
                Item headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
                Item chestItem = player.getItemBySlot(EquipmentSlot.CHEST).getItem();

                // Kage Hat passive aura: empower nearby owned Shadow Clones and tamed Ninken wolves
                if (isKageHat(headItem)) {
                    AABB auraBox = player.getBoundingBox().inflate(16.0D);
                    for (ShadowCloneEntity clone : serverLevel.getEntitiesOfClass(
                            ShadowCloneEntity.class, auraBox,
                            c -> c.isAlive() && player.getUUID().equals(c.getOwnerUUID()))) {
                        clone.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false));
                        clone.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
                    }
                    for (Wolf wolf : serverLevel.getEntitiesOfClass(
                            Wolf.class, auraBox,
                            w -> w.isAlive() && w.isTame() && w.isOwnedBy(player))) {
                        wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false));
                        wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
                    }
                }

                // Mizukage Hat: Dolphin's Grace & Water Breathing while in water
                if (headItem == NarutoItems.MIZUKAGE_HAT.get() && player.isInWater()) {
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
                }

                // Raikage Hat: passive Speed I boost
                if (headItem == NarutoItems.RAIKAGE_HAT.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
                }

                // Mist Jonin Vest: Night Vision in water or darkness
                if (chestItem == NarutoItems.MIST_JONIN_VEST.get() && (player.isInWater() || serverLevel.isDarkOutside())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (ShadowCloneEntity.areAllies(event.getEntity(), event.getSource().getEntity())) {
            event.setCanceled(true);
            return;
        }
        // Kaguya Clan Osteoblast Bone Density Passives (+20% melee damage dealt, -15% physical damage taken)
        // ANBU Tanto Backstab / Stealth (+50% direct melee damage when striking from behind, crouching, invisible, or in smoke)
        // Sage Cloak Senjutsu (+12% outgoing attack and jutsu damage)
        if (event.getSource().getEntity() instanceof Player attacker) {
            var ninjaData = attacker.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                event.setAmount(event.getAmount() * 1.20F);
            }
            if (event.getSource().getDirectEntity() == attacker
                    && attacker.getMainHandItem().getItem() instanceof AnbuTantoItem
                    && event.getEntity() instanceof LivingEntity livingTarget
                    && AnbuTantoItem.isStealthBackstab(attacker, livingTarget)) {
                event.setAmount(event.getAmount() * AnbuTantoItem.BACKSTAB_MULTIPLIER);
            }
            if (attacker.getItemBySlot(EquipmentSlot.CHEST).getItem() == NarutoItems.SAGE_CLOAK.get()) {
                event.setAmount(applySageCloakDamageMultiplier(event.getAmount(), true));
            }
        }
        if (event.getEntity() instanceof Player defender) {
            var ninjaData = defender.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getClan() != null && ninjaData.getClan().getPath().equals("kaguya")) {
                event.setAmount(event.getAmount() * 0.85F);
            }
            Item headItem = defender.getItemBySlot(EquipmentSlot.HEAD).getItem();
            Item chestItem = defender.getItemBySlot(EquipmentSlot.CHEST).getItem();
            boolean wearingWarringStates = chestItem == NarutoItems.WARRING_STATES_ARMOR.get();
            if (wearingWarringStates && isWarringStatesReducedDamage(event.getSource())) {
                event.setAmount(applyWarringStatesReduction(event.getAmount(), true, true));
            }

            event.setAmount(applyVillageArmorDamageReduction(
                    event.getAmount(),
                    getKageHatElement(headItem),
                    chestItem == NarutoItems.SAND_JONIN_VEST.get(),
                    chestItem == NarutoItems.CLOUD_JONIN_VEST.get(),
                    chestItem == NarutoItems.ROCK_JONIN_VEST.get(),
                    event.getSource().is(DamageTypeTags.IS_FIRE),
                    event.getSource().is(DamageTypeTags.IS_LIGHTNING),
                    event.getSource().is(DamageTypeTags.IS_EXPLOSION),
                    event.getSource().is(DamageTypeTags.IS_PROJECTILE)
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player) {
            Item headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
            if (headItem == NarutoItems.TSUCHIKAGE_HAT.get()) {
                event.setStrength(event.getStrength() * 0.60F);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player killer && !killer.level().isClientSide()) {
            ItemStack mainHand = killer.getMainHandItem();
            if (mainHand.getItem() instanceof KubikiribochoItem) {
                KubikiribochoItem.applyBloodRepair(mainHand, true);
                killer.heal(KubikiribochoItem.KILL_HEAL_AMOUNT);
            }
        }
    }

    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() == NarutoItems.SAGE_CLOAK.get()) {
                event.setDistance(0.0F);
                event.setDamageMultiplier(0.0F);
                return;
            }
            double distance = event.getDistance();
            if (distance < 9) {
                distance *= 0.3f;
            }
            if (distance > 3) {
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
        if (ninjaData.isByakuganActive() && event.getTarget() instanceof LivingEntity livingTarget) {
            com.sekwah.narutomod.abilities.jutsus.ByakuganAbility.applyChakraLockStrike(player, livingTarget);
        }
    }
}
