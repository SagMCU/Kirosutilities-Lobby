package com.sagmcu.KirosUtilities.filters;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import com.sagmcu.KirosUtilities.Main;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class TabCompletionFilter implements Listener {

    private final Main plugin;
    private boolean enabled = false;
    private Set<String> defaultWhitelist;
    private List<CustomWhitelist> customWhitelists;

    public TabCompletionFilter(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        try {
            this.enabled = config.getBoolean("TabCompletionFilter.enabled", true);

            // Load default whitelist
            List<String> defaultList = config.getStringList("TabCompletionFilter.default_whitelist");
            this.defaultWhitelist = defaultList.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            // Load custom whitelists
            this.customWhitelists = config.getMapList("TabCompletionFilter.custom_whitelists").stream()
                    .map(map -> {
                        CustomWhitelist whitelist = new CustomWhitelist();
                        whitelist.setPermission((String) ((Map<String, Object>) map).get("permission"));
                        whitelist.setCommands(((List<String>) ((Map<String, Object>) map).get("commands")).stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet()));
                        whitelist.setIncludeDefault(
                                (Boolean) ((Map<String, Object>) map).getOrDefault("include_default", Boolean.FALSE));
                        return whitelist;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while processing TabCompletionFilter config", e);
            this.enabled = false;
        }
    }

    public void reloadConfig() {
        loadConfig(); // This will call the existing loadConfig method to refresh the configuration
    }

    @EventHandler
    public void onCommandTabComplete(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (!enabled || player.hasPermission("kirosutilities.TabCompletionFilter.exempt")) {
            return;
        }

        Set<String> commandsToRemove = event.getCommands().stream()
                .filter(command -> !shouldKeepCommand(player, command.toLowerCase()))
                .collect(Collectors.toSet());

        event.getCommands().removeAll(commandsToRemove);
    }

    private boolean shouldKeepCommand(Player player, String command) {
        boolean commandAllowed = false;
    
        for (CustomWhitelist whitelist : customWhitelists) {
            if (player.hasPermission(whitelist.getPermission())) {
                if (whitelist.isIncludeDefault()) {
                    // If include_default is true, allow commands from both default and custom whitelist if custom isn't empty
                    commandAllowed = whitelist.getCommands().contains(command) || defaultWhitelist.contains(command);
                } else {
                    // If include_default is false, only allow commands explicitly listed in custom whitelist
                    commandAllowed = whitelist.getCommands().contains(command);
                }
                // If custom whitelist is empty and include_default is false, no commands are allowed
                if (whitelist.getCommands().isEmpty() && !whitelist.isIncludeDefault()) {
                    commandAllowed = false;
                }
                return commandAllowed; // Return immediately after handling the matched whitelist
            }
        }
    
        // Only check default whitelist if no custom whitelist applied
        return defaultWhitelist.contains(command);
    }

    private static class CustomWhitelist {
        private String permission;
        private Set<String> commands;
        private boolean defaultWhitelist;

        // Getters and setters
        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public Set<String> getCommands() {
            return commands;
        }

        public void setCommands(Set<String> commands) {
            this.commands = commands;
        }

        public boolean isIncludeDefault() {
            return defaultWhitelist;
        }

        public void setIncludeDefault(boolean includeDefault) {
            this.defaultWhitelist = includeDefault;
        }
    }
}