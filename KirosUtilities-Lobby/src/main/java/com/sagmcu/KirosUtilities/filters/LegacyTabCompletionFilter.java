package com.sagmcu.KirosUtilities.filters;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sagmcu.KirosUtilities.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LegacyTabCompletionFilter {

    private final Main plugin;
    private boolean enabled = false;
    private Set<String> defaultWhitelist;
    private List<CustomWhitelist> customWhitelists;

    public LegacyTabCompletionFilter(Main plugin) {
        this.plugin = plugin;
        loadConfig();
        registerProtocolLibListener();
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        try {
            this.enabled = config.getBoolean("TabCompletionFilter.enabled", true);

            List<String> defaultList = config.getStringList("TabCompletionFilter.default_whitelist");
            this.defaultWhitelist = defaultList.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

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
            plugin.getLogger().log(Level.SEVERE, "An error occurred while processing LegacyTabCompletionFilter config",
                    e);
            this.enabled = false;
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void registerProtocolLibListener() {
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            plugin.getLogger().warning("ProtocolLib not found or not enabled. Tab completion filtering will not work.");
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.TAB_COMPLETE) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (!enabled || event.getPlayer().hasPermission("kirosutilities.TabCompletionFilter.exempt")) {
                            return;
                        }

                        Player player = event.getPlayer();
                        String[] completionsArray = event.getPacket().getStringArrays().read(0);
                        if (completionsArray == null)
                            return;

                        List<String> completions = new ArrayList<>(Arrays.asList(completionsArray));
                        List<String> filteredCompletions = new ArrayList<>();

                        for (String command : completions) {
                            if (shouldKeepCommand(player, command)) {
                                filteredCompletions.add(command);
                            }
                        }

                        event.getPacket().getStringArrays().write(0, filteredCompletions.toArray(new String[0]));
                    }
                });
    }

    private boolean shouldKeepCommand(Player player, String command) {
        command = command.trim().toLowerCase();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        boolean commandAllowed = false;

        for (CustomWhitelist whitelist : customWhitelists) {
            if (player.hasPermission(whitelist.getPermission())) {
                if (whitelist.isIncludeDefault()) {
                    commandAllowed = whitelist.getCommands().contains(command) || defaultWhitelist.contains(command);
                } else {
                    commandAllowed = whitelist.getCommands().contains(command);
                }
                if (whitelist.getCommands().isEmpty() && !whitelist.isIncludeDefault()) {
                    commandAllowed = false;
                }
                return commandAllowed;
            }
        }

        return defaultWhitelist.contains(command);
    }

    private static class CustomWhitelist {
        private String permission;
        private Set<String> commands;
        private boolean defaultWhitelist;

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