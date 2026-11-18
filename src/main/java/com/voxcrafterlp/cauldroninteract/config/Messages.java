package com.voxcrafterlp.cauldroninteract.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads all player-facing text from {@code messages.yml}. The bundled resource
 * supplies defaults so no visible text is hardcoded in Java.
 */
public final class Messages {

    private final FileConfiguration config;

    public Messages(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = bundledDefaults(plugin);
        if (defaults != null) {
            loaded.setDefaults(defaults);
            loaded.options().copyDefaults(true);
        }
        this.config = loaded;
    }

    private static YamlConfiguration bundledDefaults(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return null;
        }
    }

    /** Raw configured value (colour codes untouched), or an empty string when absent. */
    public String raw(String key) {
        String value = config.getString(key);
        return value == null ? "" : value;
    }

    public String format(String key, Map<String, String> placeholders) {
        String value = raw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = format(key, placeholders);
        if (!message.isBlank()) sender.sendMessage(message);
    }

    public String text(String key) {
        return format(key, Map.of());
    }

    /** Returns a configured text value without applying colour codes. */
    public String rawText(String key) {
        return raw(key);
    }
}
