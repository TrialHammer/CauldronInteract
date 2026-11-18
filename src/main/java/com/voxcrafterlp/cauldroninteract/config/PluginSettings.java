package com.voxcrafterlp.cauldroninteract.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

/**
 * Immutable, validated view of {@code config.yml}. Every tunable value used by
 * the plugin is represented here so server owners can customise behaviour
 * without editing code.
 */
public final class PluginSettings {

    private final boolean waterFillEnabled;
    private final boolean bottleFillEnabled;
    private final boolean ingredientEnabled;
    private final boolean dispenserUpgradeEnabled;
    private final boolean distillationEnabled;
    private final boolean distillationChunkLoadEnabled;
    private final boolean distillationBlockPlaceEnabled;
    private final long distillationCheckIntervalTicks;
    private final long distillationInitialDelayTicks;
    private final Material distillationIngredient;
    private final double dropOffsetX;
    private final double dropOffsetY;
    private final double dropOffsetZ;
    private final long deferredInventoryDelayTicks;
    private final boolean returnEmptyContainers;
    private final List<String> bucketSuffixes;
    private final List<String> bottleMaterialNames;
    private final boolean fillToMaximum;
    private final boolean consumeBottleLevel;
    private final boolean upgradeConsumeDurability;
    private final boolean upgradeMessageEnabled;
    private final boolean soundsEnabled;
    private final String bottleFillSound;
    private final float bottleFillVolume;
    private final float bottleFillPitch;
    private final String bucketEmptySound;
    private final float bucketEmptyVolume;
    private final float bucketEmptyPitch;
    private final String upgradeSound;
    private final float upgradeSoundVolume;
    private final float upgradeSoundPitch;
    private final boolean upgradeParticlesEnabled;
    private final String upgradeParticle;
    private final int upgradeParticleCount;
    private final double upgradeParticleOffsetX;
    private final double upgradeParticleOffsetY;
    private final double upgradeParticleOffsetZ;

    private PluginSettings(FileConfiguration c) {
        waterFillEnabled = getBoolean(c, "features.water-fill.enabled", true);
        bottleFillEnabled = getBoolean(c, "features.bottle-fill.enabled", true);
        ingredientEnabled = getBoolean(c, "features.ingredient.enabled", true);
        dispenserUpgradeEnabled = getBoolean(c, "features.dispenser-upgrade.enabled",
                c.getBoolean("enable-dispenser-upgrade", false));
        distillationEnabled = getBoolean(c, "features.distillation.enabled", true);
        distillationChunkLoadEnabled = getBoolean(c, "features.distillation.track-chunk-loads", true);
        distillationBlockPlaceEnabled = getBoolean(c, "features.distillation.track-block-place", true);
        distillationCheckIntervalTicks = getLong(c, "features.distillation.check-interval-ticks", 10L, 1L, 20L * 60L);
        distillationInitialDelayTicks = getLong(c, "features.distillation.initial-delay-ticks", 10L, 0L, 20L * 60L);
        distillationIngredient = getMaterial(c, "features.distillation.ingredient-material", Material.GLOWSTONE_DUST);
        dropOffsetX = getDouble(c, "behaviour.drop-offset.x", 0.5D, -16D, 16D);
        dropOffsetY = getDouble(c, "behaviour.drop-offset.y", -0.75D, -16D, 16D);
        dropOffsetZ = getDouble(c, "behaviour.drop-offset.z", 0.5D, -16D, 16D);
        deferredInventoryDelayTicks = getLong(c, "behaviour.deferred-inventory-delay-ticks", 1L, 0L, 200L);
        returnEmptyContainers = getBoolean(c, "behaviour.return-empty-containers", true);
        fillToMaximum = getBoolean(c, "behaviour.fill-cauldron-to-maximum", true);
        consumeBottleLevel = getBoolean(c, "behaviour.consume-one-water-level-per-bottle", true);
        upgradeConsumeDurability = getBoolean(c, "features.dispenser-upgrade.consume-durability", true);
        upgradeMessageEnabled = getBoolean(c, "features.dispenser-upgrade.send-message", true);
        List<String> configuredBucketSuffixes = c.getStringList("behaviour.bucket-suffixes");
        bucketSuffixes = configuredBucketSuffixes.isEmpty() ? List.of("_BUCKET") : List.copyOf(configuredBucketSuffixes);
        List<String> configuredBottleMaterials = c.getStringList("behaviour.bottle-materials");
        bottleMaterialNames = configuredBottleMaterials.isEmpty()
                ? List.of("POTION", "SPLASH_POTION", "LINGERING_POTION", "EXPERIENCE_BOTTLE", "DRAGON_BREATH", "HONEY_BOTTLE")
                : List.copyOf(configuredBottleMaterials);

        soundsEnabled = getBoolean(c, "effects.sounds.enabled", true);
        bottleFillSound = getSoundName(c, "effects.sounds.bottle-fill.key", "ITEM_BOTTLE_FILL");
        bottleFillVolume = (float) getDouble(c, "effects.sounds.bottle-fill.volume", 1.0D, 0.0D, 4.0D);
        bottleFillPitch = (float) getDouble(c, "effects.sounds.bottle-fill.pitch", 1.0D, 0.1D, 4.0D);
        bucketEmptySound = getSoundName(c, "effects.sounds.bucket-empty.key", "ITEM_BUCKET_EMPTY");
        bucketEmptyVolume = (float) getDouble(c, "effects.sounds.bucket-empty.volume", 1.0D, 0.0D, 4.0D);
        bucketEmptyPitch = (float) getDouble(c, "effects.sounds.bucket-empty.pitch", 1.0D, 0.1D, 4.0D);
        upgradeSound = getSoundName(c, "effects.sounds.upgrade.key", "ITEM_HONEYCOMB_WAX_ON");
        upgradeSoundVolume = (float) getDouble(c, "effects.sounds.upgrade.volume", 1.0D, 0.0D, 4.0D);
        upgradeSoundPitch = (float) getDouble(c, "effects.sounds.upgrade.pitch", 1.0D, 0.1D, 4.0D);
        upgradeParticlesEnabled = getBoolean(c, "effects.upgrade-particles.enabled", true);
        upgradeParticle = getSoundName(c, "effects.upgrade-particles.particle", "WAX_ON");
        upgradeParticleCount = (int) getLong(c, "effects.upgrade-particles.count", 5L, 0L, 1000L);
        upgradeParticleOffsetX = getDouble(c, "effects.upgrade-particles.offset.x", 0.5D, -10D, 10D);
        upgradeParticleOffsetY = getDouble(c, "effects.upgrade-particles.offset.y", 1.1D, -10D, 10D);
        upgradeParticleOffsetZ = getDouble(c, "effects.upgrade-particles.offset.z", 0.5D, -10D, 10D);
    }

    public static PluginSettings from(FileConfiguration config) {
        return config == null ? defaults() : new PluginSettings(config);
    }

    public static PluginSettings defaults() {
        return new PluginSettings(new org.bukkit.configuration.file.YamlConfiguration());
    }

    private static boolean getBoolean(FileConfiguration c, String path, boolean fallback) {
        return c.contains(path) ? c.getBoolean(path, fallback) : fallback;
    }

    private static long getLong(FileConfiguration c, String path, long fallback, long min, long max) {
        long value = c.getLong(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private static double getDouble(FileConfiguration c, String path, double fallback, double min, double max) {
        double value = c.getDouble(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private static Material getMaterial(FileConfiguration c, String path, Material fallback) {
        Material value = Material.matchMaterial(c.getString(path, fallback.name()));
        return value == null ? fallback : value;
    }

    private static String getSoundName(FileConfiguration c, String path, String fallback) {
        String name = c.getString(path, fallback);
        if (name == null || name.isBlank()) return fallback;
        return name.trim().toUpperCase(Locale.ROOT);
    }

    public boolean waterFillEnabled() { return waterFillEnabled; }
    public boolean bottleFillEnabled() { return bottleFillEnabled; }
    public boolean ingredientEnabled() { return ingredientEnabled; }
    public boolean dispenserUpgradeEnabled() { return dispenserUpgradeEnabled; }
    public boolean distillationEnabled() { return distillationEnabled; }
    public boolean distillationChunkLoadEnabled() { return distillationChunkLoadEnabled; }
    public boolean distillationBlockPlaceEnabled() { return distillationBlockPlaceEnabled; }
    public long distillationCheckIntervalTicks() { return distillationCheckIntervalTicks; }
    public long distillationInitialDelayTicks() { return distillationInitialDelayTicks; }
    public Material distillationIngredient() { return distillationIngredient; }
    public double dropOffsetX() { return dropOffsetX; }
    public double dropOffsetY() { return dropOffsetY; }
    public double dropOffsetZ() { return dropOffsetZ; }
    public long deferredInventoryDelayTicks() { return deferredInventoryDelayTicks; }
    public boolean returnEmptyContainers() { return returnEmptyContainers; }
    public List<String> bucketSuffixes() { return bucketSuffixes; }
    public List<String> bottleMaterialNames() { return bottleMaterialNames; }
    public boolean fillToMaximum() { return fillToMaximum; }
    public boolean consumeBottleLevel() { return consumeBottleLevel; }
    public boolean upgradeConsumeDurability() { return upgradeConsumeDurability; }
    public boolean upgradeMessageEnabled() { return upgradeMessageEnabled; }
    public boolean soundsEnabled() { return soundsEnabled; }
    public String bottleFillSound() { return bottleFillSound; }
    public float bottleFillVolume() { return bottleFillVolume; }
    public float bottleFillPitch() { return bottleFillPitch; }
    public String bucketEmptySound() { return bucketEmptySound; }
    public float bucketEmptyVolume() { return bucketEmptyVolume; }
    public float bucketEmptyPitch() { return bucketEmptyPitch; }
    public String upgradeSound() { return upgradeSound; }
    public float upgradeSoundVolume() { return upgradeSoundVolume; }
    public float upgradeSoundPitch() { return upgradeSoundPitch; }
    public boolean upgradeParticlesEnabled() { return upgradeParticlesEnabled; }
    public String upgradeParticle() { return upgradeParticle; }
    public int upgradeParticleCount() { return upgradeParticleCount; }
    public double upgradeParticleOffsetX() { return upgradeParticleOffsetX; }
    public double upgradeParticleOffsetY() { return upgradeParticleOffsetY; }
    public double upgradeParticleOffsetZ() { return upgradeParticleOffsetZ; }
}
