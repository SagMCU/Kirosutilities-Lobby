package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;

import com.sagmcu.KirosUtilities.Main;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpawnCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final ChatColorTranslator chatColorTranslator;

    public SpawnCommand(Main plugin) {
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

        FileConfiguration spawnsConfig = plugin.getSpawnsConfig();
        String spawnName = "Default";
        Player targetPlayer = null;

        // Parse arguments
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(chatColorTranslator.translate("&cConsole must specify a spawn and player."));
                return true;
            }
            targetPlayer = (Player) sender;
        } else if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(chatColorTranslator.translate("&cConsole must specify a player."));
                return true;
            }
            spawnName = args[0];
            targetPlayer = (Player) sender;
        } else if (args.length == 2) {
            if (!sender.hasPermission("kirosutilities.spawns.others") && !!(sender instanceof Player)) {
                sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
                ));
                return true;
            }
            spawnName = args[0];
            targetPlayer = plugin.getServer().getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.spawn_no_player", "&cPlayer &e{player} &cnot found.")
                        .replace("{player}", args[1])
                ));
                return true;
            }
        } else {
            sender.sendMessage(chatColorTranslator.translate(
                "&bUso: /spawn <spawn> <jugador>"
            ));
            return true;
        }

        // Check spawn existence and permission
        if (!spawnsConfig.contains("spawns." + spawnName)) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.spawn_invalid", "&cSpawn &e{spawn} &cdoes not exist or you lack permission.")
                    .replace("{spawn}", spawnName)
            ));
            return true;
        }

        String permission = spawnsConfig.getString("spawns." + spawnName + ".permission");
        if (!targetPlayer.hasPermission(permission)) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.spawn_invalid", "&cSpawn &e{spawn} &cdoes not exist or you lack permission.")
                    .replace("{spawn}", spawnName)
            ));
            return true;
        }

        // Load spawn location
        String worldName = spawnsConfig.getString("spawns." + spawnName + ".world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(chatColorTranslator.translate(
                "&cWorld &e" + worldName + "&c not found for spawn &e" + spawnName + "&c."
            ));
            return true;
        }

        double x = spawnsConfig.getDouble("spawns." + spawnName + ".x");
        double y = spawnsConfig.getDouble("spawns." + spawnName + ".y");
        double z = spawnsConfig.getDouble("spawns." + spawnName + ".z");
        float pitch = (float) spawnsConfig.getDouble("spawns." + spawnName + ".pitch");
        float yaw = (float) spawnsConfig.getDouble("spawns." + spawnName + ".yaw");

        Location location = new Location(world, x, y, z, yaw, pitch);
        targetPlayer.teleport(location);

        // Send messages
        String senderName = (sender instanceof Player) ? ((Player) sender).getName() : "console";
        if (sender == targetPlayer) {
            // Self-teleport message
            String messageKey = (args.length == 0) ? "messages.default_spawn_success" : "messages.spawn_success";
            targetPlayer.sendMessage(chatColorTranslator.translate(
                config.getString(messageKey, "&aTeleported to spawn &e{spawn}&a.")
                    .replace("{spawn}", spawnName)
            ));
        } else {
            // Target receives "teleported by X"
            String messageKey = (args.length == 2 && spawnName.equals("Default")) ? "messages.default_spawn_success" : "messages.spawn_others_received";
            targetPlayer.sendMessage(chatColorTranslator.translate(
                config.getString(messageKey, "&aYou were teleported to spawn &e{spawn} &aby &e{player}&a.")
                    .replace("{spawn}", spawnName)
                    .replace("{player}", senderName)
            ));
            // Sender receives "teleported X to spawn"
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.spawn_others_success", "&aTeleported &e{player} &ato spawn &e{spawn}&a.")
                    .replace("{player}", targetPlayer.getName())
                    .replace("{spawn}", spawnName)
            ));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        FileConfiguration spawnsConfig = plugin.getSpawnsConfig();

        if (args.length == 1) {
            // Suggest spawn names for all users
            Set<String> spawnNames = spawnsConfig.getConfigurationSection("spawns").getKeys(false);
            for (String spawn : spawnNames) {
                String permission = spawnsConfig.getString("spawns." + spawn + ".permission");
                if (sender.hasPermission(permission)) {
                    suggestions.add(spawn);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("kirosutilities.spawns.others")) {
            // Suggest player names
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }

        return suggestions;
    }
}