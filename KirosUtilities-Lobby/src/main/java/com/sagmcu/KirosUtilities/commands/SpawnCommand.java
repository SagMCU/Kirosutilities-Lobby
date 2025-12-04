package com.sagmcu.KirosUtilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sagmcu.KirosUtilities.Main;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor, TabCompleter, Listener {
    private final Main plugin;
    private final ChatColorTranslator chatColorTranslator;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, Location> initialLocations = new HashMap<>();

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
        this.chatColorTranslator = plugin.getChatColorTranslator();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("spawn.enabled", true)) {
            sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")));
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
            if (!sender.hasPermission("kirosutilities.spawns.others") && !(sender instanceof Player)) {
                sender.sendMessage(chatColorTranslator.translate(
                        config.getString("messages.no_permission", "&cNo tienes permisos para ejecutar ese comando.")));
                return true;
            }
            spawnName = args[0];
            targetPlayer = plugin.getServer().getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(chatColorTranslator.translate(
                        config.getString("messages.spawn_no_player", "&cPlayer &e{player} &cnot found.")
                                .replace("{player}", args[1])));
                return true;
            }
        } else {
            sender.sendMessage(chatColorTranslator.translate(
                    "&bUso: /spawn <spawn> <jugador>"));
            return true;
        }

        // Check spawn existence and permission
        if (!spawnsConfig.contains("spawns." + spawnName)) {
            sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.spawn_invalid",
                            "&cSpawn &e{spawn} &cdoes not exist or you lack permission.")
                            .replace("{spawn}", spawnName)));
            return true;
        }

        String permission = spawnsConfig.getString("spawns." + spawnName + ".permission");
        if (permission != null && !targetPlayer.hasPermission(permission)) {
            sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.spawn_invalid",
                            "&cSpawn &e{spawn} &cdoes not exist or you lack permission.")
                            .replace("{spawn}", spawnName)));
            return true;
        }

        // Load spawn location
        String worldName = spawnsConfig.getString("spawns." + spawnName + ".world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(chatColorTranslator.translate(
                    "&cWorld &e" + worldName + "&c not found for spawn &e" + spawnName + "&c."));
            return true;
        }

        double x = spawnsConfig.getDouble("spawns." + spawnName + ".x");
        double y = spawnsConfig.getDouble("spawns." + spawnName + ".y");
        double z = spawnsConfig.getDouble("spawns." + spawnName + ".z");
        float pitch = (float) spawnsConfig.getDouble("spawns." + spawnName + ".pitch");
        float yaw = (float) spawnsConfig.getDouble("spawns." + spawnName + ".yaw");

        final Location teleportLocation = new Location(world, x, y, z, yaw, pitch);

        // CASE 1: Teleporting another player → instant
        if (args.length == 2) {
            targetPlayer.teleport(teleportLocation);

            String senderName = (sender instanceof Player) ? ((Player) sender).getName() : "console";

            String targetMessageKey = spawnName.equals("Default") ? "messages.default_spawn_success"
                    : "messages.spawn_others_received";
            targetPlayer.sendMessage(chatColorTranslator.translate(
                    config.getString(targetMessageKey, "&aYou were teleported to spawn &e{spawn} &aby &e{player}&a.")
                            .replace("{spawn}", spawnName)
                            .replace("{player}", senderName)));

            sender.sendMessage(chatColorTranslator.translate(
                    config.getString("messages.spawn_others_success", "&aTeleported &e{player} &ato spawn &e{spawn}&a.")
                            .replace("{player}", targetPlayer.getName())
                            .replace("{spawn}", spawnName)));

            return true;
        }

        // CASE 2: Self-teleport
        final Player finalTarget = targetPlayer;
        final String finalSpawnName = spawnName;
        final int finalArgsLength = args.length;

        UUID playerId = finalTarget.getUniqueId();

        // Cancel existing
        BukkitTask existingTask = pendingTeleports.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
            pendingTeleports.remove(playerId);
            initialLocations.remove(playerId);
        }

        if (finalTarget.hasPermission("kirosutilities.bypass.wait")) {
            finalTarget.teleport(teleportLocation);
            String messageKey = (finalArgsLength == 0) ? "messages.default_spawn_success" : "messages.spawn_success";
            finalTarget.sendMessage(chatColorTranslator.translate(
                    config.getString(messageKey, "&aTeleported to spawn &e{spawn}&a.")
                            .replace("{spawn}", finalSpawnName)));
            return true;
        }

        int waitTime = config.getInt("spawn.spawn_wait_time", 3);
        if (waitTime <= 0) {
            finalTarget.teleport(teleportLocation);
            String messageKey = (finalArgsLength == 0) ? "messages.default_spawn_success" : "messages.spawn_success";
            finalTarget.sendMessage(chatColorTranslator.translate(
                    config.getString(messageKey, "&aTeleported to spawn &e{spawn}&a.")
                            .replace("{spawn}", finalSpawnName)));
            return true;
        }

        // Store initial location
        initialLocations.put(playerId, finalTarget.getLocation().clone());

        // Send wait message
        finalTarget.sendMessage(chatColorTranslator.translate(
                config.getString("messages.teleport_wait",
                        "&eTeleporting in &b{seconds} &e seconds. Do not move or take damage.")
                        .replace("{seconds}", String.valueOf(waitTime))));

        // Schedule teleport
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!finalTarget.isOnline()) {
                    cleanup(playerId);
                    return;
                }

                finalTarget.teleport(teleportLocation);

                String messageKey = (finalArgsLength == 0) ? "messages.default_spawn_success"
                        : "messages.spawn_success";
                finalTarget.sendMessage(chatColorTranslator.translate(
                        config.getString(messageKey, "&aTeleported to spawn &e{spawn}&a.")
                                .replace("{spawn}", finalSpawnName)));

                cleanup(playerId);
            }
        }.runTaskLater(plugin, waitTime * 20L);

        pendingTeleports.put(playerId, task);

        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!pendingTeleports.containsKey(playerId))
            return;

        Location initial = initialLocations.get(playerId);
        if (initial == null)
            return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null)
            return;

        // Check significant movement
        if (from.getWorld() != to.getWorld() ||
                Math.abs(from.getX() - to.getX()) > 0.5 ||
                Math.abs(from.getZ() - to.getZ()) > 0.5 ||
                Math.abs(from.getY() - to.getY()) > 0.1) {

            cancelTeleport(player, playerId, "moved");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        if (!pendingTeleports.containsKey(playerId))
            return;

        cancelTeleport(player, playerId, "took damage");
    }

    private void cancelTeleport(Player player, UUID playerId, String reason) {
        BukkitTask task = pendingTeleports.get(playerId);
        if (task != null) {
            task.cancel();
        }

        player.sendMessage(chatColorTranslator.translate(
                plugin.getConfig().getString("messages.teleport_canceled", "&cTeleport canceled: you {reason}.")
                        .replace("{reason}", reason)));

        cleanup(playerId);
    }

    private void cleanup(UUID playerId) {
        pendingTeleports.remove(playerId);
        initialLocations.remove(playerId);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        FileConfiguration spawnsConfig = plugin.getSpawnsConfig();

        if (args.length == 1) {
            Set<String> spawnNames = spawnsConfig.getConfigurationSection("spawns").getKeys(false);
            for (String spawn : spawnNames) {
                String permission = spawnsConfig.getString("spawns." + spawn + ".permission");
                if (permission == null || sender.hasPermission(permission)) {
                    suggestions.add(spawn);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("kirosutilities.spawns.others")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }

        return suggestions;
    }
}
