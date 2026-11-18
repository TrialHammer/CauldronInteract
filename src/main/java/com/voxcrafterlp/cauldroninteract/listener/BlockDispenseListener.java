package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.config.PluginSettings;
import com.voxcrafterlp.cauldroninteract.utils.BreweryIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BlockDispenseListener implements Listener {

    private final CauldronInteract plugin;

    public BlockDispenseListener(CauldronInteract plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) return;
        PluginSettings settings = plugin.getSettings();
        if (settings.dispenserUpgradeEnabled()) {
            Dispenser dispenser = (Dispenser) event.getBlock().getState();
            if (!dispenser.getPersistentDataContainer().has(plugin.getDispenserUpgradedKey())) return;
        }

        Block cauldron = getBlockInFront(event.getBlock());
        ItemStack dispensedItem = event.getItem();
        if (settings.waterFillEnabled() && dispensedItem.getType() == Material.WATER_BUCKET
                && (cauldron.getType() == Material.CAULDRON || cauldron.getType() == Material.WATER_CAULDRON)) {
            fillCauldron(event, cauldron, dispensedItem, settings);
            return;
        }

        if (settings.bottleFillEnabled() && dispensedItem.getType() == Material.GLASS_BOTTLE) {
            ItemStack brew = BreweryIntegration.getBrew(cauldron);
            if (brew == null) return;

            event.setCancelled(true);
            if (settings.consumeBottleLevel()) removeOneCauldronLevel(cauldron);
            updateDispenserInventory(event.getBlock(), dispensedItem, brew, false, settings);
            playSound(cauldron, settings.bottleFillSound(), settings.bottleFillVolume(), settings.bottleFillPitch());
            return;
        }

        if (!settings.ingredientEnabled() || cauldron.getType() != Material.WATER_CAULDRON) return;
        if (!BreweryIntegration.addIngredientToCauldron(cauldron, dispensedItem)) return;

        event.setCancelled(true);
        updateDispenserInventory(event.getBlock(), dispensedItem,
                settings.returnEmptyContainers() ? getEmptyContainer(dispensedItem, settings) : null, false, settings);
    }

    private void fillCauldron(BlockDispenseEvent dispenseEvent, Block cauldron, ItemStack waterBucket,
                              PluginSettings settings) {
        // A full cauldron cannot accept more water; keep the bucket in the dispenser.
        dispenseEvent.setCancelled(true);
        if (cauldron.getBlockData() instanceof Levelled currentData
                && currentData.getLevel() == currentData.getMaximumLevel()) return;

        BlockState newState = cauldron.getState();
        newState.setType(Material.WATER_CAULDRON);
        Levelled waterData = (Levelled) newState.getBlockData();
        if (settings.fillToMaximum()) {
            waterData.setLevel(waterData.getMaximumLevel());
        } else if (waterData.getLevel() < waterData.getMaximumLevel()) {
            waterData.setLevel(waterData.getLevel() + 1);
        }
        newState.setBlockData(waterData);

        // Fire before updating so BreweryX sees the water increase and clears the
        // old brew, just as it does when a player refills a cauldron with a bucket.
        CauldronLevelChangeEvent levelEvent = new CauldronLevelChangeEvent(
                cauldron, null, CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY, newState);
        Bukkit.getPluginManager().callEvent(levelEvent);
        if (levelEvent.isCancelled() || !levelEvent.getNewState().update(true)) return;

        updateDispenserInventory(dispenseEvent.getBlock(), waterBucket,
                settings.returnEmptyContainers() ? new ItemStack(Material.BUCKET) : null, true, settings);
        playSound(cauldron, settings.bucketEmptySound(), settings.bucketEmptyVolume(), settings.bucketEmptyPitch());
    }

    private void playSound(Block cauldron, String sound, float volume, float pitch) {
        if (!plugin.getSettings().soundsEnabled()) return;
        cauldron.getWorld().playSound(cauldron.getLocation(), sound, volume, pitch);
    }

    private Block getBlockInFront(Block dispenser) {
        Directional directional = (Directional) dispenser.getBlockData();
        BlockFace facing = directional.getFacing();
        return dispenser.getRelative(facing);
    }

    private void removeOneCauldronLevel(Block cauldron) {
        Levelled cauldronData = (Levelled) cauldron.getBlockData();
        int waterLevel = cauldronData.getLevel();

        if (waterLevel == 1) {
            BreweryIntegration.removeCauldron(cauldron);
            cauldron.setType(Material.CAULDRON);
            return;
        }

        cauldronData.setLevel(waterLevel - 1);
        cauldron.setBlockData(cauldronData);
        Bukkit.getPluginManager().callEvent(new CauldronLevelChangeEvent(
                cauldron, null, CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL, cauldron.getState()));
    }

    private ItemStack getEmptyContainer(ItemStack ingredient, PluginSettings settings) {
        Material material = ingredient.getType();
        String materialName = material.name();
        for (String suffix : settings.bucketSuffixes()) {
            if (materialName.endsWith(suffix)) return new ItemStack(Material.BUCKET);
        }
        if (settings.bottleMaterialNames().contains(materialName)) return new ItemStack(Material.GLASS_BOTTLE);
        return null;
    }

    private void updateDispenserInventory(Block dispenserBlock, ItemStack consumedItem, ItemStack resultItem,
                                          boolean dropResult, PluginSettings settings) {
        Dispenser dispenser = (Dispenser) dispenserBlock.getState();
        Inventory inventory = dispenser.getInventory();
        plugin.getBlockedInventories().add(inventory);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!removeOneSimilarItem(inventory, consumedItem)) return;
                if (resultItem != null) {
                    if (dropResult) {
                        dispenser.getWorld().dropItem(dispenser.getLocation().add(
                                settings.dropOffsetX(), settings.dropOffsetY(), settings.dropOffsetZ()), resultItem);
                        return;
                    }
                    Map<Integer, ItemStack> leftovers = inventory.addItem(resultItem);
                    leftovers.values().forEach(item -> dispenser.getWorld().dropItem(
                            dispenser.getLocation().add(settings.dropOffsetX(), settings.dropOffsetY(), settings.dropOffsetZ()), item));
                }
            } finally {
                plugin.getBlockedInventories().remove(inventory);
            }
        }, settings.deferredInventoryDelayTicks());
    }

    private boolean removeOneSimilarItem(Inventory inventory, ItemStack consumedItem) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack inventoryItem = inventory.getItem(slot);
            if (inventoryItem == null || !inventoryItem.isSimilar(consumedItem)) continue;

            if (inventoryItem.getAmount() == 1) {
                inventory.setItem(slot, null);
            } else {
                inventoryItem.setAmount(inventoryItem.getAmount() - 1);
                inventory.setItem(slot, inventoryItem);
            }
            return true;
        }
        return false;
    }
}
