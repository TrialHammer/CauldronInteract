package com.voxcrafterlp.cauldroninteract.utils;

import com.dre.brewery.BDistiller;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BreweryIntegrationTest {
    private final Plugin breweryPlugin = mock(Plugin.class);
    private final PluginManager manager = mock(PluginManager.class);
    private final BrewingStand stand = mock(BrewingStand.class);
    private final Block block = mock(Block.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        BDistiller.reset();
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
        bukkit.when(Bukkit::getLogger).thenReturn(Logger.getAnonymousLogger());
        when(manager.getPlugin("BreweryX")).thenReturn(breweryPlugin);
        when(breweryPlugin.isEnabled()).thenReturn(true);
        when(stand.getBlock()).thenReturn(block);
        BrewerInventory inventory = mock(BrewerInventory.class);
        when(inventory.getIngredient()).thenReturn(new ItemStack(Material.GLOWSTONE_DUST));
        when(stand.getInventory()).thenReturn(inventory);
        when(stand.getFuelLevel()).thenReturn(7);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void registersTimerForBreweryOutputLockAndKeepsProgressOnFurtherInputs() {
        BreweryIntegration.startDistillation(stand);
        BreweryIntegration.startDistillation(stand);
        assertTrue(BDistiller.isTrackingDistiller(block));
        assertEquals(1, BDistiller.starts);
        assertEquals(7, BDistiller.lastFuel);
    }

    @Test
    void ordinaryPotionsDoNotStartCustomDistillation() {
        BDistiller.brewState = 0;
        BreweryIntegration.startDistillation(stand);
        assertFalse(BDistiller.isTrackingDistiller(block));
        assertEquals(0, BDistiller.starts);
    }

    @Test
    void missingGlowstoneDoesNotStartTimer() {
        when(stand.getInventory().getIngredient()).thenReturn(null);
        BreweryIntegration.startDistillation(stand);
        assertFalse(BDistiller.isTrackingDistiller(block));
        assertEquals(0, BDistiller.starts);
    }

    @Test
    void brewMissingGlowstoneOrAlreadyFinishedDoesNotStartTimer() {
        BDistiller.brewState = 1;
        BreweryIntegration.startDistillation(stand);
        assertFalse(BDistiller.isTrackingDistiller(block));
    }

    @Test
    void failedTimerStartRollsBackOutputLockAndAllowsRetry() {
        BDistiller.failStart = true;
        BreweryIntegration.startDistillation(stand);
        assertFalse(BDistiller.isTrackingDistiller(block));
        BDistiller.failStart = false;
        BreweryIntegration.startDistillation(stand);
        assertEquals(1, BDistiller.starts);
    }

    @Test
    void absentOrDisabledBreweryDoesNotStartTimer() {
        when(manager.getPlugin("BreweryX")).thenReturn(null);
        BreweryIntegration.startDistillation(stand);
        when(manager.getPlugin("BreweryX")).thenReturn(breweryPlugin);
        when(breweryPlugin.isEnabled()).thenReturn(false);
        BreweryIntegration.startDistillation(stand);
        assertEquals(0, BDistiller.starts);
        verifyNoInteractions(stand);
    }
}
