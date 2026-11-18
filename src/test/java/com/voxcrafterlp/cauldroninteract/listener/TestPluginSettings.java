package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.config.PluginSettings;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Builds settings for unit tests from an in-memory YAML document, so tests do
 * not touch Bukkit's static Material registry bootstrap.
 */
final class TestPluginSettings {
    private TestPluginSettings() {}

    static PluginSettings create() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("features.water-fill.enabled", true);
        config.set("features.bottle-fill.enabled", true);
        config.set("features.ingredient.enabled", true);
        config.set("features.dispenser-upgrade.enabled", false);
        config.set("features.distillation.enabled", true);
        config.set("features.distillation.track-chunk-loads", true);
        config.set("features.distillation.track-block-place", true);
        config.set("features.distillation.check-interval-ticks", 10);
        config.set("features.distillation.initial-delay-ticks", 10);
        config.set("features.distillation.ingredient-material", "GLOWSTONE_DUST");
        config.set("behaviour.deferred-inventory-delay-ticks", 1);
        config.set("behaviour.return-empty-containers", true);
        config.set("effects.sounds.enabled", true);
        config.set("effects.upgrade-particles.enabled", false);
        return PluginSettings.from(config);
    }
}
