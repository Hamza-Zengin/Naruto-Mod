package com.sekwah.narutomod.entity.ai;

/**
 * Tactical configuration profile for {@link NinjaJutsuAttackGoal}.
 * Allows distinct AI personalities (e.g., aggressive Akatsuki mini-boss vs. evasive Rogue Ninja).
 */
public final class NinjaJutsuProfile {

    private final double minRangedDistance;
    private final double maxRangedDistance;
    private final double closeThreatDistance;
    private final float criticalHealthRatio;

    private final int globalCooldownTicks;
    private final int handSealWindupTicks;

    private final boolean fireballEnabled;
    private final int fireballCooldownTicks;
    private final int fireballCooldownJitter;

    private final boolean waterBulletEnabled;
    private final int waterBulletCooldownTicks;
    private final int waterBulletCooldownJitter;
    private final float waterBulletDamage;
    private final int waterBulletMaxLife;
    private final double waterBulletSpeed;

    private final boolean earthBulletEnabled;
    private final int earthBulletCooldownTicks;
    private final int earthBulletCooldownJitter;
    private final float earthBulletDamage;
    private final int earthBulletMaxLife;
    private final double earthBulletSpeed;

    private final boolean vacuumSpheresEnabled;
    private final int vacuumSpheresCooldownTicks;
    private final int vacuumSpheresCooldownJitter;
    private final float vacuumSpheresDamage;
    private final int vacuumSpheresMaxLife;
    private final double vacuumSpheresSpeed;

    private final boolean chidoriSparkEnabled;
    private final int chidoriSparkCooldownTicks;
    private final float chidoriSparkDamage;

    private final boolean kawarimiEnabled;
    private final int kawarimiCooldownTicks;
    private final double kawarimiDistance;
    private final int kawarimiSpeedDurationTicks;
    private final int kawarimiSpeedAmplifier;

    private final boolean mudWallEnabled;
    private final int mudWallCooldownTicks;
    private final int mudWallMaxLifeTicks;
    private final float mudWallHealth;

    private NinjaJutsuProfile(Builder builder) {
        this.minRangedDistance = builder.minRangedDistance;
        this.maxRangedDistance = builder.maxRangedDistance;
        this.closeThreatDistance = builder.closeThreatDistance;
        this.criticalHealthRatio = builder.criticalHealthRatio;
        this.globalCooldownTicks = builder.globalCooldownTicks;
        this.handSealWindupTicks = builder.handSealWindupTicks;
        this.fireballEnabled = builder.fireballEnabled;
        this.fireballCooldownTicks = builder.fireballCooldownTicks;
        this.fireballCooldownJitter = builder.fireballCooldownJitter;
        this.waterBulletEnabled = builder.waterBulletEnabled;
        this.waterBulletCooldownTicks = builder.waterBulletCooldownTicks;
        this.waterBulletCooldownJitter = builder.waterBulletCooldownJitter;
        this.waterBulletDamage = builder.waterBulletDamage;
        this.waterBulletMaxLife = builder.waterBulletMaxLife;
        this.waterBulletSpeed = builder.waterBulletSpeed;
        this.earthBulletEnabled = builder.earthBulletEnabled;
        this.earthBulletCooldownTicks = builder.earthBulletCooldownTicks;
        this.earthBulletCooldownJitter = builder.earthBulletCooldownJitter;
        this.earthBulletDamage = builder.earthBulletDamage;
        this.earthBulletMaxLife = builder.earthBulletMaxLife;
        this.earthBulletSpeed = builder.earthBulletSpeed;
        this.vacuumSpheresEnabled = builder.vacuumSpheresEnabled;
        this.vacuumSpheresCooldownTicks = builder.vacuumSpheresCooldownTicks;
        this.vacuumSpheresCooldownJitter = builder.vacuumSpheresCooldownJitter;
        this.vacuumSpheresDamage = builder.vacuumSpheresDamage;
        this.vacuumSpheresMaxLife = builder.vacuumSpheresMaxLife;
        this.vacuumSpheresSpeed = builder.vacuumSpheresSpeed;
        this.chidoriSparkEnabled = builder.chidoriSparkEnabled;
        this.chidoriSparkCooldownTicks = builder.chidoriSparkCooldownTicks;
        this.chidoriSparkDamage = builder.chidoriSparkDamage;
        this.kawarimiEnabled = builder.kawarimiEnabled;
        this.kawarimiCooldownTicks = builder.kawarimiCooldownTicks;
        this.kawarimiDistance = builder.kawarimiDistance;
        this.kawarimiSpeedDurationTicks = builder.kawarimiSpeedDurationTicks;
        this.kawarimiSpeedAmplifier = builder.kawarimiSpeedAmplifier;
        this.mudWallEnabled = builder.mudWallEnabled;
        this.mudWallCooldownTicks = builder.mudWallCooldownTicks;
        this.mudWallMaxLifeTicks = builder.mudWallMaxLifeTicks;
        this.mudWallHealth = builder.mudWallHealth;
    }

    public static NinjaJutsuProfile villagePreset(String villageId) {
        String key = villageId == null ? "konoha" : villageId.toLowerCase(java.util.Locale.ROOT);
        Builder b = new Builder()
                .rangedDistances(8.0D, 24.0D)
                .closeThreatDistance(6.5D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(45)
                .handSealWindupTicks(10)
                .kawarimi(true, 180, 5.0D, 80, 0);
        switch (key) {
            case "suna" -> b.vacuumSpheres(true, 95, 30, 5.5F, 32, 1.50D);
            case "kiri" -> b.waterBullet(true, 95, 30, 9.5F, 32, 1.25D);
            case "kumo" -> b.chidoriSpark(true, 100, 10.5F).vacuumSpheres(true, 120, 30, 5.0F, 30, 1.45D);
            case "iwa" -> b.earthBullet(true, 100, 30, 10.5F, 38, 1.15D).mudWall(true, 180, 100, 40.0F);
            default -> b.fireball(true, 100, 30);
        }
        return b.build();
    }

    /**
     * Aggressive S-Rank Akatsuki preset: short cooldowns, multi-element projectiles (Fireball, Water Bullet,
     * Earth Dragon Bullet & Vacuum Spheres), and both Kawarimi evasion and Mud Wall defense at critical health.
     */
    public static NinjaJutsuProfile akatsukiPreset() {
        return new Builder()
                .rangedDistances(10.0D, 24.0D)
                .closeThreatDistance(7.0D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(30)
                .handSealWindupTicks(8)
                .fireball(true, 65, 25)
                .waterBullet(true, 65, 25, 12.0F, 32, 1.25D)
                .earthBullet(true, 75, 25, 13.0F, 40, 1.15D)
                .vacuumSpheres(true, 75, 25, 6.0F, 30, 1.55D)
                .kawarimi(true, 140, 5.5D, 60, 1)
                .mudWall(true, 160, 120, 45.0F)
                .build();
    }

    /**
     * Chūnin Arena Champion Boss preset: fast cooldowns, Fireball, Vacuum Spheres,
     * close-range Chidori Spark, and defensive Kawarimi & Mud Wall reflexes.
     */
    public static NinjaJutsuProfile chuninChampionPreset() {
        return new Builder()
                .rangedDistances(7.0D, 22.0D)
                .closeThreatDistance(6.0D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(28)
                .handSealWindupTicks(8)
                .fireball(true, 65, 20)
                .vacuumSpheres(true, 70, 20, 6.0F, 30, 1.55D)
                .chidoriSpark(true, 85, 11.0F)
                .kawarimi(true, 130, 5.5D, 70, 1)
                .mudWall(true, 150, 120, 45.0F)
                .build();
    }

    /**
     * Evasive Rogue Ninja preset: longer cooldowns, basic Water Bullet projectile at range,
     * and Kawarimi smoke evasion at critical health.
     */
    public static NinjaJutsuProfile roguePreset() {
        return new Builder()
                .rangedDistances(10.0D, 24.0D)
                .closeThreatDistance(6.5D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(55)
                .handSealWindupTicks(14)
                .fireball(false, 120, 30)
                .waterBullet(true, 115, 35, 8.0F, 30, 1.15D)
                .earthBullet(false, 130, 35, 9.0F, 35, 1.05D)
                .vacuumSpheres(false, 130, 35, 4.5F, 28, 1.40D)
                .kawarimi(true, 200, 4.5D, 80, 0)
                .mudWall(false, 200, 100, 35.0F)
                .build();
    }

    public double getMinRangedDistance() { return this.minRangedDistance; }
    public double getMaxRangedDistance() { return this.maxRangedDistance; }
    public double getCloseThreatDistance() { return this.closeThreatDistance; }
    public float getCriticalHealthRatio() { return this.criticalHealthRatio; }
    public int getGlobalCooldownTicks() { return this.globalCooldownTicks; }
    public int getHandSealWindupTicks() { return this.handSealWindupTicks; }

    public boolean isFireballEnabled() { return this.fireballEnabled; }
    public int getFireballCooldownTicks() { return this.fireballCooldownTicks; }
    public int getFireballCooldownJitter() { return this.fireballCooldownJitter; }

    public boolean isWaterBulletEnabled() { return this.waterBulletEnabled; }
    public int getWaterBulletCooldownTicks() { return this.waterBulletCooldownTicks; }
    public int getWaterBulletCooldownJitter() { return this.waterBulletCooldownJitter; }
    public float getWaterBulletDamage() { return this.waterBulletDamage; }
    public int getWaterBulletMaxLife() { return this.waterBulletMaxLife; }
    public double getWaterBulletSpeed() { return this.waterBulletSpeed; }

    public boolean isEarthBulletEnabled() { return this.earthBulletEnabled; }
    public int getEarthBulletCooldownTicks() { return this.earthBulletCooldownTicks; }
    public int getEarthBulletCooldownJitter() { return this.earthBulletCooldownJitter; }
    public float getEarthBulletDamage() { return this.earthBulletDamage; }
    public int getEarthBulletMaxLife() { return this.earthBulletMaxLife; }
    public double getEarthBulletSpeed() { return this.earthBulletSpeed; }

    public boolean isVacuumSpheresEnabled() { return this.vacuumSpheresEnabled; }
    public int getVacuumSpheresCooldownTicks() { return this.vacuumSpheresCooldownTicks; }
    public int getVacuumSpheresCooldownJitter() { return this.vacuumSpheresCooldownJitter; }
    public float getVacuumSpheresDamage() { return this.vacuumSpheresDamage; }
    public int getVacuumSpheresMaxLife() { return this.vacuumSpheresMaxLife; }
    public double getVacuumSpheresSpeed() { return this.vacuumSpheresSpeed; }

    public boolean isChidoriSparkEnabled() { return this.chidoriSparkEnabled; }
    public int getChidoriSparkCooldownTicks() { return this.chidoriSparkCooldownTicks; }
    public float getChidoriSparkDamage() { return this.chidoriSparkDamage; }

    public boolean isKawarimiEnabled() { return this.kawarimiEnabled; }
    public int getKawarimiCooldownTicks() { return this.kawarimiCooldownTicks; }
    public double getKawarimiDistance() { return this.kawarimiDistance; }
    public int getKawarimiSpeedDurationTicks() { return this.kawarimiSpeedDurationTicks; }
    public int getKawarimiSpeedAmplifier() { return this.kawarimiSpeedAmplifier; }

    public boolean isMudWallEnabled() { return this.mudWallEnabled; }
    public int getMudWallCooldownTicks() { return this.mudWallCooldownTicks; }
    public int getMudWallMaxLifeTicks() { return this.mudWallMaxLifeTicks; }
    public float getMudWallHealth() { return this.mudWallHealth; }

    public boolean isEnabled(NinjaJutsuType type) {
        return switch (type) {
            case FIREBALL -> this.fireballEnabled;
            case WATER_BULLET -> this.waterBulletEnabled;
            case EARTH_BULLET -> this.earthBulletEnabled;
            case VACUUM_SPHERES -> this.vacuumSpheresEnabled;
            case CHIDORI_SPARK -> this.chidoriSparkEnabled;
            case KAWARIMI -> this.kawarimiEnabled;
            case MUD_WALL -> this.mudWallEnabled;
        };
    }

    public static final class Builder {
        private double minRangedDistance = 10.0D;
        private double maxRangedDistance = 24.0D;
        private double closeThreatDistance = 6.5D;
        private float criticalHealthRatio = 0.35F;
        private int globalCooldownTicks = 40;
        private int handSealWindupTicks = 10;

        private boolean fireballEnabled = false;
        private int fireballCooldownTicks = 100;
        private int fireballCooldownJitter = 30;

        private boolean waterBulletEnabled = true;
        private int waterBulletCooldownTicks = 100;
        private int waterBulletCooldownJitter = 30;
        private float waterBulletDamage = 10.0F;
        private int waterBulletMaxLife = 30;
        private double waterBulletSpeed = 1.15D;

        private boolean earthBulletEnabled = false;
        private int earthBulletCooldownTicks = 110;
        private int earthBulletCooldownJitter = 30;
        private float earthBulletDamage = 11.0F;
        private int earthBulletMaxLife = 40;
        private double earthBulletSpeed = 1.10D;

        private boolean vacuumSpheresEnabled = false;
        private int vacuumSpheresCooldownTicks = 110;
        private int vacuumSpheresCooldownJitter = 30;
        private float vacuumSpheresDamage = 5.5F;
        private int vacuumSpheresMaxLife = 30;
        private double vacuumSpheresSpeed = 1.45D;

        private boolean chidoriSparkEnabled = false;
        private int chidoriSparkCooldownTicks = 100;
        private float chidoriSparkDamage = 10.0F;

        private boolean kawarimiEnabled = true;
        private int kawarimiCooldownTicks = 180;
        private double kawarimiDistance = 4.5D;
        private int kawarimiSpeedDurationTicks = 80;
        private int kawarimiSpeedAmplifier = 0;

        private boolean mudWallEnabled = false;
        private int mudWallCooldownTicks = 180;
        private int mudWallMaxLifeTicks = 120;
        private float mudWallHealth = 45.0F;

        public Builder rangedDistances(double min, double max) {
            this.minRangedDistance = min;
            this.maxRangedDistance = max;
            return this;
        }

        public Builder closeThreatDistance(double dist) {
            this.closeThreatDistance = dist;
            return this;
        }

        public Builder criticalHealthRatio(float ratio) {
            this.criticalHealthRatio = ratio;
            return this;
        }

        public Builder globalCooldownTicks(int ticks) {
            this.globalCooldownTicks = ticks;
            return this;
        }

        public Builder handSealWindupTicks(int ticks) {
            this.handSealWindupTicks = ticks;
            return this;
        }

        public Builder fireball(boolean enabled, int cooldownTicks, int jitter) {
            this.fireballEnabled = enabled;
            this.fireballCooldownTicks = cooldownTicks;
            this.fireballCooldownJitter = jitter;
            return this;
        }

        public Builder waterBullet(boolean enabled, int cooldownTicks, int jitter, float damage, int maxLife, double speed) {
            this.waterBulletEnabled = enabled;
            this.waterBulletCooldownTicks = cooldownTicks;
            this.waterBulletCooldownJitter = jitter;
            this.waterBulletDamage = damage;
            this.waterBulletMaxLife = maxLife;
            this.waterBulletSpeed = speed;
            return this;
        }

        public Builder earthBullet(boolean enabled, int cooldownTicks, int jitter, float damage, int maxLife, double speed) {
            this.earthBulletEnabled = enabled;
            this.earthBulletCooldownTicks = cooldownTicks;
            this.earthBulletCooldownJitter = jitter;
            this.earthBulletDamage = damage;
            this.earthBulletMaxLife = maxLife;
            this.earthBulletSpeed = speed;
            return this;
        }

        public Builder vacuumSpheres(boolean enabled, int cooldownTicks, int jitter, float damage, int maxLife, double speed) {
            this.vacuumSpheresEnabled = enabled;
            this.vacuumSpheresCooldownTicks = cooldownTicks;
            this.vacuumSpheresCooldownJitter = jitter;
            this.vacuumSpheresDamage = damage;
            this.vacuumSpheresMaxLife = maxLife;
            this.vacuumSpheresSpeed = speed;
            return this;
        }

        public Builder chidoriSpark(boolean enabled, int cooldownTicks, float damage) {
            this.chidoriSparkEnabled = enabled;
            this.chidoriSparkCooldownTicks = cooldownTicks;
            this.chidoriSparkDamage = damage;
            return this;
        }

        public Builder kawarimi(boolean enabled, int cooldownTicks, double distance, int speedDurationTicks, int speedAmplifier) {
            this.kawarimiEnabled = enabled;
            this.kawarimiCooldownTicks = cooldownTicks;
            this.kawarimiDistance = distance;
            this.kawarimiSpeedDurationTicks = speedDurationTicks;
            this.kawarimiSpeedAmplifier = speedAmplifier;
            return this;
        }

        public Builder mudWall(boolean enabled, int cooldownTicks, int maxLifeTicks, float health) {
            this.mudWallEnabled = enabled;
            this.mudWallCooldownTicks = cooldownTicks;
            this.mudWallMaxLifeTicks = maxLifeTicks;
            this.mudWallHealth = health;
            return this;
        }

        public NinjaJutsuProfile build() {
            return new NinjaJutsuProfile(this);
        }
    }
}
