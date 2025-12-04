package com.sagmcu.KirosUtilities.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;

import com.sagmcu.KirosUtilities.Main;

public class ChatColorTranslator {

    private final Main plugin;
    private final boolean supportsHexColors;

    public ChatColorTranslator(Main plugin) {
        this.plugin = plugin;
        this.supportsHexColors = isVersionAtLeast("1.16");
    }

    public String translate(String input) {
        if (input == null) return "";
        input = translateHexColors(input);
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String translateHexColors(String input) {
        if (!supportsHexColors) {
            return convertHexToLegacy(input);
        }

        try {
            String regex = "(#(?:[0-9a-fA-F]{6}))";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);

            StringBuffer result = new StringBuffer();
            Method ofMethod = ChatColor.class.getMethod("of", String.class); // Reflection to get ChatColor.of
            while (matcher.find()) {
                String hex = matcher.group(1);
                String chatColor = ofMethod.invoke(null, hex).toString(); // Invoke statically
                matcher.appendReplacement(result, chatColor);
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            // Fallback to legacy conversion if reflection fails (e.g., pre-1.16)
            return convertHexToLegacy(input);
        }
    }

    private String convertHexToLegacy(String input) {
        String regex = "(#(?:[0-9a-fA-F]{6}))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String legacyColor = hexToLegacyColor(hex);
            matcher.appendReplacement(result, legacyColor);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean isVersionAtLeast(String targetVersion) {
        try {
            String serverVersion = plugin.getServerVersion();
            String[] serverParts = serverVersion.split("\\.");
            String[] targetParts = targetVersion.split("\\.");

            int serverMajor = Integer.parseInt(serverParts[0]);
            int serverMinor = Integer.parseInt(serverParts[1]);
            int targetMajor = Integer.parseInt(targetParts[0]);
            int targetMinor = Integer.parseInt(targetParts[1]);

            return serverMajor > targetMajor || (serverMajor == targetMajor && serverMinor >= targetMinor);
        } catch (Exception e) {
            return false; // Default to no hex support if version parsing fails
        }
    }

    private String hexToLegacyColor(String hex) {
        // Parse hex to RGB
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);

        // Map to closest legacy color based on Euclidean distance in RGB space
        LegacyColor closest = null;
        double minDistance = Double.MAX_VALUE;

        for (LegacyColor color : LegacyColor.values()) {
            double distance = Math.sqrt(
                Math.pow(r - color.r, 2) +
                Math.pow(g - color.g, 2) +
                Math.pow(b - color.b, 2)
            );
            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }

        return closest != null ? closest.code : "&f"; // Default to white if something goes wrong
    }

    // Enum for legacy colors with RGB values
    private enum LegacyColor {
        BLACK("&0", 0, 0, 0),
        DARK_BLUE("&1", 0, 0, 170),
        DARK_GREEN("&2", 0, 170, 0),
        DARK_AQUA("&3", 0, 170, 170),
        DARK_RED("&4", 170, 0, 0),
        DARK_PURPLE("&5", 170, 0, 170),
        GOLD("&6", 255, 170, 0),
        GRAY("&7", 170, 170, 170),
        DARK_GRAY("&8", 85, 85, 85),
        BLUE("&9", 85, 85, 255),
        GREEN("&a", 85, 255, 85),
        AQUA("&b", 85, 255, 255),
        RED("&c", 255, 85, 85),
        LIGHT_PURPLE("&d", 255, 85, 255),
        YELLOW("&e", 255, 255, 85),
        WHITE("&f", 255, 255, 255);

        private final String code;
        private final int r, g, b;

        LegacyColor(String code, int r, int g, int b) {
            this.code = code;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}