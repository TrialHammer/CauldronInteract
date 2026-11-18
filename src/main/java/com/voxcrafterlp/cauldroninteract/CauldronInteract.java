package com.voxcrafterlp.cauldroninteract;

import com.voxcrafterlp.cauldroninteract.command.CauldronInteractCommand;
import com.voxcrafterlp.cauldroninteract.config.Messages;
import com.voxcrafterlp.cauldroninteract.config.PluginSettings;
import com.voxcrafterlp.cauldroninteract.listener.BlockDispenseListener;
import com.voxcrafterlp.cauldroninteract.listener.DistillationListener;
import com.voxcrafterlp.cauldroninteract.listener.InventoryMoveItemListener;
import com.voxcrafterlp.cauldroninteract.listener.PlayerInteractListener;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CauldronInteract extends JavaPlugin {

    private static final int CONFIG_VERSION = 1;

    private final Set<Inventory> blockedInventories = new HashSet<>();
    private NamespacedKey dispenserUpgradedKey;
    private PluginSettings settings;
    private Messages messages;
    private DistillationListener distillationListener;
    private InventoryMoveItemListener inventoryMoveItemListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dispenserUpgradedKey = new NamespacedKey(this, "dispenser-upgraded");
        migrateLegacyConfig();
        loadRuntimeConfiguration();

        PluginCommand command = getCommand("cauldroninteract");
        if (command != null) {
            CauldronInteractCommand executor = new CauldronInteractCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void migrateLegacyConfig() {
        boolean changed = false;

        // v1 legacy flat switch -> grouped feature section.
        if (getConfig().contains("enable-dispenser-upgrade")) {
            boolean enabled = getConfig().getBoolean("enable-dispenser-upgrade", false);
            getConfig().set("features.dispenser-upgrade.enabled", enabled);
            getConfig().set("enable-dispenser-upgrade", null);
            changed = true;
        }

        int version = getConfig().getInt("config-version", 0);
        if (version < CONFIG_VERSION) {
            getConfig().set("config-version", CONFIG_VERSION);
            changed = true;
        }

        // Merge keys added by newer releases (existing values always win).
        var before = new java.util.HashSet<>(getConfig().getKeys(true));
        getConfig().options().copyDefaults(true);
        boolean defaultsAdded = !new java.util.HashSet<>(getConfig().getKeys(true)).equals(before);

        if (changed || defaultsAdded) saveConfig();
    }

    /** Reloads config.yml and messages.yml, then re-registers event listeners. */
    public void reloadPluginConfiguration() {
        reloadConfig();
        loadRuntimeConfiguration();
    }

    private void loadRuntimeConfiguration() {
        if (distillationListener != null) distillationListener.stopWatching();
        settings = PluginSettings.from(getConfig());
        messages = new Messages(this);

        org.bukkit.event.HandlerList.unregisterAll(this);
        blockedInventories.clear();

        if (settings.waterFillEnabled() || settings.bottleFillEnabled() || settings.ingredientEnabled()) {
            getServer().getPluginManager().registerEvents(new BlockDispenseListener(this), this);
        }

        inventoryMoveItemListener = new InventoryMoveItemListener(this);
        getServer().getPluginManager().registerEvents(inventoryMoveItemListener, this);

        distillationListener = new DistillationListener(this);
        getServer().getPluginManager().registerEvents(distillationListener, this);
        if (settings.distillationEnabled()) {
            distillationListener.startWatching();
        }

        if (settings.dispenserUpgradeEnabled()) {
            getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        }
    }

    @Override
    public void onDisable() {
        blockedInventories.clear();
    }

    public Set<Inventory> getBlockedInventories() {
        return blockedInventories;
    }

    public NamespacedKey getDispenserUpgradedKey() {
        return dispenserUpgradedKey;
    }

    public PluginSettings getSettings() {
        return Objects.requireNonNull(settings, "Settings not loaded");
    }

    public Messages getMessages() {
        return Objects.requireNonNull(messages, "Messages not loaded");
    }

    public boolean isDispenserUpgradeEnabled() {
        return getSettings().dispenserUpgradeEnabled();
    }

    public boolean isBreweryAvailable() {
        var brewery = getServer().getPluginManager().getPlugin("BreweryX");
        return brewery != null && brewery.isEnabled();
    }
}
