package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.sekclib.capabilitysync.annotation.Sync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class NinjaData implements INinjaData {

    @Sync(minTicks = 2)
    private float chakra;

    @Sync(minTicks = 2)
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

    @Sync(minTicks = 5)
    private int activeHandJutsuTicks = 0;

    @Sync(minTicks = 1, syncGlobally = true)
    private int activeJutsusFlag = 0;

    @Sync
    private boolean kageHatEquipped = false;

    @Sync
    private String equippedJutsus = ";;;;;;;";

    @Sync
    private int selectedJutsuSlot = 0;

    private final NinjaTickScheduler tickScheduler = new NinjaTickScheduler();

    private transient Set<Identifier> cachedUnlockedAbilities;
    private transient String cachedUnlockedJutsusSource;

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
            this.equippedJutsus = ";;;;;;;";
            this.selectedJutsuSlot = 0;
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
        return NinjaMasteryHelper.isAbilityUnlocked(this, ability);
    }

    public static boolean isScrollRequiredJutsu(Identifier id) {
        return NinjaMasteryHelper.isScrollRequiredJutsu(id);
    }

    public static boolean isClanRestrictedJutsu(Identifier id) {
        return NinjaMasteryHelper.isClanRestrictedJutsu(id);
    }

    @Override
    public boolean isAbilityUnlocked(Identifier abilityId) {
        return NinjaMasteryHelper.isAbilityUnlocked(this, abilityId);
    }

    @Override
    public void unlockAbility(Identifier abilityId) {
        this.unlockedJutsus = NinjaMasteryHelper.unlockAbility(this.unlockedJutsus, abilityId);
    }

    @Override
    public void lockAbility(Identifier abilityId) {
        this.unlockedJutsus = NinjaMasteryHelper.lockAbility(this, this.unlockedJutsus, abilityId);
    }

    @Override
    public Set<Identifier> getUnlockedAbilities() {
        String source = this.unlockedJutsus != null ? this.unlockedJutsus : "";
        if (this.cachedUnlockedAbilities == null || !source.equals(this.cachedUnlockedJutsusSource)) {
            this.cachedUnlockedAbilities = NinjaMasteryHelper.parseUnlockedAbilities(source);
            this.cachedUnlockedJutsusSource = source;
        }
        return this.cachedUnlockedAbilities;
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
        return NinjaMasteryHelper.getMastery(this.jutsuMastery, jutsuId);
    }

    @Override
    public void setMastery(Identifier jutsuId, int amount) {
        NinjaMasteryHelper.setMastery(this.jutsuMastery, jutsuId, amount);
    }

    @Override
    public void addMastery(Identifier jutsuId, int amount) {
        int before = getMastery(jutsuId);
        NinjaMasteryHelper.addMastery(this.jutsuMastery, jutsuId, amount);
        checkMasterJutsuMissionProgress(before, getMastery(jutsuId));
    }

    @Override
    public void incrementJutsuMastery(Identifier jutsuId, boolean isToggle) {
        int before = getMastery(jutsuId);
        NinjaMasteryHelper.incrementJutsuMastery(this.jutsuMastery, jutsuId, isToggle);
        checkMasterJutsuMissionProgress(before, getMastery(jutsuId));
    }

    private void checkMasterJutsuMissionProgress(int beforeMastery, int afterMastery) {
        if (afterMastery <= beforeMastery) {
            return;
        }
        int tierBefore = NinjaMasteryHelper.getMasteryTier(beforeMastery);
        int tierAfter = NinjaMasteryHelper.getMasteryTier(afterMastery);
        if (tierAfter > tierBefore) {
            MissionDefinition active = MissionDataManager.get(this.getActiveMissionId());
            if (active != null
                    && active.targetType() == NinjaMission.TargetType.MASTER_JUTSU
                    && NinjaMission.MissionStatus.fromString(this.getMissionStatus()) == NinjaMission.MissionStatus.IN_PROGRESS) {
                this.incrementMissionProgress(1);
            }
        }
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
        return NinjaMasteryHelper.getEnemiesDefeated(this.getRank(), this.enemiesDefeated);
    }

    @Override
    public void addEnemyDefeated() {
        NinjaMasteryHelper.addEnemyDefeated(this);
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
        return NinjaMasteryHelper.getJutsusCastCount(this.getRank(), this.jutsusCastCount);
    }

    @Override
    public void addJutsuCast() {
        NinjaMasteryHelper.addJutsuCast(this);
    }

    @Override
    public void setJutsusCastCount(int count) {
        this.jutsusCastCount = count;
    }

    @Override
    public boolean canRankUp() {
        return NinjaMasteryHelper.canRankUp(this);
    }

    @Override
    public boolean rankUp() {
        return NinjaMasteryHelper.rankUp(this);
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
        this.equippedJutsus = ";;;;;;;";
        this.selectedJutsuSlot = 0;
        this.jutsuMastery.clear();
        this.activeJutsusFlag = 0;
        if (this.toggleAbilityData != null) {
            this.toggleAbilityData.clear();
        }
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
        NinjaChannelAndEquipmentHelper.setCurrentlyChanneledAbility(this, player, ability);
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
        NinjaChannelAndEquipmentHelper.updateServerData(
                this, player, this.tickScheduler, this.staminaRegenInfo, this.chakraRegenInfo);
    }

    public void updateClientData() {
        if (this.activeHandJutsuTicks > 0) {
            this.activeHandJutsuTicks--;
        }
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.tickScheduler.scheduleDelayedTickEvent(consumer, tickDelay);
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        NinjaDataSerializer.serializeNBT(this, nbt);
    }

    @Override
    public void serialize(ValueOutput output) {
        NinjaDataSerializer.serialize(this, output);
    }

    @Override
    public void deserialize(ValueInput input) {
        NinjaDataSerializer.deserialize(this, input);
    }

    @Override
    public void loadNBTData(CompoundTag compoundTag) {
        NinjaDataSerializer.deserializeNBT(this, compoundTag);
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
        this.activeJutsusFlag = NinjaChannelAndEquipmentHelper.setJutsuActiveFlag(this.activeJutsusFlag, flag, active);
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
    public boolean isSageModeActive() {
        return isJutsuActive(JUTSU_FLAG_SAGE_MODE);
    }

    @Override
    public void setSageModeActive(boolean active) {
        setJutsuActive(JUTSU_FLAG_SAGE_MODE, active);
    }

    @Override
    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return this.tickScheduler.getCooldownEvents();
    }

    @Override
    @javax.annotation.Nullable
    public Identifier getEquippedJutsu(int slot) {
        return NinjaChannelAndEquipmentHelper.getEquippedJutsu(this.equippedJutsus, slot);
    }

    @Override
    public void setEquippedJutsu(int slot, @javax.annotation.Nullable Identifier abilityId) {
        this.equippedJutsus = NinjaChannelAndEquipmentHelper.setEquippedJutsu(this.equippedJutsus, slot, abilityId);
    }

    @Override
    public java.util.List<Identifier> getEquippedJutsusList() {
        return NinjaChannelAndEquipmentHelper.getEquippedJutsusList(this.equippedJutsus);
    }

    @Override
    public String getEquippedJutsusString() {
        return this.equippedJutsus != null ? this.equippedJutsus : ";;;;;;;";
    }

    @Override
    public void setEquippedJutsusString(String serialized) {
        this.equippedJutsus = NinjaChannelAndEquipmentHelper.normalizeEquippedJutsusString(serialized);
    }

    @Override
    public int getSelectedJutsuSlot() {
        return NinjaChannelAndEquipmentHelper.clampSelectedJutsuSlot(this.selectedJutsuSlot);
    }

    @Override
    public void setSelectedJutsuSlot(int slot) {
        this.selectedJutsuSlot = NinjaChannelAndEquipmentHelper.clampSelectedJutsuSlot(slot);
    }

    @Override
    @javax.annotation.Nullable
    public Identifier getSelectedJutsuId() {
        return getEquippedJutsu(getSelectedJutsuSlot());
    }

    /**
     * Applies death penalty (-5% normally, -25% when killed by Eighth Gate / Hachimon: Shimon)
     * without re-adding clan/rank bonuses on top of existing stats.
     */
    public static void applyDeathPenalty(INinjaData original, INinjaData future, boolean hasEighthGateToggle) {
        NinjaMasteryHelper.applyDeathPenalty(original, future, hasEighthGateToggle);
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

    // --- Package-private raw accessors used by NinjaDataSerializer (bypass clamping/side effects) ---

    void setRawRank(String rank) { this.rank = rank; }

    void setRawChakra(float value) { this.chakra = value; }

    void setRawStamina(float value) { this.stamina = value; }

    float getRawMaxChakra() { return this.maxChakra; }

    void setRawMaxChakra(float value) { this.maxChakra = value; }

    float getRawMaxStamina() { return this.maxStamina; }

    void setRawMaxStamina(float value) { this.maxStamina = value; }

    void setRawClan(Identifier clan) { this.clan = clan; }

    void setRawMissionProgress(int value) { this.missionProgress = value; }

    void setRawChakraRegenCooldown(int value) { this.chakraRegenInfo.cooldown = value; }

    void setRawChakraExp(float value) { this.chakraExp = value; }

    void setRawStaminaExp(float value) { this.staminaExp = value; }

    int getRawEnemiesDefeated() { return this.enemiesDefeated; }

    int getRawJutsusCastCount() { return this.jutsusCastCount; }

    void setRawCurrentlyChanneled(Identifier abilityId) { this.currentlyChanneled = abilityId; }

    void setRawTicksChanneled(int ticks) { this.ticksChanneled = ticks; }

    void setRawActiveHandJutsuTicks(int ticks) { this.activeHandJutsuTicks = ticks; }

    ToggleAbilityData getRawToggleAbilityData() { return this.toggleAbilityData; }
}
