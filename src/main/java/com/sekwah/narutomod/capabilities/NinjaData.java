package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.sekclib.capabilitysync.annotation.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NinjaData implements INinjaData {

    @Sync(minTicks = 1)
    private float chakra;

    @Sync(minTicks = 1)
    private float stamina;

    @Sync
    private float maxChakra;

    @Sync
    private float maxStamina;

    @Sync
    private Identifier clan;

    @Sync
    private boolean clanLocked = false;

    @Sync
    private String rank = "ACADEMY_STUDENT";

    @Sync
    private float chakraExp = 0;

    @Sync
    private float staminaExp = 0;

    @Sync
    private int enemiesDefeated = 0;

    @Sync
    private int rogueNinjasDefeated = 0;

    @Sync
    private int akatsukiNinjasDefeated = 0;

    @Sync
    private int jutsusCastCount = 0;

    @Sync
    private String unlockedJutsus = "";

    @Sync
    private Map<Identifier, Integer> jutsuMastery = new HashMap<>();

    @Sync
    private String activeMissionId = "";

    @Sync
    private int missionProgress = 0;

    @Sync
    private int completedMissionsCount = 0;

    @Sync
    private String missionStatus = "NONE";

    private boolean creativeMode = false;

    @Sync(minTicks = 1)
    private Identifier currentlyChanneled;

    @Sync(minTicks = 1)
    private int ticksChanneled;

    @Sync(minTicks = 1, syncGlobally = true)
    private ToggleAbilityData toggleAbilityData;

    @Sync(minTicks = 1, syncGlobally = true)
    private String activeHandJutsu = "";

    @Sync(minTicks = 1, syncGlobally = true)
    private int activeHandJutsuTicks = 0;

    @Sync(minTicks = 1, syncGlobally = true)
    private int activeJutsusFlag = 0;

    @Sync
    private boolean kageHatEquipped = false;

    public static final int DEFAULT_SEAL_TIMEOUT = 60;
    public static final int MAX_ACTIVE_SEALS = 12;

    private final List<Byte> activeSeals = new ArrayList<>();
    private int sealTimeout = 0;

    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    public NinjaData() {
        this(true);
    }

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.maxChakra = NarutoConfig.startingChakra > 0 ? NarutoConfig.startingChakra : 100.0f;
            this.maxStamina = NarutoConfig.startingStamina > 0 ? NarutoConfig.startingStamina : 100.0f;
            this.chakra = this.getMaxChakra();
            this.stamina = this.getMaxStamina();
            this.clan = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi");
            this.clanLocked = false;
            this.unlockedJutsus = "";
            this.rank = "ACADEMY_STUDENT";
            this.chakraExp = 0;
            this.staminaExp = 0;
            this.activeJutsusFlag = 0;
            this.kageHatEquipped = false;
        }
        this.toggleAbilityData = new ToggleAbilityData();
    }

    private final ChakraStaminaLogic.RegenInfo chakraRegenInfo =
            new ChakraStaminaLogic.RegenInfo(NarutoConfig.chakraRegenRate > 0 ? NarutoConfig.chakraRegenRate : 0.05f);
    private final ChakraStaminaLogic.RegenInfo staminaRegenInfo =
            new ChakraStaminaLogic.RegenInfo(NarutoConfig.staminaRegenRate > 0 ? NarutoConfig.staminaRegenRate : 0.4f);

    @Override
    public float getChakra() {
        return this.chakra;
    }

    @Override
    public float getMaxChakra() {
        return ChakraStaminaLogic.computeMaxChakra(this.maxChakra, this.rank, this.clan, this.kageHatEquipped);
    }

    @Override
    public boolean isKageHatEquipped() {
        return this.kageHatEquipped;
    }

    @Override
    public void setKageHatEquipped(boolean equipped) {
        this.kageHatEquipped = equipped;
        float maxC = this.getMaxChakra();
        if (this.chakra > maxC) {
            this.chakra = maxC;
        }
    }

    @Override
    public float getBaseMaxChakra() {
        return this.maxChakra;
    }

    @Override
    public float getStamina() {
        return this.stamina;
    }

    @Override
    public float getMaxStamina() {
        return ChakraStaminaLogic.computeMaxStamina(this.maxStamina, this.rank);
    }

    @Override
    public float getBaseMaxStamina() {
        return this.maxStamina;
    }

    @Override
    public void setChakra(float chakra) {
        float max = this.getMaxChakra();
        this.chakra = Math.min(Math.max(chakra, 0), max);
    }

    @Override
    public void setStamina(float stamina) {
        float max = this.getMaxStamina();
        this.stamina = Math.min(Math.max(stamina, 0), max);
    }

    @Override
    public void setMaxChakra(float amount) {
        setBaseMaxChakra(amount);
    }

    @Override
    public void setMaxStamina(float amount) {
        setBaseMaxStamina(amount);
    }

    @Override
    public void setBaseMaxChakra(float amount) {
        this.maxChakra = Math.min(Math.max(100f, amount), 1000f);
        float max = this.getMaxChakra();
        if (this.chakra > max) this.chakra = max;
    }

    @Override
    public void setBaseMaxStamina(float amount) {
        this.maxStamina = Math.min(Math.max(100f, amount), 800f);
        float max = this.getMaxStamina();
        if (this.stamina > max) this.stamina = max;
    }

    @Override
    public Identifier getClan() {
        return this.clan;
    }

    @Override
    public void setClan(Identifier clan) {
        // Clan cannot be changed once locked by player
        if (this.clanLocked) {
            return;
        }
        this.clan = clan;
    }

    @Override
    public void forceSetClan(Identifier clan) {
        this.clan = clan;
        this.clanLocked = true;
    }

    @Override
    public boolean isClanLocked() {
        return this.clanLocked;
    }

    @Override
    public void setClanLocked(boolean locked) {
        this.clanLocked = locked;
    }

    @Override
    public String getRank() {
        return this.rank != null ? this.rank : "ACADEMY_STUDENT";
    }

    @Override
    public void setRank(String rank) {
        this.rank = rank;
        // Clamp current stats if rank change adjusted max caps
        float maxC = this.getMaxChakra();
        float maxS = this.getMaxStamina();
        if (this.chakra > maxC) this.chakra = maxC;
        if (this.stamina > maxS) this.stamina = maxS;
    }

    @Override
    public boolean isCreativeMode() {
        return this.creativeMode;
    }

    @Override
    public void setCreativeMode(boolean creative) {
        this.creativeMode = creative;
    }

    @Override
    public boolean isAbilityUnlocked(Ability ability) {
        if (ability == null) return false;
        return JutsuUnlockRules.checkAbilityUnlocked(this, ability, ability.getRegistryName());
    }

    public static boolean isScrollRequiredJutsu(Identifier id) {
        return JutsuUnlockRules.isScrollRequiredJutsu(id);
    }

    public static boolean isClanRestrictedJutsu(Identifier id) {
        return JutsuUnlockRules.isClanRestrictedJutsu(id);
    }

    @Override
    public boolean isAbilityUnlocked(Identifier abilityId) {
        if (abilityId == null) return false;
        Ability ability = null;
        try {
            ability = NarutoAbilities.getAbility(abilityId);
        } catch (Throwable ignored) {
            // NarutoAbilities or registry may not be initialized in standalone unit tests
        }
        if (ability == null) {
            return getUnlockedAbilities().contains(abilityId);
        }
        return JutsuUnlockRules.checkAbilityUnlocked(this, ability, abilityId);
    }

    @Override
    public void unlockAbility(Identifier abilityId) {
        this.unlockedJutsus = JutsuUnlockRules.addUnlockedAbility(this.unlockedJutsus, abilityId);
    }

    @Override
    public void lockAbility(Identifier abilityId) {
        this.unlockedJutsus = JutsuUnlockRules.removeUnlockedAbility(this.unlockedJutsus, abilityId);
    }

    @Override
    public java.util.Set<Identifier> getUnlockedAbilities() {
        return JutsuUnlockRules.parseUnlockedAbilities(this.unlockedJutsus);
    }

    @Override
    public String getUnlockedJutsusString() {
        return this.unlockedJutsus != null ? this.unlockedJutsus : "";
    }

    @Override
    public void setUnlockedJutsusString(String jutsus) {
        this.unlockedJutsus = jutsus != null ? jutsus : "";
    }

    @Override
    public Map<Identifier, Integer> getJutsuMasteryMap() {
        return this.jutsuMastery;
    }

    @Override
    public int getMastery(Identifier jutsuId) {
        if (jutsuId == null) return 0;
        return this.jutsuMastery.getOrDefault(jutsuId, 0);
    }

    @Override
    public void setMastery(Identifier jutsuId, int amount) {
        if (jutsuId == null) return;
        this.jutsuMastery.put(jutsuId, Math.max(0, amount));
    }

    @Override
    public void addMastery(Identifier jutsuId, int amount) {
        if (jutsuId == null || amount == 0) return;
        int current = getMastery(jutsuId);
        int updated = Math.max(0, current + amount);
        this.jutsuMastery.put(jutsuId, updated);
    }

    @Override
    public void incrementJutsuMastery(Identifier jutsuId, boolean isToggle) {
        if (jutsuId == null) return;
        int current = getMastery(jutsuId);
        int gain;
        if (current < 100) {
            gain = isToggle ? 2 : 5;
        } else if (current < 250) {
            gain = isToggle ? 2 : 3;
        } else if (current < 500) {
            gain = isToggle ? 1 : 2;
        } else {
            gain = 1;
        }
        addMastery(jutsuId, gain);
    }

    @Override
    public float getChakraExp() {
        return this.chakraExp;
    }

    @Override
    public void setChakraExp(float amount) {
        this.chakraExp = Math.max(0f, amount);
    }

    @Override
    public void addChakraExp(float amount) {
        ChakraStaminaLogic.addChakraExp(this, amount);
    }

    @Override
    public float getStaminaExp() {
        return this.staminaExp;
    }

    @Override
    public void setStaminaExp(float amount) {
        this.staminaExp = Math.max(0f, amount);
    }

    @Override
    public void addStaminaExp(float amount) {
        ChakraStaminaLogic.addStaminaExp(this, amount);
    }

    @Override
    public int getEnemiesDefeated() {
        return RankProgression.getEnemiesDefeated(this.getRank(), this.enemiesDefeated);
    }

    @Override
    public void addEnemyDefeated() {
        RankProgression.addEnemyDefeated(this);
    }

    @Override
    public void setEnemiesDefeated(int count) {
        this.enemiesDefeated = count;
    }

    @Override
    public int getRogueNinjasDefeated() {
        return this.rogueNinjasDefeated;
    }

    @Override
    public void addRogueNinjaDefeated() {
        this.rogueNinjasDefeated++;
    }

    @Override
    public void setRogueNinjasDefeated(int count) {
        this.rogueNinjasDefeated = count;
    }

    @Override
    public int getAkatsukiNinjasDefeated() {
        return this.akatsukiNinjasDefeated;
    }

    @Override
    public void addAkatsukiNinjaDefeated() {
        this.akatsukiNinjasDefeated++;
    }

    @Override
    public void setAkatsukiNinjasDefeated(int count) {
        this.akatsukiNinjasDefeated = count;
    }

    @Override
    public int getJutsusCastCount() {
        return RankProgression.getJutsusCastCount(this.getRank(), this.jutsusCastCount);
    }

    @Override
    public void addJutsuCast() {
        RankProgression.addJutsuCast(this);
    }

    @Override
    public void setJutsusCastCount(int count) {
        this.jutsusCastCount = count;
    }

    @Override
    public boolean canRankUp() {
        return RankProgression.canRankUp(this);
    }

    @Override
    public boolean rankUp() {
        return RankProgression.rankUp(this);
    }

    @Override
    public void resetData() {
        this.maxChakra = NarutoConfig.maxChakra;
        this.maxStamina = NarutoConfig.maxStamina;
        this.rank = "ACADEMY_STUDENT";
        this.clan = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi");
        this.clanLocked = false;
        this.chakraExp = 0;
        this.staminaExp = 0;
        this.enemiesDefeated = 0;
        this.rogueNinjasDefeated = 0;
        this.akatsukiNinjasDefeated = 0;
        this.jutsusCastCount = 0;
        this.activeMissionId = "";
        this.missionProgress = 0;
        this.completedMissionsCount = 0;
        this.missionStatus = "NONE";
        this.unlockedJutsus = "";
        this.jutsuMastery.clear();
        this.activeJutsusFlag = 0;
        this.clearSeals();
        this.chakra = this.getMaxChakra();
        this.stamina = this.getMaxStamina();
        this.chakraRegenInfo.cooldown = 0;
    }

    @Override
    public void useChakra(float amount, int cooldown) {
        ChakraStaminaLogic.useChakra(this, amount, cooldown, this.chakraRegenInfo);
    }

    @Override
    public void useStamina(float amount, int cooldown) {
        ChakraStaminaLogic.useStamina(this, amount, cooldown, this.staminaRegenInfo);
    }

    @Override
    public void addChakra(float amount) {
        ChakraStaminaLogic.addChakra(this, amount);
    }

    @Override
    public void addStamina(float amount) {
        ChakraStaminaLogic.addStamina(this, amount);
    }

    @Override
    public void lockChakraRegen(int ticks) {
        if (ticks > 0) {
            this.chakraRegenInfo.cooldown = Math.max(this.chakraRegenInfo.cooldown, ticks);
        }
    }

    @Override
    public int getChakraRegenCooldown() {
        return this.chakraRegenInfo.cooldown;
    }

    @Override
    public boolean isChakraRegenLocked() {
        return this.chakraRegenInfo.cooldown > 0;
    }

    @Override
    public Identifier getCurrentlyChanneledAbility() {
        return this.currentlyChanneled;
    }

    @Override
    public int getCurrentlyChanneledTicks() {
        return this.ticksChanneled;
    }

    @Override
    public void setCurrentlyChanneledAbility(Player player, Ability ability) {
        if (ability != null) {
            if (ability.castingSound() != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            if (ability instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                player.sendSystemMessage(Component.translatable("jutsu.charge.start", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.translatable("jutsu.channel.start", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
            }

            this.currentlyChanneled = ability.getRegistryName();
        } else {
            if (this.currentlyChanneled != null) {
                Ability currentAbility = NarutoAbilities.getAbility(this.currentlyChanneled);
                if (currentAbility != null) {
                    if (currentAbility instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                        player.sendSystemMessage(Component.translatable("jutsu.cast", Component.translatable(currentAbility.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage(Component.translatable("jutsu.channel.stop", Component.translatable(currentAbility.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                    }
                }
            }
            this.currentlyChanneled = null;
        }
        this.ticksChanneled = 0;
    }

    @Override
    public ToggleAbilityData getToggleAbilityData() {
        if (this.toggleAbilityData == null) {
            this.toggleAbilityData = new ToggleAbilityData();
        }
        return this.toggleAbilityData;
    }

    @Override
    public void updateServerData(Player player) {
        if (player.isDeadOrDying()) {
            if (this.currentlyChanneled != null) {
                this.setCurrentlyChanneledAbility(player, null);
            }
            if (this.activeHandJutsuTicks > 0) {
                this.clearActiveHandJutsu();
            }
            this.clearSeals();
            this.activeJutsusFlag = 0;
            return;
        }

        // Tick hand seal timeout
        this.tick();

        // Countdown active hand jutsu (Rasengan/Chidori 5-second window)
        if (this.activeHandJutsuTicks > 0) {
            this.activeHandJutsuTicks--;
            if (this.activeHandJutsuTicks % 20 == 0) {
                if ("chidori".equals(this.activeHandJutsu)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            com.sekwah.narutomod.sounds.NarutoSounds.CHIDORI_LOOP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
                } else if ("rasengan".equals(this.activeHandJutsu)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            com.sekwah.narutomod.sounds.NarutoSounds.RASENGAN_LOOP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
                }
            }
            if (this.activeHandJutsuTicks == 0) {
                if ("chidori".equals(this.activeHandJutsu)) {
                    player.removeEffect(net.minecraft.world.effect.MobEffects.SPEED);
                }
                this.activeHandJutsu = "";
            }
        }

        // Zero-allocation delayed tick execution: iterate backwards to safely remove and run
        if (!this.delayedTickEvents.isEmpty()) {
            for (int i = this.delayedTickEvents.size() - 1; i >= 0; i--) {
                DelayedPlayerTickEvent event = this.delayedTickEvents.get(i);
                event.tick();
                if (event.shouldRun()) {
                    this.delayedTickEvents.remove(i);
                    event.run(player);
                }
            }
        }

        // Zero-allocation cooldown tick execution: remove directly without allocating keySet copy
        if (!this.cooldownTickEvents.isEmpty()) {
            this.cooldownTickEvents.values().removeIf(CooldownTickEvent::tickAndCheckComplete);
        }

        boolean wearingKageHat = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem()
                == com.sekwah.narutomod.item.NarutoItems.KAGE_HAT.get();
        if (this.kageHatEquipped != wearingKageHat) {
            this.setKageHatEquipped(wearingKageHat);
        }

        ChakraStaminaLogic.tickRegen(this, player, this.staminaRegenInfo, this.chakraRegenInfo);

        if (this.currentlyChanneled != null) {
            Ability ability = NarutoAbilities.getAbility(this.currentlyChanneled);
            if (ability != null && ability.activationType() == Ability.ActivationType.CHANNELED) {
                if(ability.handleCost(player, this, this.ticksChanneled)) {
                    if (ability instanceof Ability.Channeled channeled) {
                        channeled.handleChannelling(player, this, this.ticksChanneled);
                    }
                } else {
                    if (this.ticksChanneled > 0) {
                        ability.performServer(player, this, this.ticksChanneled - 1);
                    }
                    this.setCurrentlyChanneledAbility(player, null);
                }
            } else {
                NarutoMod.LOGGER.error("Somehow non channeled ability has been set to ninja data {}", this.currentlyChanneled);
                this.setCurrentlyChanneledAbility(player, null);
            }
            this.ticksChanneled++;
        }

        // Sync active abilities bitmask with ToggleAbilityData
        if (this.toggleAbilityData != null) {
            boolean hasSharingan = false;
            boolean hasByakugan = false;
            boolean hasEightGates = false;
            boolean hasWaterWalk = false;
            boolean hasEightGatesDeath = false;
            for (Identifier id : this.toggleAbilityData.getAbilitiesHashSet()) {
                String path = id.getPath();
                if ("sharingan".equals(path)) hasSharingan = true;
                else if ("byakugan".equals(path)) hasByakugan = true;
                else if ("eight_gates".equals(path)) hasEightGates = true;
                else if ("eight_gates_death".equals(path)) {
                    hasEightGates = true;
                    hasEightGatesDeath = true;
                }
                else if ("water_walk".equals(path)) hasWaterWalk = true;
            }
            setJutsuActive(JUTSU_FLAG_SHARINGAN, hasSharingan);
            setJutsuActive(JUTSU_FLAG_BYAKUGAN, hasByakugan);
            setJutsuActive(JUTSU_FLAG_EIGHT_GATES, hasEightGates);
            setJutsuActive(JUTSU_FLAG_WATER_WALK, hasWaterWalk);
            setJutsuActive(JUTSU_FLAG_EIGHT_GATES_DEATH, hasEightGatesDeath);
        }
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        NinjaDataNbtCodec.save(this, nbt);
    }

    @Override
    public void serialize(ValueOutput output) {
        final CompoundTag nbt = new CompoundTag();
        saveNBTData(nbt);
        output.store("ninjaData", CompoundTag.CODEC, nbt);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("ninjaData", CompoundTag.CODEC).ifPresent(this::loadNBTData);
    }

    @Override
    public void loadNBTData(CompoundTag compoundTag) {
        NinjaDataNbtCodec.load(this, compoundTag);
    }

    private boolean shimonPenaltyActive = false;

    @Override
    public boolean isShimonPenaltyActive() {
        return this.shimonPenaltyActive;
    }

    @Override
    public void setShimonPenaltyActive(boolean active) {
        this.shimonPenaltyActive = active;
    }

    @Override
    public String getActiveHandJutsu() {
        return this.activeHandJutsu != null ? this.activeHandJutsu : "";
    }

    @Override
    public int getActiveHandJutsuTicks() {
        return this.activeHandJutsuTicks;
    }

    @Override
    public void setActiveHandJutsu(String jutsu, int ticks) {
        this.activeHandJutsu = jutsu != null ? jutsu : "";
        this.activeHandJutsuTicks = ticks;
    }

    @Override
    public void clearActiveHandJutsu() {
        this.activeHandJutsu = "";
        this.activeHandJutsuTicks = 0;
    }

    @Override
    public int getActiveJutsusFlag() {
        return this.activeJutsusFlag;
    }

    @Override
    public void setActiveJutsusFlag(int flag) {
        this.activeJutsusFlag = flag;
    }

    @Override
    public boolean isJutsuActive(int flag) {
        return (this.activeJutsusFlag & flag) != 0;
    }

    @Override
    public void setJutsuActive(int flag, boolean active) {
        if (active) {
            this.activeJutsusFlag |= flag;
        } else {
            this.activeJutsusFlag &= ~flag;
        }
    }

    @Override
    public boolean isSharinganActive() {
        return isJutsuActive(JUTSU_FLAG_SHARINGAN);
    }

    @Override
    public void setSharinganActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_SHARINGAN, active);
    }

    @Override
    public boolean isByakuganActive() {
        return isJutsuActive(JUTSU_FLAG_BYAKUGAN);
    }

    @Override
    public void setByakuganActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_BYAKUGAN, active);
    }

    @Override
    public boolean isEightGatesActive() {
        return isJutsuActive(JUTSU_FLAG_EIGHT_GATES);
    }

    @Override
    public void setEightGatesActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_EIGHT_GATES, active);
    }

    @Override
    public boolean isWaterWalkActive() {
        return isJutsuActive(JUTSU_FLAG_WATER_WALK);
    }

    @Override
    public void setWaterWalkActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_WATER_WALK, active);
    }

    @Override
    public boolean isEighthGateActive() {
        return isJutsuActive(JUTSU_FLAG_EIGHT_GATES_DEATH);
    }

    @Override
    public void setEighthGateActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_EIGHT_GATES_DEATH, active);
    }

    @Override
    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return cooldownTickEvents;
    }

    @Override
    public List<Byte> getActiveSeals() {
        return Collections.unmodifiableList(this.activeSeals);
    }

    @Override
    public void addSeal(byte seal) {
        if (this.activeSeals.size() >= MAX_ACTIVE_SEALS) {
            this.activeSeals.remove(0);
        }
        this.activeSeals.add(seal);
        this.sealTimeout = DEFAULT_SEAL_TIMEOUT;
    }

    @Override
    public void clearSeals() {
        this.activeSeals.clear();
        this.sealTimeout = 0;
    }

    @Override
    public int getSealTimeout() {
        return this.sealTimeout;
    }

    @Override
    public void tick() {
        if (this.sealTimeout > 0) {
            this.sealTimeout--;
            if (this.sealTimeout == 0) {
                this.clearSeals();
            }
        }
    }

    /**
     * Applies death penalty (-5% normally, -25% when killed by Eighth Gate / Hachimon: Shimon)
     * without re-adding clan/rank bonuses on top of existing stats.
     */
    public static void applyDeathPenalty(INinjaData original, INinjaData future, boolean hasEighthGateToggle) {
        if (original == null || future == null) {
            return;
        }
        boolean isEighthGateDeath = original.isShimonPenaltyActive()
                || original.isEighthGateActive()
                || hasEighthGateToggle;
        float multiplier = isEighthGateDeath ? 0.75f : 0.95f;

        float newBaseMaxChakra = Math.max(100.0f, original.getBaseMaxChakra() * multiplier);
        float newBaseMaxStamina = Math.max(100.0f, original.getBaseMaxStamina() * multiplier);

        future.setBaseMaxChakra(newBaseMaxChakra);
        future.setBaseMaxStamina(newBaseMaxStamina);
        future.setChakra(future.getMaxChakra());
        future.setStamina(future.getMaxStamina());

        future.setChakraExp(original.getChakraExp() * multiplier);
        future.setStaminaExp(original.getStaminaExp() * multiplier);

        for (Map.Entry<Identifier, Integer> entry : original.getJutsuMasteryMap().entrySet()) {
            int penalized = (int) Math.floor(entry.getValue() * multiplier);
            future.setMastery(entry.getKey(), Math.max(0, penalized));
        }

        future.setShimonPenaltyActive(false);
        future.setEighthGateActive(false);
        future.setEightGatesActive(false);
        future.clearActiveHandJutsu();
        if (future.getToggleAbilityData() != null) {
            future.getToggleAbilityData().getAbilitiesHashSet().clear();
        }
    }

    @Override
    public String getActiveMissionId() {
        return this.activeMissionId != null ? this.activeMissionId : "";
    }

    @Override
    public void setActiveMissionId(String missionId) {
        this.activeMissionId = missionId != null ? missionId : "";
    }

    @Override
    public int getMissionProgress() {
        return this.missionProgress;
    }

    @Override
    public void setMissionProgress(int progress) {
        MissionLogic.setMissionProgress(this, progress);
    }

    @Override
    public int getCompletedMissionsCount() {
        return this.completedMissionsCount;
    }

    @Override
    public void setCompletedMissionsCount(int count) {
        this.completedMissionsCount = Math.max(0, count);
    }

    @Override
    public String getMissionStatus() {
        return this.missionStatus != null ? this.missionStatus : NinjaMission.MissionStatus.NONE.name();
    }

    @Override
    public void setMissionStatus(String status) {
        this.missionStatus = status != null ? status : NinjaMission.MissionStatus.NONE.name();
    }

    @Override
    public boolean acceptMission(NinjaMission mission) {
        return MissionLogic.acceptMission(this, mission);
    }

    @Override
    public boolean incrementMissionProgress(int amount) {
        return MissionLogic.incrementMissionProgress(this, amount);
    }

    @Override
    public void abandonMission() {
        MissionLogic.abandonMission(this);
    }

    @Override
    public boolean claimMissionReward() {
        return MissionLogic.claimMissionReward(this);
    }

    // --- Package-private raw accessors used by NinjaDataNbtCodec (bypass clamping/side effects) ---

    void setRawRank(String rank) { this.rank = rank; }

    void setRawChakra(float value) { this.chakra = value; }

    void setRawStamina(float value) { this.stamina = value; }

    float getRawMaxChakra() { return this.maxChakra; }

    void setRawMaxChakra(float value) { this.maxChakra = value; }

    float getRawMaxStamina() { return this.maxStamina; }

    void setRawMaxStamina(float value) { this.maxStamina = value; }

    void setRawClan(Identifier clan) { this.clan = clan; }

    List<Byte> getMutableActiveSeals() { return this.activeSeals; }

    void setRawSealTimeout(int value) { this.sealTimeout = value; }

    void setRawMissionProgress(int value) { this.missionProgress = value; }

    void setRawChakraRegenCooldown(int value) { this.chakraRegenInfo.cooldown = value; }

    void setRawChakraExp(float value) { this.chakraExp = value; }

    void setRawStaminaExp(float value) { this.staminaExp = value; }

    int getRawEnemiesDefeated() { return this.enemiesDefeated; }

    int getRawJutsusCastCount() { return this.jutsusCastCount; }
}
