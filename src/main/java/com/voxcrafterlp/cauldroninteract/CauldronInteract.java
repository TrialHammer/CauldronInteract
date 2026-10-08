package com.voxcrafterlp.cauldroninteract;

import com.voxcrafterlp.cauldroninteract.listener.BlockDispenseListener;
import com.voxcrafterlp.cauldroninteract.listener.DistillationListener;
import com.voxcrafterlp.cauldroninteract.listener.InventoryMoveItemListener;
import com.voxcrafterlp.cauldroninteract.listener.PlayerInteractListener;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class CauldronInteract extends JavaPlugin {

    private final Set<Inventory> blockedInventories = new HashSet<>();
    private NamespacedKey dispenserUpgradedKey;
    private boolean dispenserUpgradeEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dispenserUpgradedKey = new NamespacedKey(this, "dispenser-upgraded");
        dispenserUpgradeEnabled = getConfig().getBoolean("enable-dispenser-upgrade", false);

        getServer().getPluginManager().registerEvents(new BlockDispenseListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryMoveItemListener(this), this);
        DistillationListener distillation = new DistillationListener(this);
        getServer().getPluginManager().registerEvents(distillation, this);
        distillation.startWatching();
        if (dispenserUpgradeEnabled) {
            getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        }
    }

    public Set<Inventory> getBlockedInventories() {
        return blockedInventories;
    }

    public NamespacedKey getDispenserUpgradedKey() {
        return dispenserUpgradedKey;
    }

    public boolean isDispenserUpgradeEnabled() {
        return dispenserUpgradeEnabled;
    }
}
