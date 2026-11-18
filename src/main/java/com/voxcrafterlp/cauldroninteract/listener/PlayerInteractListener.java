package com.voxcrafterlp.cauldroninteract.listener;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.config.PluginSettings;
import org.bukkit.GameMode;
import org.bukkit.Material;
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

import java.util.Map;

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
        if (!org.bukkit.Tag.ITEMS_HOES.isTagged(hoe.getType())) return;

        Dispenser dispenser = (Dispenser) event.getClickedBlock().getState();
        if (dispenser.getPersistentDataContainer().has(plugin.getDispenserUpgradedKey())) return;

        dispenser.getPersistentDataContainer().set(
                plugin.getDispenserUpgradedKey(), PersistentDataType.BYTE, (byte) 1);
        dispenser.update(true);
        playUpgradeEffects(event.getPlayer(), dispenser);
        if (plugin.getSettings().upgradeMessageEnabled()) {
            plugin.getMessages().send(event.getPlayer(), "dispenser.upgraded", Map.of());
        }
        damageHoe(event.getPlayer(), hoe);
    }

    private void playUpgradeEffects(Player player, Dispenser dispenser) {
        PluginSettings settings = plugin.getSettings();
        if (settings.soundsEnabled()) {
            player.playSound(dispenser.getLocation(), settings.upgradeSound(),
                    settings.upgradeSoundVolume(), settings.upgradeSoundPitch());
        }
        if (settings.upgradeParticlesEnabled() && settings.upgradeParticleCount() > 0) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(settings.upgradeParticle());
                player.spawnParticle(particle,
                        dispenser.getLocation().add(settings.upgradeParticleOffsetX(),
                                settings.upgradeParticleOffsetY(), settings.upgradeParticleOffsetZ()),
                        settings.upgradeParticleCount());
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown upgrade particle: " + settings.upgradeParticle());
            }
        }
    }

    private void damageHoe(Player player, ItemStack hoe) {
        if (!plugin.getSettings().upgradeConsumeDurability() || player.getGameMode() == GameMode.CREATIVE) return;

        ItemMeta itemMeta = hoe.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable)) return;

        damageable.setDamage(damageable.getDamage() + 1);
        hoe.setItemMeta(itemMeta);
    }
}
