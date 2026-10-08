package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.MockMakers;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlockDispenseListenerTest {
    private final CauldronInteract plugin = mock(CauldronInteract.class);
    private final Block dispenserBlock = mock(Block.class);
    private final Block cauldron = mock(Block.class);
    private final Dispenser dispenser = mock(Dispenser.class);
    private final Inventory inventory = mock(Inventory.class);
    private final PluginManager manager = mock(PluginManager.class);
    private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    private final World world = mock(World.class);
    private final BlockState newState = mock(BlockState.class);
    private final Levelled waterData = mock(Levelled.class);
    private MockedStatic<Bukkit> bukkit;
    private BlockDispenseListener listener;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        // Modern Spigot initializes Sound through server-provided registries.
        // Proxy mocks avoid initializing those interfaces before this bootstrap.
        bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(call -> {
            Class<?> entryType = call.getArgument(0);
            return mock(Registry.class, withSettings().mockMaker(MockMakers.PROXY).defaultAnswer(entry -> {
                if (entry.getMethod().getName().equals("get")) {
                    return mock(entryType, withSettings().mockMaker(MockMakers.PROXY));
                }
                return RETURNS_DEFAULTS.answer(entry);
            }));
        });
        when(plugin.getBlockedInventories()).thenReturn(new HashSet<>());
        when(dispenserBlock.getType()).thenReturn(Material.DISPENSER);
        Directional facing = mock(Directional.class);
        when(facing.getFacing()).thenReturn(BlockFace.NORTH);
        when(dispenserBlock.getBlockData()).thenReturn(facing);
        when(dispenserBlock.getRelative(BlockFace.NORTH)).thenReturn(cauldron);
        when(dispenserBlock.getState()).thenReturn(dispenser);
        when(dispenser.getInventory()).thenReturn(inventory);
        when(dispenser.getWorld()).thenReturn(world);
        when(dispenser.getLocation()).thenAnswer(inv -> new Location(world, 0, 64, 0));
        when(cauldron.getWorld()).thenReturn(world);
        when(cauldron.getLocation()).thenReturn(new Location(world, 0, 64, -1));
        when(cauldron.getType()).thenReturn(Material.CAULDRON);
        when(cauldron.getState()).thenReturn(newState);
        when(newState.getBlockData()).thenReturn(waterData);
        when(newState.update(true)).thenReturn(true);
        when(waterData.getMaximumLevel()).thenReturn(3);
        listener = new BlockDispenseListener(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void emptyCauldronFillsAndDropsExactlyOneEmptyBucketAfterConsumingInput() {
        BlockDispenseEvent event = waterBucketEvent();
        ItemStack bucket = mock(ItemStack.class);
        when(bucket.isSimilar(any())).thenReturn(true);
        when(bucket.getAmount()).thenReturn(1);
        when(inventory.getSize()).thenReturn(9);
        when(inventory.getItem(2)).thenReturn(bucket);

        listener.onBlockDispense(event);

        assertTrue(event.isCancelled());
        verify(newState).setType(Material.WATER_CAULDRON);
        verify(waterData).setLevel(3);
        var order = inOrder(manager, newState);
        ArgumentCaptor<CauldronLevelChangeEvent> level = ArgumentCaptor.forClass(CauldronLevelChangeEvent.class);
        order.verify(manager).callEvent(level.capture());
        order.verify(newState).update(true);
        assertEquals(CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY, level.getValue().getReason());
        assertTrue(plugin.getBlockedInventories().contains(inventory));
        verify(world, never()).dropItem(any(), any());

        runInventoryUpdate();

        verify(inventory).setItem(2, null);
        ArgumentCaptor<ItemStack> output = ArgumentCaptor.forClass(ItemStack.class);
        verify(world).dropItem(eq(new Location(world, 0.5, 63.25, 0.5)), output.capture());
        assertEquals(Material.BUCKET, output.getValue().getType());
        assertEquals(1, output.getValue().getAmount());
        assertFalse(plugin.getBlockedInventories().contains(inventory));
    }

    @Test
    void partiallyFilledCauldronAlsoFillsToMaximum() {
        Levelled current = mock(Levelled.class);
        when(current.getLevel()).thenReturn(1);
        when(current.getMaximumLevel()).thenReturn(3);
        when(cauldron.getType()).thenReturn(Material.WATER_CAULDRON);
        when(cauldron.getBlockData()).thenReturn(current);

        listener.onBlockDispense(waterBucketEvent());

        verify(waterData).setLevel(3);
        verify(newState).update(true);
    }

    @Test
    void fullCauldronKeepsWaterBucketWithoutChangingWaterOrInventory() {
        Levelled current = mock(Levelled.class);
        when(current.getLevel()).thenReturn(3);
        when(current.getMaximumLevel()).thenReturn(3);
        when(cauldron.getType()).thenReturn(Material.WATER_CAULDRON);
        when(cauldron.getBlockData()).thenReturn(current);
        BlockDispenseEvent event = waterBucketEvent();

        listener.onBlockDispense(event);

        assertTrue(event.isCancelled());
        verifyNoInteractions(manager, scheduler, inventory, newState);
    }

    @Test
    void cancelledLevelChangeKeepsWaterBucketAndDoesNotApplyNewState() {
        doAnswer(call -> {
            ((CauldronLevelChangeEvent) call.getArgument(0)).setCancelled(true);
            return null;
        }).when(manager).callEvent(any());

        listener.onBlockDispense(waterBucketEvent());

        verify(newState, never()).update(anyBoolean());
        verifyNoInteractions(scheduler, inventory);
    }

    @Test
    void missingInputDoesNotProduceAnExtraBucket() {
        listener.onBlockDispense(waterBucketEvent());
        runInventoryUpdate();
        verify(world, never()).dropItem(any(), any());
        assertTrue(plugin.getBlockedInventories().isEmpty());
    }

    @Test
    void waterBucketDoesNotReplaceLavaCauldron() {
        when(cauldron.getType()).thenReturn(Material.LAVA_CAULDRON);
        BlockDispenseEvent event = waterBucketEvent();
        listener.onBlockDispense(event);
        assertFalse(event.isCancelled());
        verifyNoInteractions(manager, scheduler, newState);
    }

    private BlockDispenseEvent waterBucketEvent() {
        return new BlockDispenseEvent(dispenserBlock, new ItemStack(Material.WATER_BUCKET), new org.bukkit.util.Vector());
    }

    private void runInventoryUpdate() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), task.capture(), eq(1L));
        task.getValue().run();
    }
}
