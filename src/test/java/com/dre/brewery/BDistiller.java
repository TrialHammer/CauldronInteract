package com.dre.brewery;

import org.bukkit.block.Block;
import org.bukkit.inventory.BrewerInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Minimal test double for BreweryX's reflected distiller entry points. */
public class BDistiller {
    private static final Map<Block, BDistiller> trackedDistillers = new ConcurrentHashMap<>();
    public static byte brewState;
    public static int starts;
    public static int lastFuel;
    public static boolean failStart;
    private final Block block;

    public BDistiller(Block block, int fuel) {
        this.block = block;
        lastFuel = fuel;
    }

    public static boolean isTrackingDistiller(Block block) {
        return trackedDistillers.containsKey(block);
    }

    public static Object[] getDistillContents(BrewerInventory inventory) {
        return new Object[3];
    }

    public static byte hasBrew(BrewerInventory inventory, Object[] contents) {
        return brewState;
    }

    public void start() {
        if (!isTrackingDistiller(block)) throw new IllegalStateException("Distiller not registered");
        if (failStart) throw new IllegalStateException("Timer unavailable");
        starts++;
    }

    public static void reset() {
        trackedDistillers.clear();
        brewState = 2;
        starts = 0;
        lastFuel = -1;
        failStart = false;
    }
}
