package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.utils.BreweryIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;

import java.util.HashSet;
import java.util.Set;

public class InventoryMoveItemListener implements Listener {

    private final CauldronInteract plugin;
    private final Set<Block> pendingDistillers = new HashSet<>();

    public InventoryMoveItemListener(CauldronInteract plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (plugin.getBlockedInventories().contains(event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewerInput(InventoryMoveItemEvent event) {
        if (!(event.getDestination() instanceof BrewerInventory inventory)) return;
        BrewingStand stand = inventory.getHolder();
        if (stand == null) return;

        Block block = stand.getBlock();
        if (!pendingDistillers.add(block)) return;

        // InventoryMoveItemEvent runs before the transfer. Read the actual stand
        // next tick, including inputs of glowstone/fuel after the brew itself.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingDistillers.remove(block);
            if (event.isCancelled() || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                    || block.getType() != Material.BREWING_STAND) return;
            BreweryIntegration.startDistillation((BrewingStand) block.getState());
        }, 1L);
    }
}
