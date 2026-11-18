package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Village-specific civilian merchant NPC for each of the Five Great Nations
 * (Konoha, Suna, Kiri, Kumo, Iwa). Provides thematic shinobi equipment and scroll trades.
 */
public class ShinobiVillagerEntity extends PathfinderMob implements Merchant {

    private static final EntityDataAccessor<String> DATA_VILLAGE_TYPE =
            SynchedEntityData.defineId(ShinobiVillagerEntity.class, EntityDataSerializers.STRING);

    private final VillageNinjaEntity.VillageType fixedVillageType;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private int villagerXp = 0;

    public ShinobiVillagerEntity(EntityType<? extends PathfinderMob> type, Level level,
                                 VillageNinjaEntity.VillageType villageType) {
        super(type, level);
        this.fixedVillageType = villageType != null ? villageType : VillageNinjaEntity.VillageType.KONOHA;
        this.setVillageType(this.fixedVillageType);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VILLAGE_TYPE, VillageNinjaEntity.VillageType.KONOHA.getId());
    }

    public VillageNinjaEntity.VillageType getVillageType() {
        return VillageNinjaEntity.VillageType.fromId(this.entityData.get(DATA_VILLAGE_TYPE));
    }

    public void setVillageType(VillageNinjaEntity.VillageType type) {
        VillageNinjaEntity.VillageType resolved = type != null ? type : this.fixedVillageType;
        this.entityData.set(DATA_VILLAGE_TYPE, resolved.getId());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.setVillageType(this.fixedVillageType);
        this.offers = buildVillageOffers(this.fixedVillageType);
        this.setPersistenceRequired();
        return data;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && this.getTradingPlayer() == null && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource source) {
        this.setTradingPlayer(null);
        super.die(source);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = buildVillageOffers(this.getVillageType());
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.villagerXp += offer.getXp();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(this.getNotifyTradeSound(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    public int getVillagerXp() {
        return this.villagerXp;
    }

    @Override
    public void overrideXp(int xp) {
        this.villagerXp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.getTradingPlayer() != null ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.getTradingPlayer() == player && this.isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("VillageType", this.getVillageType().getId());
        output.putInt("VillagerXp", this.villagerXp);
        if (this.offers != null && !this.offers.isEmpty()) {
            output.store("Offers", MerchantOffers.CODEC, this.offers);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVillageType(VillageNinjaEntity.VillageType.fromId(
                input.getStringOr("VillageType", this.fixedVillageType.getId())));
        this.villagerXp = input.getIntOr("VillagerXp", 0);
        input.read("Offers", MerchantOffers.CODEC).ifPresent(loaded -> this.offers = loaded);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.tradingPlayer != null && (!this.tradingPlayer.isAlive() || this.tradingPlayer.distanceToSqr(this) > 64.0D)) {
            this.setTradingPlayer(null);
        }
    }

    public static java.util.List<String> getVillageSpecialtyItemIds(VillageNinjaEntity.VillageType type) {
        VillageNinjaEntity.VillageType village = type != null ? type : VillageNinjaEntity.VillageType.KONOHA;
        return switch (village) {
            case KONOHA -> java.util.List.of("ichiraku_ramen", "headband_leaf", "scroll_shadow_clone");
            case SUNA -> java.util.List.of("senbon", "headband_sand", "fuma_shuriken");
            case KIRI -> java.util.List.of("headband_mist", "anbu_tanto", "scroll_water_dragon");
            case KUMO -> java.util.List.of("headband_cloud", "chakra_blades", "scroll_chidori_stream");
            case IWA -> java.util.List.of("headband_rock", "chakra_metal", "scroll_stone_spikes");
        };
    }

    public static MerchantOffers buildVillageOffers(VillageNinjaEntity.VillageType type) {
        MerchantOffers list = new MerchantOffers();
        VillageNinjaEntity.VillageType village = type != null ? type : VillageNinjaEntity.VillageType.KONOHA;

        // Shared shinobi supply trades
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), Optional.empty(),
                new ItemStack(NarutoItems.KUNAI.get(), 8), 16, 2, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.empty(),
                new ItemStack(NarutoItems.SHURIKEN.get(), 8), 16, 2, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), Optional.empty(),
                new ItemStack(NarutoItems.EXPLOSIVE_KUNAI.get(), 4), 10, 4, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 8), Optional.empty(),
                new ItemStack(NarutoItems.SMOKE_BOMB.get(), 4), 12, 3, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), Optional.empty(),
                new ItemStack(NarutoItems.MILITARY_RATIONS_PILL.get(), 2), 12, 3, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 18),
                Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 2)),
                new ItemStack(NarutoItems.SEALED_AMMO_SCROLL.get(), 1), 6, 8, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 24),
                Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 3)),
                new ItemStack(NarutoItems.BLANK_JUTSU_SCROLL.get(), 1), 6, 10, 0.05F));

        // Village-unique specialty offers
        switch (village) {
            case KONOHA -> {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), Optional.empty(),
                        new ItemStack(NarutoItems.ICHIRAKU_RAMEN.get(), 2), 16, 2, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 14), Optional.empty(),
                        new ItemStack(NarutoItems.HEADBAND_LEAF.get(), 1), 8, 5, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 34),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 4)),
                        new ItemStack(NarutoItems.SCROLL_SHADOW_CLONE.get(), 1), 3, 15, 0.05F));
            }
            case SUNA -> {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(),
                        new ItemStack(NarutoItems.SENBON.get(), 12), 16, 2, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 14), Optional.empty(),
                        new ItemStack(NarutoItems.HEADBAND_SAND.get(), 1), 8, 5, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 30),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 4)),
                        new ItemStack(NarutoItems.FUMA_SHURIKEN.get(), 1), 4, 12, 0.05F));
            }
            case KIRI -> {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 14), Optional.empty(),
                        new ItemStack(NarutoItems.HEADBAND_MIST.get(), 1), 8, 5, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 26),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 2)),
                        new ItemStack(NarutoItems.ANBU_TANTO.get(), 1), 4, 10, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 42),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 6)),
                        new ItemStack(NarutoItems.SCROLL_WATER_DRAGON.get(), 1), 3, 18, 0.05F));
            }
            case KUMO -> {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 14), Optional.empty(),
                        new ItemStack(NarutoItems.HEADBAND_CLOUD.get(), 1), 8, 5, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 28),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 3)),
                        new ItemStack(NarutoItems.CHAKRA_BLADES.get(), 1), 4, 12, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 42),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 6)),
                        new ItemStack(NarutoItems.SCROLL_CHIDORI_STREAM.get(), 1), 3, 18, 0.05F));
            }
            case IWA -> {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 14), Optional.empty(),
                        new ItemStack(NarutoItems.HEADBAND_ROCK.get(), 1), 8, 5, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), Optional.empty(),
                        new ItemStack(NarutoItems.CHAKRA_METAL.get(), 2), 12, 4, 0.05F));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 42),
                        Optional.of(new ItemCost(NarutoItems.CHAKRA_METAL.get(), 6)),
                        new ItemStack(NarutoItems.SCROLL_STONE_SPIKES.get(), 1), 3, 18, 0.05F));
            }
        }
        return list;
    }
}
