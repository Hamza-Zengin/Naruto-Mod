package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.sekclib.capabilitysync.capabilitysync.annotation.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NinjaData implements INinjaData, ICapabilityProvider {

    @Sync(minTicks = 1)
    private float chakra;

    @Sync(minTicks = 1)
    private float stamina;

    @Sync
    private float maxChakra;

    @Sync
    private float maxStamina;

    @Sync
    private ResourceLocation clan;

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
    private Map<ResourceLocation, Integer> jutsuMastery = new HashMap<>();

    private boolean creativeMode = false;

    @Sync(minTicks = 1)
    private ResourceLocation currentlyChanneled;

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

    public static final int DEFAULT_SEAL_TIMEOUT = 60;
    public static final int MAX_ACTIVE_SEALS = 12;

    private final List<Byte> activeSeals = new ArrayList<>();
    private int sealTimeout = 0;

    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.maxChakra = NarutoConfig.maxChakra > 0 ? NarutoConfig.maxChakra : 100.0f;
            this.maxStamina = NarutoConfig.maxStamina > 0 ? NarutoConfig.maxStamina : 100.0f;
            this.chakra = this.getMaxChakra();
            this.stamina = this.getMaxStamina();
            this.clan = new ResourceLocation(NarutoMod.MOD_ID, "shinobi");
            this.clanLocked = false;
            this.unlockedJutsus = "";
            this.rank = "ACADEMY_STUDENT";
            this.chakraExp = 0;
            this.staminaExp = 0;
            this.activeJutsusFlag = 0;
        }
        this.toggleAbilityData = new ToggleAbilityData();
    }

    class RegenInfo {
        public float regenRate;
        public int cooldown;

        public RegenInfo(float regenRate) {
            this.regenRate = regenRate;
        }

        public boolean canRegen() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return true;
        }
    }

    private RegenInfo chakraRegenInfo = new RegenInfo(NarutoConfig.chakraRegenRate > 0 ? NarutoConfig.chakraRegenRate : 0.05f);
    private RegenInfo staminaRegenInfo = new RegenInfo(NarutoConfig.staminaRegenRate > 0 ? NarutoConfig.staminaRegenRate : 0.4f);

    private static final String CHAKRA_TAG = "chakra";
    private static final String STAMINA_TAG = "stamina";
    private static final String MAX_CHAKRA_TAG = "maxChakra";
    private static final String MAX_STAMINA_TAG = "maxStamina";
    private static final String CLAN_TAG = "clan";
    private static final String CLAN_LOCKED_TAG = "clanLocked";
    private static final String RANK_TAG = "rank";
    private static final String CHAKRA_EXP_TAG = "chakraExp";
    private static final String STAMINA_EXP_TAG = "staminaExp";
    private static final String ENEMIES_DEFEATED_TAG = "enemiesDefeated";
    private static final String ROGUE_NINJAS_DEFEATED_TAG = "rogueNinjasDefeated";
    private static final String AKATSUKI_NINJAS_DEFEATED_TAG = "akatsukiNinjasDefeated";
    private static final String JUTSUS_CAST_TAG = "jutsusCast";
    private static final String UNLOCKED_JUTSUS_TAG = "unlockedJutsus";
    private static final String ACTIVE_HAND_JUTSU_TAG = "activeHandJutsu";
    private static final String ACTIVE_HAND_JUTSU_TICKS_TAG = "activeHandJutsuTicks";
    private static final String ACTIVE_JUTSUS_FLAG_TAG = "activeJutsusFlag";
    private static final String ACTIVE_SEALS_TAG = "activeSeals";
    private static final String SEAL_TIMEOUT_TAG = "sealTimeout";
    private static final String JUTSU_MASTERY_TAG = "jutsuMastery";
    private static final String CHAKRA_REGEN_COOLDOWN_TAG = "chakraRegenCooldown";

    private final LazyOptional<INinjaData> holder = LazyOptional.of(() -> this);

    @Override
    public float getChakra() {
        return this.chakra;
    }

    @Override
    public float getMaxChakra() {
        NinjaRank currentRank = NinjaRank.fromString(this.rank);
        float total = this.maxChakra + currentRank.getChakraBonus();
        if (this.clan != null && this.clan.getPath().equals("uzumaki")) {
            total *= 1.25f; // Uzumaki clan's renowned boundless chakra reserve
        }
        return total;
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
        NinjaRank currentRank = NinjaRank.fromString(this.rank);
        return this.maxStamina + currentRank.getStaminaBonus();
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
        this.maxChakra = Math.min(Math.max(100f, amount), 500f);
        float max = this.getMaxChakra();
        if (this.chakra > max) this.chakra = max;
    }

    @Override
    public void setBaseMaxStamina(float amount) {
        this.maxStamina = Math.min(Math.max(100f, amount), 400f);
        float max = this.getMaxStamina();
        if (this.stamina > max) this.stamina = max;
    }

    @Override
    public ResourceLocation getClan() {
        return this.clan;
    }

    @Override
    public void setClan(ResourceLocation clan) {
        // Clan cannot be changed once locked by player
        if (this.clanLocked) {
            return;
        }
        this.clan = clan;
    }

    @Override
    public void forceSetClan(ResourceLocation clan) {
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
        return checkAbilityUnlocked(ability, ability.getRegistryName());
    }

    public static boolean isScrollRequiredJutsu(ResourceLocation id) {
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("rasengan") || path.equals("chidori") || path.equals("shadow_clone")
                || path.equals("kirin") || path.equals("amaterasu") || path.equals("susanoo_ribcage")
                || path.equals("eight_gates") || path.equals("eight_gates_death");
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation abilityId) {
        if (abilityId == null) return false;
        Ability ability = null;
        try {
            ability = NarutoAbilities.getAbility(abilityId);
        } catch (Throwable ignored) {
            // NarutoAbilities or Forge registry may not be initialized in standalone unit tests
        }
        if (ability == null) {
            return getUnlockedAbilities().contains(abilityId);
        }
        return checkAbilityUnlocked(ability, abilityId);
    }

    private boolean checkAbilityUnlocked(Ability ability, ResourceLocation abilityId) {
        if (ability == null) return false;

        // If ability specifies prerequisites and they are not met, it cannot be unlocked
        if (!ability.getPrerequisites().isEmpty() && !ability.meetsPrerequisites(this)) {
            return false;
        }

        // Basic utility always unlocked
        if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) {
            return true;
        }

        // Innate Clan Dōjutsu (Awakens upon reaching Genin rank)
        if (abilityId != null) {
            if (abilityId.getPath().equals("sharingan")) {
                NinjaRank playerRank = NinjaRank.fromString(this.getRank());
                return this.clan != null && this.clan.getPath().equals("uchiha") && playerRank.getLevel() >= NinjaRank.GENIN.getLevel();
            }
            if (abilityId.getPath().equals("byakugan")) {
                NinjaRank playerRank = NinjaRank.fromString(this.getRank());
                return this.clan != null && this.clan.getPath().equals("hyuga") && playerRank.getLevel() >= NinjaRank.GENIN.getLevel();
            }

            // Scroll-only advanced & secret jutsus
            if (isScrollRequiredJutsu(abilityId)) {
                // Mangekyo jutsus strictly require Uchiha bloodline
                if (abilityId.getPath().equals("amaterasu") || abilityId.getPath().equals("susanoo_ribcage")) {
                    if (this.clan == null || !this.clan.getPath().equals("uchiha")) {
                        return false;
                    }
                }
                // Eight Gates also requires Jonin rank to withstand the forbidden strain
                if (abilityId.getPath().equals("eight_gates")) {
                    NinjaRank playerRank = NinjaRank.fromString(this.getRank());
                    if (playerRank.getLevel() < NinjaRank.JONIN.getLevel()) {
                        return false;
                    }
                }
                // Unlocked explicitly via scroll or training
                if (getUnlockedAbilities().contains(abilityId)) {
                    return true;
                }
                // If jutsu has prerequisites (like Susanoo requiring Mangekyou), rank progression can awaken it
                if (!ability.getPrerequisites().isEmpty()) {
                    NinjaRank playerRank = NinjaRank.fromString(this.getRank());
                    if (playerRank.getLevel() >= ability.getRequiredRank().getLevel()) {
                        return true;
                    }
                }
                return false;
            }
        }

        // Check prerequisites if jutsu defines any
        if (!ability.getPrerequisites().isEmpty()) {
            if (!ability.meetsPrerequisites(this)) {
                return false;
            }
            // Once prerequisites are met, unlock if player meets required rank or has explicitly unlocked it
            NinjaRank playerRank = NinjaRank.fromString(this.getRank());
            if (playerRank.getLevel() >= ability.getRequiredRank().getLevel() || (abilityId != null && getUnlockedAbilities().contains(abilityId))) {
                return true;
            }
        }

        // Unlocked explicitly via scroll or training
        if (abilityId != null && getUnlockedAbilities().contains(abilityId)) {
            return true;
        }

        // Check if player rank meets or exceeds requirement for standard elemental/taijutsu abilities
        NinjaRank playerRank = NinjaRank.fromString(this.getRank());
        if (playerRank.getLevel() >= ability.getRequiredRank().getLevel()) {
            return true;
        }

        return false;
    }

    @Override
    public void unlockAbility(ResourceLocation abilityId) {
        if (abilityId == null) return;
        java.util.Set<ResourceLocation> unlocked = getUnlockedAbilities();
        if (!unlocked.contains(abilityId)) {
            if (this.unlockedJutsus == null || this.unlockedJutsus.isEmpty()) {
                this.unlockedJutsus = abilityId.toString();
            } else {
                this.unlockedJutsus = this.unlockedJutsus + "," + abilityId.toString();
            }
        }
    }

    @Override
    public void lockAbility(ResourceLocation abilityId) {
        if (abilityId == null) return;
        java.util.Set<ResourceLocation> unlocked = getUnlockedAbilities();
        if (unlocked.remove(abilityId)) {
            StringBuilder sb = new StringBuilder();
            for (ResourceLocation loc : unlocked) {
                if (sb.length() > 0) sb.append(",");
                sb.append(loc.toString());
            }
            this.unlockedJutsus = sb.toString();
        }
    }

    @Override
    public java.util.Set<ResourceLocation> getUnlockedAbilities() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        if (this.unlockedJutsus != null && !this.unlockedJutsus.isEmpty()) {
            String[] split = this.unlockedJutsus.split(",");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    set.add(new ResourceLocation(s.trim()));
                }
            }
        }
        return set;
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
    public Map<ResourceLocation, Integer> getJutsuMasteryMap() {
        return this.jutsuMastery;
    }

    @Override
    public int getMastery(ResourceLocation jutsuId) {
        if (jutsuId == null) return 0;
        return this.jutsuMastery.getOrDefault(jutsuId, 0);
    }

    @Override
    public void addMastery(ResourceLocation jutsuId, int amount) {
        if (jutsuId == null || amount == 0) return;
        int current = getMastery(jutsuId);
        int updated = Math.max(0, current + amount);
        this.jutsuMastery.put(jutsuId, updated);
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
        if (this.maxChakra >= 500f) {
            this.chakraExp = 0;
            return;
        }
        this.chakraExp += amount;
        float neededExp;
        if (this.maxChakra < 150.0f) {
            neededExp = 30.0f + (this.maxChakra - 100.0f) * 0.50f;
        } else {
            neededExp = 55.0f + (this.maxChakra - 150.0f) * 1.10f;
        }
        if (this.chakraExp >= neededExp && this.maxChakra < 500f) {
            this.chakraExp -= neededExp;
            this.maxChakra = Math.min(this.maxChakra + 5.0f, 500.0f);
            if (this.chakra > this.getMaxChakra()) this.chakra = this.getMaxChakra();
        }
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
        if (this.maxStamina >= 400f) {
            this.staminaExp = 0;
            return;
        }
        this.staminaExp += amount;
        float neededExp = 50.0f + (this.maxStamina - 100.0f) * 1.10f;
        if (this.staminaExp >= neededExp && this.maxStamina < 400f) {
            this.staminaExp -= neededExp;
            this.maxStamina = Math.min(this.maxStamina + 5.0f, 400.0f);
            if (this.stamina > this.getMaxStamina()) this.stamina = this.getMaxStamina();
        }
    }

    @Override
    public int getEnemiesDefeated() {
        NinjaRank current = NinjaRank.fromString(this.getRank());
        if (current == NinjaRank.GENIN) {
            return Math.min(this.enemiesDefeated, 25);
        } else if (current == NinjaRank.CHUNIN) {
            return Math.min(this.enemiesDefeated, 75);
        } else if (current == NinjaRank.JONIN) {
            return Math.min(this.enemiesDefeated, 150);
        } else if (current == NinjaRank.ANBU) {
            return Math.min(this.enemiesDefeated, 300);
        }
        return this.enemiesDefeated;
    }

    @Override
    public void addEnemyDefeated() {
        NinjaRank current = NinjaRank.fromString(this.getRank());
        if (current == NinjaRank.GENIN) {
            if (this.enemiesDefeated < 25) this.enemiesDefeated++;
        } else if (current == NinjaRank.CHUNIN) {
            if (this.enemiesDefeated < 75) this.enemiesDefeated++;
        } else if (current == NinjaRank.JONIN) {
            if (this.enemiesDefeated < 150) this.enemiesDefeated++;
        } else if (current == NinjaRank.ANBU) {
            if (this.enemiesDefeated < 300) this.enemiesDefeated++;
        } else {
            this.enemiesDefeated++;
        }
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
        NinjaRank current = NinjaRank.fromString(this.getRank());
        if (current == NinjaRank.ACADEMY_STUDENT) {
            return Math.min(this.jutsusCastCount, 10);
        }
        return this.jutsusCastCount;
    }

    @Override
    public void addJutsuCast() {
        NinjaRank current = NinjaRank.fromString(this.getRank());
        if (current == NinjaRank.ACADEMY_STUDENT) {
            if (this.jutsusCastCount < 10) {
                this.jutsusCastCount++;
            }
        } else {
            this.jutsusCastCount++;
        }
    }

    @Override
    public void setJutsusCastCount(int count) {
        this.jutsusCastCount = count;
    }

    @Override
    public boolean canRankUp() {
        return NinjaRank.fromString(this.getRank()).canRankUp(this);
    }

    @Override
    public boolean rankUp() {
        if (!canRankUp()) return false;
        NinjaRank current = NinjaRank.fromString(this.getRank());
        NinjaRank next = current.next();
        if (next == null) return false;

        setRank(next.name());
        this.jutsusCastCount = 0;
        this.enemiesDefeated = 0;
        // Refill to new higher maximum stats upon achieving new rank
        this.chakra = this.getMaxChakra();
        this.stamina = this.getMaxStamina();
        return true;
    }

    @Override
    public void resetData() {
        this.maxChakra = NarutoConfig.maxChakra;
        this.maxStamina = NarutoConfig.maxStamina;
        this.rank = "ACADEMY_STUDENT";
        this.clan = new ResourceLocation(NarutoMod.MOD_ID, "shinobi");
        this.clanLocked = false;
        this.chakraExp = 0;
        this.staminaExp = 0;
        this.enemiesDefeated = 0;
        this.rogueNinjasDefeated = 0;
        this.akatsukiNinjasDefeated = 0;
        this.jutsusCastCount = 0;
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
        if (!this.creativeMode) {
            this.chakra = Math.max(0f, this.chakra - amount);
        }
        this.chakraRegenInfo.cooldown = Math.max(cooldown, this.chakraRegenInfo.cooldown);
        addChakraExp(amount * 0.10f);
    }

    @Override
    public void useStamina(float amount, int cooldown) {
        if (!this.creativeMode) {
            this.stamina = Math.max(0f, this.stamina - amount);
        }
        this.staminaRegenInfo.cooldown = Math.max(cooldown, this.staminaRegenInfo.cooldown);
        addStaminaExp(amount * 0.10f);
    }

    @Override
    public void addChakra(float amount) {
        this.chakra = Math.min(Math.max(this.chakra + amount, 0), this.getMaxChakra());
    }

    @Override
    public void addStamina(float amount) {
        this.stamina = Math.min(Math.max(this.stamina + amount, 0), this.getMaxStamina());
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
    public ResourceLocation getCurrentlyChanneledAbility() {
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
                    player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
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

        net.minecraft.world.item.Item chestItem = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem();

        if (this.staminaRegenInfo.canRegen()) {
            float staminaRegen = NarutoConfig.staminaRegenRate > 0 ? NarutoConfig.staminaRegenRate : staminaRegenInfo.regenRate;
            if (this.clan != null && this.clan.getPath().equals("nara")) {
                staminaRegen *= 1.3f;
            }
            if (chestItem == com.sekwah.narutomod.item.NarutoItems.CHUNIN_VEST.get()) {
                staminaRegen *= 1.25f;
            } else if (chestItem == com.sekwah.narutomod.item.NarutoItems.ANBU_ARMOR.get()) {
                staminaRegen *= 1.15f;
            }
            this.stamina += staminaRegen;
        }

        if (this.chakraRegenInfo.canRegen()) {
            float chakraRegen = NarutoConfig.chakraRegenRate > 0 ? NarutoConfig.chakraRegenRate : chakraRegenInfo.regenRate;
            if (this.clan != null && this.clan.getPath().equals("uzumaki")) {
                chakraRegen *= 2.0f;
            } else if (this.clan != null && this.clan.getPath().equals("senju")) {
                chakraRegen *= 1.4f;
            }
            if (chestItem == com.sekwah.narutomod.item.NarutoItems.AKATSUKI_CLOAK.get()) {
                chakraRegen *= 1.35f;
            } else if (chestItem == com.sekwah.narutomod.item.NarutoItems.ANBU_ARMOR.get()) {
                chakraRegen *= 1.15f;
            }
            this.chakra += chakraRegen;
        }

        // Senju Clan natural vitality regeneration
        if (this.clan != null && this.clan.getPath().equals("senju") && player.tickCount % 80 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }
        float curMaxStamina = this.getMaxStamina();
        float curMaxChakra = this.getMaxChakra();
        this.stamina = Math.min(Math.max(this.stamina, 0), curMaxStamina);
        this.chakra = Math.min(Math.max(this.chakra, 0), curMaxChakra);

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
            for (ResourceLocation id : this.toggleAbilityData.getAbilitiesHashSet()) {
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
        if (nbt == null) return;
        nbt.putFloat(CHAKRA_TAG, this.chakra);
        nbt.putFloat(STAMINA_TAG, this.stamina);
        nbt.putFloat(MAX_CHAKRA_TAG, this.maxChakra);
        nbt.putFloat(MAX_STAMINA_TAG, this.maxStamina);
        if (this.clan != null) {
            nbt.putString(CLAN_TAG, this.clan.toString());
        }
        nbt.putBoolean(CLAN_LOCKED_TAG, this.clanLocked);
        nbt.putString(RANK_TAG, this.rank != null ? this.rank : "ACADEMY_STUDENT");
        nbt.putFloat(CHAKRA_EXP_TAG, this.chakraExp);
        nbt.putFloat(STAMINA_EXP_TAG, this.staminaExp);
        nbt.putInt(ENEMIES_DEFEATED_TAG, this.enemiesDefeated);
        nbt.putInt(ROGUE_NINJAS_DEFEATED_TAG, this.rogueNinjasDefeated);
        nbt.putInt(AKATSUKI_NINJAS_DEFEATED_TAG, this.akatsukiNinjasDefeated);
        nbt.putInt(JUTSUS_CAST_TAG, this.jutsusCastCount);
        if (this.unlockedJutsus != null) {
            nbt.putString(UNLOCKED_JUTSUS_TAG, this.unlockedJutsus);
        }
        nbt.putString(ACTIVE_HAND_JUTSU_TAG, this.activeHandJutsu != null ? this.activeHandJutsu : "");
        nbt.putInt(ACTIVE_HAND_JUTSU_TICKS_TAG, this.activeHandJutsuTicks);
        nbt.putInt(ACTIVE_JUTSUS_FLAG_TAG, this.activeJutsusFlag);
        byte[] seals = new byte[this.activeSeals.size()];
        for (int i = 0; i < this.activeSeals.size(); i++) {
            seals[i] = this.activeSeals.get(i);
        }
        nbt.putByteArray(ACTIVE_SEALS_TAG, seals);
        nbt.putInt(SEAL_TIMEOUT_TAG, this.sealTimeout);
        nbt.putInt(CHAKRA_REGEN_COOLDOWN_TAG, this.chakraRegenInfo.cooldown);

        CompoundTag masteryTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : this.jutsuMastery.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                masteryTag.putInt(entry.getKey().toString(), entry.getValue());
            }
        }
        nbt.put(JUTSU_MASTERY_TAG, masteryTag);
    }

    @Override
    public Tag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void loadNBTData(CompoundTag compoundTag) {
        if (compoundTag == null) return;
        this.chakra = compoundTag.getFloat(CHAKRA_TAG);
        this.stamina = compoundTag.getFloat(STAMINA_TAG);
        if (compoundTag.contains(MAX_CHAKRA_TAG)) {
            this.maxChakra = compoundTag.getFloat(MAX_CHAKRA_TAG);
        }
        if (compoundTag.contains(MAX_STAMINA_TAG)) {
            this.maxStamina = compoundTag.getFloat(MAX_STAMINA_TAG);
        }
        if (compoundTag.contains(CLAN_TAG)) {
            this.clan = new ResourceLocation(compoundTag.getString(CLAN_TAG));
        }
        if (compoundTag.contains(CLAN_LOCKED_TAG)) {
            this.clanLocked = compoundTag.getBoolean(CLAN_LOCKED_TAG);
        }
        if (compoundTag.contains(RANK_TAG)) {
            this.rank = compoundTag.getString(RANK_TAG);
        } else {
            this.rank = "ACADEMY_STUDENT";
        }
        if (compoundTag.contains(CHAKRA_EXP_TAG)) {
            this.chakraExp = compoundTag.getFloat(CHAKRA_EXP_TAG);
        }
        if (compoundTag.contains(STAMINA_EXP_TAG)) {
            this.staminaExp = compoundTag.getFloat(STAMINA_EXP_TAG);
        }
        if (compoundTag.contains(ENEMIES_DEFEATED_TAG)) {
            this.enemiesDefeated = compoundTag.getInt(ENEMIES_DEFEATED_TAG);
        }
        if (compoundTag.contains(ROGUE_NINJAS_DEFEATED_TAG)) {
            this.rogueNinjasDefeated = compoundTag.getInt(ROGUE_NINJAS_DEFEATED_TAG);
        }
        if (compoundTag.contains(AKATSUKI_NINJAS_DEFEATED_TAG)) {
            this.akatsukiNinjasDefeated = compoundTag.getInt(AKATSUKI_NINJAS_DEFEATED_TAG);
        }
        if (compoundTag.contains(JUTSUS_CAST_TAG)) {
            this.jutsusCastCount = compoundTag.getInt(JUTSUS_CAST_TAG);
        }
        NinjaRank current = NinjaRank.fromString(this.rank);
        if (current == NinjaRank.ACADEMY_STUDENT) {
            if (this.jutsusCastCount > 10) this.jutsusCastCount = 10;
        } else if (current == NinjaRank.GENIN) {
            if (this.enemiesDefeated > 25) this.enemiesDefeated = 25;
        } else if (current == NinjaRank.CHUNIN) {
            if (this.enemiesDefeated > 75) this.enemiesDefeated = 75;
        } else if (current == NinjaRank.JONIN) {
            if (this.enemiesDefeated > 150) this.enemiesDefeated = 150;
        } else if (current == NinjaRank.ANBU) {
            if (this.enemiesDefeated > 300) this.enemiesDefeated = 300;
        }
        if (compoundTag.contains(UNLOCKED_JUTSUS_TAG)) {
            this.unlockedJutsus = compoundTag.getString(UNLOCKED_JUTSUS_TAG);
        }
        if (compoundTag.contains(ACTIVE_HAND_JUTSU_TAG)) {
            this.activeHandJutsu = compoundTag.getString(ACTIVE_HAND_JUTSU_TAG);
        }
        if (compoundTag.contains(ACTIVE_HAND_JUTSU_TICKS_TAG)) {
            this.activeHandJutsuTicks = compoundTag.getInt(ACTIVE_HAND_JUTSU_TICKS_TAG);
        }
        if (compoundTag.contains(ACTIVE_JUTSUS_FLAG_TAG)) {
            this.activeJutsusFlag = compoundTag.getInt(ACTIVE_JUTSUS_FLAG_TAG);
        }
        if (compoundTag.contains(ACTIVE_SEALS_TAG)) {
            this.activeSeals.clear();
            byte[] seals = compoundTag.getByteArray(ACTIVE_SEALS_TAG);
            for (byte b : seals) {
                this.activeSeals.add(b);
            }
        }
        if (compoundTag.contains(SEAL_TIMEOUT_TAG)) {
            this.sealTimeout = compoundTag.getInt(SEAL_TIMEOUT_TAG);
        }
        if (compoundTag.contains(CHAKRA_REGEN_COOLDOWN_TAG)) {
            this.chakraRegenInfo.cooldown = compoundTag.getInt(CHAKRA_REGEN_COOLDOWN_TAG);
        }
        if (compoundTag.contains(JUTSU_MASTERY_TAG, Tag.TAG_COMPOUND)) {
            this.jutsuMastery.clear();
            CompoundTag masteryTag = compoundTag.getCompound(JUTSU_MASTERY_TAG);
            for (String key : masteryTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    this.jutsuMastery.put(id, masteryTag.getInt(key));
                }
            }
        }
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            loadNBTData(compoundTag);
        }
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

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return NinjaCapabilityHandler.NINJA_DATA.orEmpty(cap, holder);
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
}
