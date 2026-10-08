package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlayerInteractListener implements Listener {

    private final CauldronInteract plugin;

    public PlayerInteractListener(CauldronInteract plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.DISPENSER || !event.getPlayer().isSneaking()) return;

        ItemStack hoe = event.getPlayer().getInventory().getItemInMainHand();
        if (!Tag.ITEMS_HOES.isTagged(hoe.getType())) return;

        Dispenser dispenser = (Dispenser) event.getClickedBlock().getState();
        if (dispenser.getPersistentDataContainer().has(plugin.getDispenserUpgradedKey())) return;

        dispenser.getPersistentDataContainer().set(
                plugin.getDispenserUpgradedKey(), PersistentDataType.BYTE, (byte) 1);
        dispenser.update(true);
        playUpgradeEffects(event.getPlayer(), dispenser);
        damageHoe(event.getPlayer(), hoe);
    }

    private void playUpgradeEffects(Player player, Dispenser dispenser) {
        player.playSound(dispenser.getLocation(), Sound.ITEM_HONEYCOMB_WAX_ON, 1, 1);
        player.spawnParticle(Particle.WAX_ON, dispenser.getLocation().add(0.5, 1.1, 0.5), 5);
    }

    private void damageHoe(Player player, ItemStack hoe) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemMeta itemMeta = hoe.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable)) return;

        damageable.setDamage(damageable.getDamage() + 1);
        hoe.setItemMeta(itemMeta);
    }
}
