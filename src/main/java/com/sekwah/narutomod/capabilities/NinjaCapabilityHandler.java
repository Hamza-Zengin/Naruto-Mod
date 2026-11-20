package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NinjaCapabilityHandler {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NarutoMod.MOD_ID);

    public static final Supplier<AttachmentType<NinjaData>> NINJA_DATA = ATTACHMENT_TYPES.register(
            "ninja_data",
            () -> AttachmentType.serializable(holder ->
                    new NinjaData(!(holder instanceof Player player && player.level().isClientSide())))
                    .copyOnDeath()
                    .build()
    );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isServer = !player.level().isClientSide();
        NinjaData data = player.getData(NINJA_DATA);
        data.setCreativeMode(player.isCreative());
        if (isServer) {
            if (player.isSprinting() && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                data.addStaminaExp(0.03f);
            }
            data.updateServerData(player);
        } else {
            data.updateClientData();
        }
        ToggleAbilityData toggleData = data.getToggleAbilityData();
        if (toggleData != null && toggleData.getAbilitiesHashSet() != null && !toggleData.getAbilitiesHashSet().isEmpty()) {
            for (Identifier abilityName : toggleData.getAbilitiesSnapshot()) {
                if (!toggleData.hasToggleAbility(abilityName)) {
                    continue;
                }
                Ability ability = NarutoAbilities.getAbility(abilityName);
                if (ability == null) continue;
                if (isServer) {
                    if (ability.handleCost(player, data)) {
                        int ticksActive = toggleData.incrementAbilityTicks(abilityName);
                        ability.performServer(player, data, ticksActive);
                    } else {
                        toggleData.removeAbilityEnded(player, data, ability);
                    }
                } else {
                    if (ability instanceof Ability.Toggled toggleAbility) {
                        toggleAbility.performToggleClient(player, data);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            NinjaData original = event.getOriginal().getData(NINJA_DATA);
            NinjaData future = event.getEntity().getData(NINJA_DATA);
            applyDeathPenalty(original, future);
        }
    }

    public static void applyDeathPenalty(INinjaData original, INinjaData future) {
        boolean hasEighthGateToggle = original.getToggleAbilityData().getAbilitiesHashSet().contains(
                com.sekwah.narutomod.abilities.NarutoAbilities.EIGHT_GATES_DEATH.getId());
        NinjaData.applyDeathPenalty(original, future, hasEighthGateToggle);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.getData(NINJA_DATA).updateServerData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.sekwah.narutomod.network.ServerSecurityValidator.clearPlayer(serverPlayer.getUUID());
            com.sekwah.narutomod.network.c2s.ServerJutsuCastingPacket.clearPlayer(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            NinjaData ninjaData = player.getData(NINJA_DATA);
            if (ninjaData.isEighthGateActive()
                    || ninjaData.getToggleAbilityData().hasToggleAbility(
                            com.sekwah.narutomod.abilities.NarutoAbilities.EIGHT_GATES_DEATH.getId())) {
                ninjaData.setShimonPenaltyActive(true);
            }
            if (ninjaData.getCurrentlyChanneledAbility() != null) {
                ninjaData.setCurrentlyChanneledAbility(player, null);
            }
            ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
            if (toggleData != null) {
                toggleData.clearActiveTicks();
            }
        }
        if (event.getSource().getEntity() instanceof Player killer) {
            NinjaData ninjaData = killer.getData(NINJA_DATA);
            ninjaData.addEnemyDefeated();
            if (event.getEntity() instanceof RogueNinjaEntity) {
                ninjaData.addRogueNinjaDefeated();
            } else if (event.getEntity() instanceof AkatsukiNinjaEntity) {
                ninjaData.addAkatsukiNinjaDefeated();
            }

            NinjaMission active = NinjaMission.fromId(ninjaData.getActiveMissionId());
            if (active != null && NinjaMission.MissionStatus.fromString(ninjaData.getMissionStatus()) == NinjaMission.MissionStatus.IN_PROGRESS) {
                boolean matches = switch (active.getTargetType()) {
                    case KILL_ENEMY -> !(event.getEntity() instanceof Player)
                            && !(event.getEntity() instanceof com.sekwah.narutomod.entity.ShadowCloneEntity);
                    case KILL_ROGUE -> event.getEntity() instanceof RogueNinjaEntity;
                    case KILL_AKATSUKI -> event.getEntity() instanceof AkatsukiNinjaEntity;
                    case CAST_JUTSU -> false;
                };
                if (matches) {
                    boolean justCompleted = ninjaData.incrementMissionProgress(1);
                    if (justCompleted) {
                        killer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "naruto.mission.completed",
                                net.minecraft.network.chat.Component.translatable(active.getTranslationKey()))
                                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        NinjaData ninjaData = player.getData(NINJA_DATA);
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
            } else if ("chakra_scalpel".equalsIgnoreCase(activeJutsu)) {
                com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility.executeStrike(player, ninjaData, livingTarget);
                event.setCanceled(true);
            }
        }
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
