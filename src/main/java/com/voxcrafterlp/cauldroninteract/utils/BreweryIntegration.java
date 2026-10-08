package com.voxcrafterlp.cauldroninteract.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Reflection-based BreweryX integration. CauldronInteract has no compile-time
 * dependency on BreweryX and remains inert when BreweryX is not installed.
 */
public final class BreweryIntegration {

    private static final String BREWERY_PLUGIN_NAME = "BreweryX";
    private static final String BREWERY_API_CLASS = "com.dre.brewery.api.BreweryApi";
    private static final String BREWERY_CAULDRON_CLASS = "com.dre.brewery.BCauldron";
    private static final String BREWERY_DISTILLER_CLASS = "com.dre.brewery.BDistiller";
    private static final Set<String> LOGGED_FAILURES = new HashSet<>();

    private BreweryIntegration() {
    }

    /**
     * Creates the BreweryX brew without changing the cauldron. The caller is
     * responsible for applying the same water-level sequence as the old plugin.
     */
    public static ItemStack getBrew(Block cauldronBlock) {
        Object breweryCauldron = getBreweryCauldron(cauldronBlock);
        if (breweryCauldron == null) return null;

        try {
            Object ingredients = breweryCauldron.getClass().getMethod("getIngredients").invoke(breweryCauldron);
            int state = (int) breweryCauldron.getClass().getMethod("getState").invoke(breweryCauldron);
            Method cook = ingredients.getClass().getMethod("cook", int.class, Class.forName("org.bukkit.entity.Player"));
            return (ItemStack) cook.invoke(ingredients, state, null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            logFailureOnce("create brew", e);
            return null;
        }
    }

    /**
     * Lets BreweryX validate and add one ingredient. BreweryX creates its
     * cauldron data when this is the first accepted ingredient.
     */
    public static boolean addIngredientToCauldron(Block cauldronBlock, ItemStack ingredient) {
        if (!isBreweryEnabled()) return false;

        try {
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            Class<?> cauldronClass = Class.forName(BREWERY_CAULDRON_CLASS);
            Method ingredientAdd = cauldronClass.getMethod(
                    "ingredientAdd", Block.class, ItemStack.class, playerClass);
            return (boolean) ingredientAdd.invoke(null, cauldronBlock, ingredient, null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            logFailureOnce("add ingredient", e);
            return false;
        }
    }

    private static Object getBreweryCauldron(Block block) {
        if (!isBreweryEnabled()) return null;

        try {
            Class<?> apiClass = Class.forName(BREWERY_API_CLASS);
            return apiClass.getMethod("getCauldron", Block.class).invoke(null, block);
        } catch (ReflectiveOperationException e) {
            logFailureOnce("detect cauldron", e);
            return null;
        }
    }

    public static void removeCauldron(Block block) {
        if (!isBreweryEnabled()) return;

        try {
            Class<?> apiClass = Class.forName(BREWERY_API_CLASS);
            apiClass.getMethod("removeCauldron", Block.class).invoke(null, block);
        } catch (ReflectiveOperationException e) {
            logFailureOnce("remove cauldron", e);
        }
    }

    /**
     * Starts BreweryX's own timer after an automated brewing-stand input. Already
     * running distillers are left alone so successive hopper inputs do not reset
     * progress. BreweryX retains control over recipes, duration and output locks.
     */
    @SuppressWarnings("unchecked")
    public static void startDistillation(BrewingStand stand) {
        Plugin brewery = Bukkit.getPluginManager().getPlugin(BREWERY_PLUGIN_NAME);
        if (brewery == null || !brewery.isEnabled()) return;

        try {
            Class<?> distillerClass = Class.forName(
                    BREWERY_DISTILLER_CLASS, true, brewery.getClass().getClassLoader());
            Block block = stand.getBlock();
            if ((boolean) distillerClass.getMethod("isTrackingDistiller", Block.class).invoke(null, block)) return;

            BrewerInventory inventory = stand.getInventory();
            ItemStack ingredient = inventory.getIngredient();
            if (ingredient == null || ingredient.getType() != Material.GLOWSTONE_DUST) return;
            Object contents = distillerClass.getMethod("getDistillContents", BrewerInventory.class)
                    .invoke(null, inventory);
            Number hasBrew = (Number) distillerClass.getMethod("hasBrew", BrewerInventory.class, contents.getClass())
                    .invoke(null, inventory, contents);
            if (hasBrew.intValue() != 2) return;

            // BreweryX exposes no start-by-block API. Register in the same map
            // used by distillerClick, so its hopper extraction lock and cleanup
            // work without fabricating a player inventory click.
            Field trackedField = distillerClass.getDeclaredField("trackedDistillers");
            trackedField.setAccessible(true);
            Map<Block, Object> tracked = (Map<Block, Object>) trackedField.get(null);
            Object distiller = distillerClass.getConstructor(Block.class, int.class)
                    .newInstance(block, stand.getFuelLevel());
            if (tracked.putIfAbsent(block, distiller) != null) return;
            try {
                distillerClass.getMethod("start").invoke(distiller);
            } catch (ReflectiveOperationException | RuntimeException e) {
                tracked.remove(block, distiller);
                throw e;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            logFailureOnce("start distillation", e);
        }
    }

    private static boolean isBreweryEnabled() {
        Plugin brewery = Bukkit.getPluginManager().getPlugin(BREWERY_PLUGIN_NAME);
        return brewery != null && brewery.isEnabled();
    }

    private static void logFailureOnce(String operation, Throwable exception) {
        if (!LOGGED_FAILURES.add(operation)) return;

        Throwable cause = exception instanceof InvocationTargetException && exception.getCause() != null
                ? exception.getCause()
                : exception;
        Bukkit.getLogger().log(Level.WARNING,
                "[CauldronInteract] BreweryX integration could not " + operation, cause);
    }
}
