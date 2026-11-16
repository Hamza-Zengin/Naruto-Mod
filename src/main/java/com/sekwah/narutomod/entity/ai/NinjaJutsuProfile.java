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

    /**
     * Aggressive S-Rank Akatsuki preset: short cooldowns, dual elemental projectiles (Fireball & Water Bullet),
     * and both Kawarimi evasion and Mud Wall defense at critical health.
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
                .kawarimi(true, 140, 5.5D, 60, 1)
                .mudWall(true, 160, 120, 45.0F)
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
