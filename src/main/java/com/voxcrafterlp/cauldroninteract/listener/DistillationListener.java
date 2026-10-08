package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.utils.BreweryIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Checks live stand contents independently of hopper event delivery and timing. */
public final class DistillationListener implements Listener {

    private final CauldronInteract plugin;
    private final Set<Block> stands = new HashSet<>();

    public DistillationListener(CauldronInteract plugin) {
        this.plugin = plugin;
    }

    public void startWatching() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                trackChunk(chunk);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkStands, 10L, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        trackChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.BREWING_STAND) {
            stands.add(event.getBlockPlaced());
        }
    }

    private void trackChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof BrewingStand) {
                stands.add(state.getBlock());
            }
        }
    }

    private void checkStands() {
        Iterator<Block> iterator = stands.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            // Never load chunks just to check a distiller. ChunkLoadEvent will
            // register the stand again when its chunk returns.
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                    || block.getType() != Material.BREWING_STAND) {
                iterator.remove();
                continue;
            }
            BreweryIntegration.startDistillation((BrewingStand) block.getState());
        }
    }
}
