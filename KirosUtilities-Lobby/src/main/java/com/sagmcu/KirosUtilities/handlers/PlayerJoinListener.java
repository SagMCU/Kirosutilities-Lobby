package com.sagmcu.KirosUtilities.handlers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;

import com.sagmcu.KirosUtilities.Main;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class PlayerJoinListener implements Listener {
    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        Player player = event.getPlayer();

        // Handle spawn teleportation
        if (config.getBoolean("spawn.enabled", true)) {
            FileConfiguration spawnsConfig = plugin.getSpawnsConfig();
            String spawnName;

            // Check for first join
            if (!player.hasPlayedBefore()) {
                spawnName = config.getString("spawn.first_spawn", "Default");
            } else if (config.getBoolean("spawn.spawn_on_join", true)) {
                spawnName = "Default";
            } else {
                spawnName = null;
            }

            if (spawnName != null && spawnsConfig.contains("spawns." + spawnName)) {
                String permission = spawnsConfig.getString("spawns." + spawnName + ".permission");
                if (player.hasPermission(permission)) {
                    String worldName = spawnsConfig.getString("spawns." + spawnName + ".world");
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("World " + worldName + " not found for spawn " + spawnName);
                    } else {
                        double x = spawnsConfig.getDouble("spawns." + spawnName + ".x");
                        double y = spawnsConfig.getDouble("spawns." + spawnName + ".y");
                        double z = spawnsConfig.getDouble("spawns." + spawnName + ".z");
                        float pitch = (float) spawnsConfig.getDouble("spawns." + spawnName + ".pitch");
                        float yaw = (float) spawnsConfig.getDouble("spawns." + spawnName + ".yaw");

                        Location location = new Location(world, x, y, z, yaw, pitch);
                        player.teleport(location);
                    }
                }
            }
        }

        // Handle flight persistence (Survival/Adventure only)
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) {
            JSONObject flyConfig = plugin.getFlyConfig();
            JSONObject flight = (JSONObject) flyConfig.getOrDefault("flight", new JSONObject());
            String uuid = player.getUniqueId().toString();
            if (player.hasPermission("kirosutilities.fly.persist") && flight.containsKey(uuid) && (Boolean) flight.get(uuid)) {
                player.setAllowFlight(true);
                player.setFlying(true);
            } else {
                player.setAllowFlight(false);
                player.setFlying(false);
                flight.remove(uuid);
                flyConfig.put("flight", flight);
                plugin.saveFlyConfig();
            }
        }
    }
}