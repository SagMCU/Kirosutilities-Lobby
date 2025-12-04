package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sagmcu.KirosUtilities.Main;

public class ReloadCommand implements CommandExecutor {
    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String reloadSuccessMessage = plugin.getConfig().getString("messages.reload_success");
        String noPermissionMessage = plugin.getConfig().getString("messages.no_permission");

        reloadSuccessMessage = plugin.getChatColorTranslator().translate(reloadSuccessMessage);
        noPermissionMessage = plugin.getChatColorTranslator().translate(noPermissionMessage);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("kirosutilities.admin")) {
                player.sendMessage(noPermissionMessage);
                return true;
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.reloadTabFilterConfig();
            sender.sendMessage(reloadSuccessMessage);
            return true;
        }

        return false;
    }
}