package com.sagmcu.KirosUtilities;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sagmcu.KirosUtilities.commands.FlyCommand;
import com.sagmcu.KirosUtilities.commands.ReloadCommand;
import com.sagmcu.KirosUtilities.commands.SetSpawnCommand;
import com.sagmcu.KirosUtilities.commands.SpawnCommand;
import com.sagmcu.KirosUtilities.commands.SpawnListCommand;
import com.sagmcu.KirosUtilities.filters.LegacyTabCompletionFilter;
import com.sagmcu.KirosUtilities.filters.TabCompletionFilter;
import com.sagmcu.KirosUtilities.handlers.JoinFullServers;
import com.sagmcu.KirosUtilities.handlers.PlayerJoinListener;
import com.sagmcu.KirosUtilities.utils.ChatColorTranslator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("unchecked") // Suppress type safety warnings for json-simple
public class Main extends JavaPlugin {
    private Object tabFilter;
    private String serverVersion;
    private ChatColorTranslator chatColorTranslator;
    private File spawnsFile;
    private FileConfiguration spawnsConfig;
    private File flyFile;
    private JSONObject flyConfig;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            initializeSpawnsConfig();
            initializeFlyConfig();
            // Detect server version
            serverVersion = getServer().getBukkitVersion().split("-")[0];
            chatColorTranslator = new ChatColorTranslator(this);
            // Load the appropriate filter based on version
            if (isModernVersion()) {
                tabFilter = new TabCompletionFilter(this);
                getServer().getPluginManager().registerEvents((TabCompletionFilter) tabFilter, this);
            } else {
                // For 1.8-1.12, check for ProtocolLib and use LegacyTabCompletionFilter
                Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
                if (protocolLib != null && protocolLib.isEnabled()) {
                    tabFilter = new LegacyTabCompletionFilter(this);
                    getLogger().info("ProtocolLib detected. Enabling tab completion filtering for 1.8 - 1.12.");
                } else {
                    tabFilter = null; // Disable filtering if ProtocolLib is missing
                    getLogger().warning("ProtocolLib not found or not enabled. Tab completion filtering for 1.8 - 1.12 will not work.");
                }
            }

            // Register commands
            getCommand("kirosutilities").setExecutor(new ReloadCommand(this));
            getCommand("spawn").setExecutor(new SpawnCommand(this));
            getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
            getCommand("spawnlist").setExecutor(new SpawnListCommand(this));
            getCommand("fly").setExecutor(new FlyCommand(this));
            // Register listeners
            getServer().getPluginManager().registerEvents(new JoinFullServers(), this);
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            String enabledMessage = chatColorTranslator.translate("&aKirosUtilities &7has been &aenabled&7! &6:D");
            this.getServer().getConsoleSender().sendMessage(enabledMessage);
        } catch (Exception e) {
            getLogger().severe("Failed to enable KirosUtilities: " + e.getMessage());
            e.printStackTrace();
            // Disable plugin to prevent partial initialization
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        String disabledMessage = "&cKirosUtilities &7has been &cdisabled&7! :(";
        if (chatColorTranslator != null) {
            disabledMessage = chatColorTranslator.translate(disabledMessage);
        }
        this.getServer().getConsoleSender().sendMessage(disabledMessage);
    }

    public Object getTabFilter() {
        return tabFilter;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public boolean isModernVersion() {
        try {
            String[] versionParts = serverVersion.split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);
            return major > 1 || (major == 1 && minor >= 13);
        } catch (Exception e) {
            return false; // Default to legacy if parsing fails
        }
    }

    public void reloadTabFilterConfig() {
        if (isModernVersion()) {
            ((TabCompletionFilter) tabFilter).reloadConfig();
        } else {
            ((LegacyTabCompletionFilter) tabFilter).reloadConfig();
        }
    }

    public ChatColorTranslator getChatColorTranslator() {
        return chatColorTranslator;
    }

    public FileConfiguration getSpawnsConfig() {
        return spawnsConfig;
    }

    public void saveSpawnsConfig() {
        try {
            spawnsConfig.save(spawnsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save spawns.yml: " + e.getMessage());
        }
    }

    public JSONObject getFlyConfig() {
        return flyConfig;
    }

    public void saveFlyConfig() {
        try (FileWriter writer = new FileWriter(flyFile)) {
            writer.write(flyConfig.toJSONString());
        } catch (IOException e) {
            getLogger().severe("Could not save fly.json: " + e.getMessage());
        }
    }

    private void initializeSpawnsConfig() {
        try {
            spawnsFile = new File(getDataFolder(), "spawns.yml");
            spawnsConfig = YamlConfiguration.loadConfiguration(spawnsFile);
            if (!spawnsFile.exists()) {
                spawnsFile.getParentFile().mkdirs();
                try {
                    saveResource("spawns.yml", false);
                    getLogger().info("Copied default spawns.yml from JAR.");
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Embedded spawns.yml not found in JAR. Creating a new one.");
                    spawnsFile.createNewFile();
                }
            }
            // Ensure default spawn exists
            if (!spawnsConfig.contains("spawns.Default")) {
                spawnsConfig.set("spawns.Default.world", "world");
                spawnsConfig.set("spawns.Default.x", 0.5);
                spawnsConfig.set("spawns.Default.y", 64.0);
                spawnsConfig.set("spawns.Default.z", 0.5);
                spawnsConfig.set("spawns.Default.pitch", 0.0);
                spawnsConfig.set("spawns.Default.yaw", 0.0);
                spawnsConfig.set("spawns.Default.permission", "kirosutilities.spawn.default");
                saveSpawnsConfig();
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize spawns.yml: " + e.getMessage());
            throw new RuntimeException("Spawns config initialization failed", e);
        }
    }

    private void initializeFlyConfig() {
        try {
            flyFile = new File(getDataFolder(), "fly.json");
            flyConfig = new JSONObject();
            if (!flyFile.exists()) {
                flyFile.getParentFile().mkdirs();
                flyFile.createNewFile();
                flyConfig.put("flight", new JSONObject());
                saveFlyConfig();
            } else {
                try (FileReader reader = new FileReader(flyFile)) {
                    JSONParser parser = new JSONParser();
                    flyConfig = (JSONObject) parser.parse(reader);
                    if (!flyConfig.containsKey("flight")) {
                        flyConfig.put("flight", new JSONObject());
                        saveFlyConfig();
                    }
                } catch (IOException | ParseException e) {
                    getLogger().severe("Could not load fly.json: " + e.getMessage());
                    flyConfig.put("flight", new JSONObject());
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize fly.json: " + e.getMessage());
            throw new RuntimeException("Fly config initialization failed", e);
        }
    }
}