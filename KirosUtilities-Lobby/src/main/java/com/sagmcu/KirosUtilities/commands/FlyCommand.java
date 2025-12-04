package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.sagmcu.KirosUtilities.Main;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class FlyCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final ChatColorTranslator chatColorTranslator;

    public FlyCommand(Main plugin) {
        this.plugin = plugin;
        this.chatColorTranslator = plugin.getChatColorTranslator();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        Player targetPlayer = null;
        boolean isConsole = !(sender instanceof Player);

        // Parse arguments
        if (args.length == 0) {
            if (isConsole) {
                sender.sendMessage(chatColorTranslator.translate(
                    "&cConsole must specify a player."
                ));
                return true;
            }
            targetPlayer = (Player) sender;
            if (!targetPlayer.hasPermission("kirosutilities.fly")) {
                targetPlayer.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
                ));
                return true;
            }
        } else if (args.length == 1) {
            if (!sender.hasPermission("kirosutilities.fly.others") && !isConsole) {
                sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")
                ));
                return true;
            }
            targetPlayer = plugin.getServer().getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.fly_no_player", "&cPlayer &e{player} &cnot found.")
                        .replace("{player}", args[0])
                ));
                return true;
            }
        } else {
            sender.sendMessage(chatColorTranslator.translate(
                "&cUsage: /fly [player]"
            ));
            return true;
        }

        // Toggle flight
        boolean newFlightState = !targetPlayer.getAllowFlight();
        targetPlayer.setAllowFlight(newFlightState);
        targetPlayer.setFlying(newFlightState);

        // Update fly.json for persistence
        JSONObject flyConfig = plugin.getFlyConfig();
        JSONObject flight = (JSONObject) flyConfig.getOrDefault("flight", new JSONObject());
        String uuid = targetPlayer.getUniqueId().toString();
        if (newFlightState && targetPlayer.hasPermission("kirosutilities.fly.persist")) {
            flight.put(uuid, true);
        } else {
            flight.remove(uuid);
        }
        flyConfig.put("flight", flight);
        plugin.saveFlyConfig();

        // Send messages
        String senderName = isConsole ? "console" : ((Player) sender).getName();
        if (sender == targetPlayer) {
            // Self-toggle message
            targetPlayer.sendMessage(chatColorTranslator.translate(
                config.getString(
                    newFlightState ? "messages.fly_enabled" : "messages.fly_disabled",
                    newFlightState ? "&aFlight enabled for &e{player}&a." : "&aFlight disabled for &e{player}&a."
                ).replace("{player}", targetPlayer.getName())
            ));
        } else {
            // Target receives "enabled/disabled by X"
            targetPlayer.sendMessage(chatColorTranslator.translate(
                config.getString(
                    newFlightState ? "messages.fly_others_enabled_received" : "messages.fly_others_disabled_received",
                    newFlightState ? "&aYour flight was enabled by &e{player}&a." : "&aYour flight was disabled by &e{player}&a."
                ).replace("{player}", senderName)
            ));
            // Sender receives "enabled/disabled fly to X"
            sender.sendMessage(chatColorTranslator.translate(
                config.getString(
                    newFlightState ? "messages.fly_others_enabled" : "messages.fly_others_disabled",
                    newFlightState ? "&aFlight enabled for &e{player}&a." : "&aFlight disabled for &e{player}&a."
                ).replace("{player}", targetPlayer.getName())
            ));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1 && (sender.hasPermission("kirosutilities.fly.others") || !(sender instanceof Player))) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }
}