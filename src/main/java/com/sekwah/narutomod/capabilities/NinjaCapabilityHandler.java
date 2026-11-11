package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NinjaCapabilityHandler {

    public static final Capability<INinjaData> NINJA_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(NarutoMod.MOD_ID, "ninja_data"), new NinjaData(!event.getObject().level().isClientSide));
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if(event.phase.equals(TickEvent.Phase.END)) {
            Player player = event.player;
            player.getCapability(NINJA_DATA).ifPresent(data -> {
                data.setCreativeMode(player.isCreative());
                if(event.side.isServer()) {
                    if (player.isSprinting() && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                        data.addStaminaExp(0.03f);
                    }
                    data.updateServerData(player);
                }
                ToggleAbilityData toggleData = data.getToggleAbilityData();
                if (toggleData != null) {
                    for (ResourceLocation abilityName : toggleData.getAbilitiesSnapshot()) {
                        if (!toggleData.hasToggleAbility(abilityName)) {
                            continue;
                        }
                        Ability ability = NarutoAbilities.getAbility(abilityName);
                        if(ability == null) continue;
                        if(event.side.isServer()) {
                            if(ability.handleCost(player, data)) {
                                ability.performServer(player, data);
                            } else {
                                toggleData.removeAbilityEnded(player, data, ability);
                            }
                        } else {
                            if(ability instanceof Ability.Toggled toggleAbility) toggleAbility.performToggleClient(player, data);
                        }
                    }
                }
            });
        }

    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(NINJA_DATA).ifPresent(original -> {
            event.getEntity().getCapability(NINJA_DATA).ifPresent(future -> {
                future.deserializeNBT(original.serializeNBT());
                if (event.isWasDeath()) {
                    future.setChakra(future.getMaxChakra());
                    future.setStamina(future.getMaxStamina());
                }
            });
        });
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getCurrentlyChanneledAbility() != null) {
                    ninjaData.setCurrentlyChanneledAbility(player, null);
                }
            });
        }
        if (event.getSource().getEntity() instanceof Player killer) {
            killer.getCapability(NINJA_DATA).ifPresent(INinjaData::addEnemyDefeated);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        player.getCapability(NINJA_DATA).ifPresent(ninjaData -> {
            String activeJutsu = ninjaData.getActiveHandJutsu();
            if (ninjaData.getActiveHandJutsuTicks() > 0 && !activeJutsu.isEmpty()) {
                Entity target = event.getTarget();
                net.minecraft.world.entity.LivingEntity livingTarget = (target instanceof net.minecraft.world.entity.LivingEntity l) ? l : null;
                if ("rasengan".equalsIgnoreCase(activeJutsu)) {
                    com.sekwah.narutomod.abilities.jutsus.RasenganAbility.executeStrike(player, ninjaData, livingTarget);
                    event.setCanceled(true);
                } else if ("chidori".equalsIgnoreCase(activeJutsu)) {
                    com.sekwah.narutomod.abilities.jutsus.ChidoriAbility.executeStrike(player, ninjaData, livingTarget);
                    event.setCanceled(true);
                }
            }
        });
    }

    //public static final EntitySize STANDING_SIZE = EntitySize.flexible(0.1F, 0.1F);

    // For things like the transformation jutsus (make sure to trigger it manually when you change)
    @SubscribeEvent
    public static void playerSize(EntityEvent.Size event) {
//        Entity entity = event.getEntity();
//        if (entity instanceof PlayerEntity) {
//            event.setNewEyeHeight(5f);
//            event.setNewSize(EntitySize.flexible(10F, 10F));
//        }
    }
}
