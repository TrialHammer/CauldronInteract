package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.utils.BreweryIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryMoveItemListenerTest {
    private final CauldronInteract plugin = mock(CauldronInteract.class);
    private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    private final Inventory hopper = mock(Inventory.class);
    private final BrewerInventory inventory = mock(BrewerInventory.class);
    private final BrewingStand stand = mock(BrewingStand.class);
    private final Block block = mock(Block.class);
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<BreweryIntegration> brewery;
    private InventoryMoveItemListener listener;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        brewery = mockStatic(BreweryIntegration.class);
        when(plugin.getBlockedInventories()).thenReturn(new HashSet<>());
        when(inventory.getHolder()).thenReturn(stand);
        when(stand.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.BREWING_STAND);
        when(block.getState()).thenReturn(stand);
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(block.getWorld()).thenReturn(world);
        listener = new InventoryMoveItemListener(plugin);
    }

    @AfterEach
    void tearDown() {
        brewery.close();
        bukkit.close();
    }

    @Test
    void hopperInputsAreCheckedAfterTransferAndCoalescedWithinOneTick() {
        listener.onBrewerInput(input(Material.POTION));
        listener.onBrewerInput(input(Material.GLOWSTONE_DUST));
        brewery.verifyNoInteractions();

        pendingTask().run();

        brewery.verify(() -> BreweryIntegration.startDistillation(stand));
        listener.onBrewerInput(input(Material.BLAZE_POWDER));
        verify(scheduler, times(2)).runTaskLater(eq(plugin), any(Runnable.class), eq(1L));
    }

    @Test
    void transferCancelledLaterDoesNotStartDistillation() {
        InventoryMoveItemEvent event = input(Material.POTION);
        listener.onBrewerInput(event);
        event.setCancelled(true);
        pendingTask().run();
        brewery.verifyNoInteractions();
    }

    @Test
    void destroyedStandDoesNotStartDistillation() {
        listener.onBrewerInput(input(Material.POTION));
        when(block.getType()).thenReturn(Material.AIR);
        pendingTask().run();
        brewery.verifyNoInteractions();
    }

    @Test
    void dispenserInventoryRemainsProtectedDuringDeferredConsumption() {
        plugin.getBlockedInventories().add(hopper);
        InventoryMoveItemEvent event = input(Material.POTION);
        listener.onInventoryMoveItem(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void ordinaryInventoriesDoNotScheduleDistillation() {
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(hopper, new ItemStack(Material.POTION), mock(Inventory.class), true);
        listener.onBrewerInput(event);
        verifyNoInteractions(scheduler);
    }

    private InventoryMoveItemEvent input(Material material) {
        return new InventoryMoveItemEvent(hopper, new ItemStack(material), inventory, true);
    }

    private Runnable pendingTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), task.capture(), eq(1L));
        return task.getValue();
    }
}
