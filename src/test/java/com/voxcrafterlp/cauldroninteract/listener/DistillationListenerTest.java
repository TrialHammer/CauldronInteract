package com.voxcrafterlp.cauldroninteract.listener;

import com.dre.brewery.BDistiller;
import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DistillationListenerTest {
    private final CauldronInteract plugin = mock(CauldronInteract.class);
    private final World world = mock(World.class);
    private final Chunk chunk = mock(Chunk.class);
    private final Block block = mock(Block.class);
    private final BrewingStand stand = mock(BrewingStand.class);
    private final BrewerInventory inventory = mock(BrewerInventory.class);
    private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    private MockedStatic<Bukkit> bukkit;
    private DistillationListener listener;
    private Runnable check;

    @BeforeEach
    void setUp() {
        BDistiller.reset();
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getLogger).thenReturn(Logger.getAnonymousLogger());
        PluginManager manager = mock(PluginManager.class);
        Plugin brewery = mock(Plugin.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
        when(manager.getPlugin("BreweryX")).thenReturn(brewery);
        when(brewery.isEnabled()).thenReturn(true);
        when(world.getLoadedChunks()).thenReturn(new Chunk[]{chunk});
        when(chunk.getTileEntities()).thenReturn(new BlockState[]{stand});
        when(stand.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.BREWING_STAND);
        when(block.getState()).thenReturn(stand);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(stand.getInventory()).thenReturn(inventory);
        when(inventory.getIngredient()).thenReturn(new ItemStack(Material.GLOWSTONE_DUST));
        listener = new DistillationListener(plugin);
        listener.startWatching();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskTimer(eq(plugin), task.capture(), eq(10L), eq(10L));
        check = task.getValue();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void hopperInputStartsDistillationWithoutMoveEventAndKeepsExistingTimer() {
        BDistiller.brewState = 0;
        check.run();
        assertEquals(0, BDistiller.starts);

        // The hopper transfer completes without any event delivered to us.
        BDistiller.brewState = 2;
        check.run();
        check.run();

        assertTrue(BDistiller.isTrackingDistiller(block));
        assertEquals(1, BDistiller.starts);
    }

    @Test
    void glowstoneArrivingAfterDrinkStartsDistillationOnNextCheck() {
        when(inventory.getIngredient()).thenReturn(null);
        check.run();
        assertEquals(0, BDistiller.starts);

        when(inventory.getIngredient()).thenReturn(new ItemStack(Material.GLOWSTONE_DUST));
        check.run();

        assertEquals(1, BDistiller.starts);
    }

    @Test
    void chunkUnloadStopsChecksAndReloadRestoresThem() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        check.run();
        verify(block, never()).getState();
        assertEquals(0, BDistiller.starts);

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        listener.onChunkLoad(new ChunkLoadEvent(chunk, false));
        check.run();

        assertEquals(1, BDistiller.starts);
    }

    @Test
    void newlyPlacedStandIsWatchedWithoutInventoryEvents() {
        when(block.getType()).thenReturn(Material.AIR);
        check.run();
        when(block.getType()).thenReturn(Material.BREWING_STAND);
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);
        listener.onBlockPlace(event);

        check.run();

        assertEquals(1, BDistiller.starts);
    }

    @Test
    void failedStartIsRetriedOnNextCheck() {
        BDistiller.failStart = true;
        check.run();
        assertFalse(BDistiller.isTrackingDistiller(block));

        BDistiller.failStart = false;
        check.run();

        assertEquals(1, BDistiller.starts);
    }
}
