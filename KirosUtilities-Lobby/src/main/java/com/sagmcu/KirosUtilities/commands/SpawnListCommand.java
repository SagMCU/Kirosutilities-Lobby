package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.sagmcu.KirosUtilities.Main;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import java.util.Set;

public class SpawnListCommand implements CommandExecutor {
    private final Main plugin;
    private final ChatColorTranslator chatColorTranslator;

    public SpawnListCommand(Main plugin) {
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

        if (sender instanceof Player && !sender.hasPermission("kirosutilities.spawnlist")) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
            ));
            return true;
        }

        FileConfiguration spawnsConfig = plugin.getSpawnsConfig();
        Set<String> spawnNames = spawnsConfig.getConfigurationSection("spawns").getKeys(false);
        boolean hasSpawns = false;

        sender.sendMessage(chatColorTranslator.translate(
            config.getString("messages.spawnlist_header", "&aAvailable spawns:")
        ));

        for (String spawn : spawnNames) {
            String permission = spawnsConfig.getString("spawns." + spawn + ".permission");
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("kirosutilities.setspawn") || player.hasPermission(permission)) {
                    sender.sendMessage(chatColorTranslator.translate(
                        config.getString("messages.spawnlist_entry", "&e{spawn}")
                            .replace("{spawn}", spawn)
                    ));
                    hasSpawns = true;
                }
            } else {
                sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.spawnlist_entry", "&e{spawn}")
                        .replace("{spawn}", spawn)
                ));
                hasSpawns = true;
            }
        }

        if (!hasSpawns) {
            sender.sendMessage(chatColorTranslator.translate(
                config.getString("messages.spawnlist_empty", "&cNo spawns available.")
            ));
        }

        return true;
    }
}