package com.voxcrafterlp.cauldroninteract.command;

import com.voxcrafterlp.cauldroninteract.CauldronInteract;
import com.voxcrafterlp.cauldroninteract.config.PluginSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CauldronInteractCommand implements CommandExecutor, TabCompleter {

    private final CauldronInteract plugin;

    public CauldronInteractCommand(CauldronInteract plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessages().send(sender, "command.unknown-subcommand", Map.of());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                try {
                    plugin.reloadPluginConfiguration();
                    plugin.getMessages().send(sender, "command.reload-success", Map.of());
                } catch (RuntimeException exception) {
                    plugin.getMessages().send(sender, "command.reload-failed",
                            Map.of("error", String.valueOf(exception.getMessage())));
                }
            }
            case "status" -> sendStatus(sender);
            default -> plugin.getMessages().send(sender, "command.unknown-subcommand", Map.of());
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        PluginSettings settings = plugin.getSettings();
        plugin.getMessages().send(sender, "command.status-header",
                Map.of("version", plugin.getDescription().getVersion()));
        plugin.getMessages().send(sender, "command.status-features",
                Map.of(
                        "water_fill", enabled(settings.waterFillEnabled()),
                        "bottle_fill", enabled(settings.bottleFillEnabled()),
                        "ingredient", enabled(settings.ingredientEnabled()),
                        "distillation", enabled(settings.distillationEnabled()),
                        "upgrade", enabled(settings.dispenserUpgradeEnabled())
                ));
        plugin.getMessages().send(sender, "command.status-brewery",
                Map.of("brewery", plugin.isBreweryAvailable()
                        ? plugin.getMessages().rawText("command.status-enabled")
                        : plugin.getMessages().rawText("command.status-disabled")));
    }

    private String enabled(boolean value) {
        return value
                ? plugin.getMessages().rawText("command.status-feature-on")
                : plugin.getMessages().rawText("command.status-feature-off");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "status").stream().filter(value -> value.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
