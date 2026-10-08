package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.utils.BreweryIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryMoveItemListener implements Listener {

    private final CauldronInteract plugin;
    private final Map<Block, List<InventoryMoveItemEvent>> pendingDistillers = new HashMap<>();

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
        Inventory inventory = event.getDestination();
        Block block;
        if (inventory.getHolder() instanceof BrewingStand stand) {
            block = stand.getBlock();
        } else {
            if (inventory.getType() != InventoryType.BREWING) return;
            Location location = inventory.getLocation();
            if (location == null) return;
            block = location.getBlock();
        }

        List<InventoryMoveItemEvent> pending = pendingDistillers.get(block);
        if (pending != null) {
            pending.add(event);
            return;
        }
        List<InventoryMoveItemEvent> transfers = new ArrayList<>();
        transfers.add(event);
        pendingDistillers.put(block, transfers);

        // InventoryMoveItemEvent runs before the transfer. Read the actual stand
        // next tick, including inputs of glowstone/fuel after the brew itself.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingDistillers.remove(block);
            if (transfers.stream().allMatch(InventoryMoveItemEvent::isCancelled)
                    || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                    || block.getType() != Material.BREWING_STAND) return;
            BreweryIntegration.startDistillation((BrewingStand) block.getState());
        }, 1L);
    }
}
