package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import com.sagmcu.KirosUtilities.Main;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SetSpawnCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final ChatColorTranslator chatColorTranslator;

    public SetSpawnCommand(Main plugin) {
        this.plugin = plugin;
        this.chatColorTranslator = plugin.getChatColorTranslator();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("spawn.enabled", true)) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
            ));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.setspawn_console", "&cThis command cannot be run from console.")
            ));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("kirosutilities.setspawn")) {
            player.sendMessage(chatColorTranslator.translate(
                config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
            ));
            return true;
        }

        String spawnName = "Default";
        String permission = "kirosutilities.spawn.default";
        boolean overwrite = false;

        // Parse arguments
        if (args.length > 0) {
            if (!args[0].equalsIgnoreCase("--name")) {
                player.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.setspawn_invalid_args",
                        "&cInvalid arguments. Usage: /setspawn [--name <name>] [--permission <permission>] [--overwrite]")
                ));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.setspawn_invalid_args",
                        "&cInvalid arguments. Usage: /setspawn [--name <name>] [--permission <permission>] [--overwrite]")
                ));
                return true;
            }

            spawnName = args[1];

            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--permission") && i + 1 < args.length) {
                    permission = args[++i];
                    if (spawnName.equalsIgnoreCase("Default")) {
                        player.sendMessage(chatColorTranslator.translate(
                            "&cDefault spawn cannot have a custom permission."
                        ));
                        return true;
                    }
                } else if (args[i].equalsIgnoreCase("--overwrite")) {
                    overwrite = true;
                }
            }

            if (!spawnName.equalsIgnoreCase("Default") && permission.equals("kirosutilities.spawn.default")) {
                player.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.setspawn_invalid_args",
                        "&cNon-default spawns require a custom permission. Use --permission.")
                ));
                return true;
            }
        }

        // Check for existing spawn
        FileConfiguration spawnsConfig = plugin.getSpawnsConfig();
        String path = "spawns." + spawnName;
        if (spawnsConfig.contains(path) && !overwrite) {
            player.sendMessage(chatColorTranslator.translate(
                "&cSpawn &e{spawn}&c already exists. Use --overwrite to replace it."
                    .replace("{spawn}", spawnName)
            ));
            return true;
        }

        // Save spawn
        Location location = player.getLocation();
        spawnsConfig.set(path + ".world", location.getWorld().getName());
        spawnsConfig.set(path + ".x", location.getX());
        spawnsConfig.set(path + ".y", location.getY());
        spawnsConfig.set(path + ".z", location.getZ());
        spawnsConfig.set(path + ".pitch", location.getPitch());
        spawnsConfig.set(path + ".yaw", location.getYaw());
        spawnsConfig.set(path + ".permission", permission);
        plugin.saveSpawnsConfig();

        player.sendMessage(chatColorTranslator.translate(
            config.getString("messages.setspawn_success", "&aSpawn &e{spawn} &aset successfully.")
                .replace("{spawn}", spawnName)
        ));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("kirosutilities.setspawn")) {
            return suggestions;
        }

        if (args.length == 1) {
            suggestions.add("--name");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--name")) {
            suggestions.add("Default");
            // Suggest existing spawn names
            Set<String> spawnNames = plugin.getSpawnsConfig().getConfigurationSection("spawns").getKeys(false);
            suggestions.addAll(spawnNames);
        } else if (args.length == 3) {
            suggestions.add("--permission");
            suggestions.add("--overwrite");
        } else if (args.length == 4 && args[2].equalsIgnoreCase("--permission")) {
            suggestions.add("kirosutilities.spawn.");
        }

        return suggestions;
    }
}