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
import java.util.HashMap;
import java.util.Iterator;
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
    private int jutsusCastCount = 0;

    @Sync
    private String unlockedJutsus = "";

    private boolean creativeMode = false;

    @Sync(minTicks = 1)
    private ResourceLocation currentlyChanneled;

    @Sync(minTicks = 1)
    private int ticksChanneled;

    @Sync(minTicks = 1, syncGlobally = true)
    private ToggleAbilityData toggleAbilityData;

    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.maxChakra = NarutoConfig.maxChakra;
            this.maxStamina = NarutoConfig.maxStamina;
            this.chakra = this.getMaxChakra();
            this.stamina = this.getMaxStamina();
            this.clan = new ResourceLocation(NarutoMod.MOD_ID, "shinobi");
            this.clanLocked = false;
            this.unlockedJutsus = "";
            this.rank = "ACADEMY_STUDENT";
            this.chakraExp = 0;
            this.staminaExp = 0;
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

    private RegenInfo chakraRegenInfo = new RegenInfo(0.05f);
    private RegenInfo staminaRegenInfo = new RegenInfo(0.4f);

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
    private static final String JUTSUS_CAST_TAG = "jutsusCast";
    private static final String UNLOCKED_JUTSUS_TAG = "unlockedJutsus";

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
        return isAbilityUnlocked(ability.getRegistryName());
    }

    public static boolean isScrollRequiredJutsu(ResourceLocation id) {
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("rasengan") || path.equals("chidori") || path.equals("shadow_clone")
                || path.equals("kirin") || path.equals("amaterasu") || path.equals("susanoo_ribcage")
                || path.equals("eight_gates");
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation abilityId) {
        if (abilityId == null) return false;
        Ability ability = NarutoAbilities.getAbility(abilityId);
        if (ability == null) return false;

        // Basic utility always unlocked
        if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) {
            return true;
        }

        // Innate Clan Dōjutsu
        if (abilityId.getPath().equals("sharingan")) {
            return this.clan != null && this.clan.getPath().equals("uchiha");
        }
        if (abilityId.getPath().equals("byakugan")) {
            return this.clan != null && this.clan.getPath().equals("hyuga");
        }

        // Scroll-only advanced & secret jutsus
        if (isScrollRequiredJutsu(abilityId)) {
            if (!getUnlockedAbilities().contains(abilityId)) {
                return false;
            }
            // Mangekyo jutsus also strictly require Uchiha bloodline
            if (abilityId.getPath().equals("amaterasu") || abilityId.getPath().equals("susanoo_ribcage")) {
                return this.clan != null && this.clan.getPath().equals("uchiha");
            }
            // Eight Gates also requires Jonin rank to withstand the forbidden strain
            if (abilityId.getPath().equals("eight_gates")) {
                NinjaRank playerRank = NinjaRank.fromString(this.getRank());
                return playerRank.getLevel() >= NinjaRank.JONIN.getLevel();
            }
            return true;
        }

        // Unlocked explicitly via scroll or training
        if (getUnlockedAbilities().contains(abilityId)) {
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
        float neededExp = 60.0f + (this.maxChakra - 100.0f) * 1.20f;
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
        NinjaRank current = NinjaRank.fromString(this.getRank());
        if (current == NinjaRank.ACADEMY_STUDENT) {
            return this.maxChakra >= 120.0f && this.getJutsusCastCount() >= 10;
        } else if (current == NinjaRank.GENIN) {
            return this.maxChakra >= 200.0f && this.maxStamina >= 150.0f && this.getEnemiesDefeated() >= 25;
        } else if (current == NinjaRank.CHUNIN) {
            return this.maxChakra >= 300.0f && this.maxStamina >= 200.0f && this.getEnemiesDefeated() >= 75 && this.getUnlockedAbilities().size() >= 3;
        } else if (current == NinjaRank.JONIN) {
            return this.maxChakra >= 400.0f && this.maxStamina >= 280.0f && this.getEnemiesDefeated() >= 150 && this.getUnlockedAbilities().size() >= 4;
        } else if (current == NinjaRank.ANBU) {
            return this.maxChakra >= 500.0f && this.maxStamina >= 350.0f && this.getEnemiesDefeated() >= 300 && this.getUnlockedAbilities().size() >= 5;
        }
        return false;
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
        this.jutsusCastCount = 0;
        this.unlockedJutsus = "";
        this.chakra = this.getMaxChakra();
        this.stamina = this.getMaxStamina();
    }

    @Override
    public void useChakra(float amount, int cooldown) {
        if (!this.creativeMode) {
            this.chakra = Math.max(0f, this.chakra - amount);
        }
        this.chakraRegenInfo.cooldown = Math.max(cooldown, this.chakraRegenInfo.cooldown);
        addChakraExp(amount * 0.04f);
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
        return this.toggleAbilityData;
    }

    @Override
    public void updateServerData(Player player) {
        if (player.isDeadOrDying()) {
            if (this.currentlyChanneled != null) {
                this.setCurrentlyChanneledAbility(player, null);
            }
            return;
        }

        // Safe delayed tick execution: collect events to run, then execute outside iterator
        ArrayList<DelayedPlayerTickEvent> toRun = new ArrayList<>();
        Iterator<DelayedPlayerTickEvent> iterator = this.delayedTickEvents.iterator();
        while (iterator.hasNext()) {
            DelayedPlayerTickEvent event = iterator.next();
            event.tick();
            if (event.shouldRun()) {
                toRun.add(event);
                iterator.remove();
            }
        }
        for (DelayedPlayerTickEvent event : toRun) {
            event.run(player);
        }

        ArrayList<String> completeList = new ArrayList<>(cooldownTickEvents.keySet());
        for (String name : completeList) {
            CooldownTickEvent event = cooldownTickEvents.get(name);
            if (event != null) {
                event.tick();
                if (event.isComplete()) {
                    cooldownTickEvents.remove(name);
                }
            }
        }

        if (this.staminaRegenInfo.canRegen()) {
            float staminaRegen = staminaRegenInfo.regenRate;
            if (this.clan != null && this.clan.getPath().equals("nara")) {
                staminaRegen *= 1.3f;
            }
            this.stamina += staminaRegen;
        }

        if (this.chakraRegenInfo.canRegen()) {
            float chakraRegen = chakraRegenInfo.regenRate;
            if (this.clan != null && this.clan.getPath().equals("uzumaki")) {
                chakraRegen *= 2.0f;
            }
            this.chakra += chakraRegen;
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
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    @Override
    public Tag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
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
        nbt.putInt(JUTSUS_CAST_TAG, this.jutsusCastCount);
        if (this.unlockedJutsus != null) {
            nbt.putString(UNLOCKED_JUTSUS_TAG, this.unlockedJutsus);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
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
        }
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
}
