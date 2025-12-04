package com.sagmcu.KirosUtilities.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class JoinFullServers implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Check if the login is denied due to the server being full
        if (event.getResult() == Result.KICK_FULL) {
            // Allow players with the permission to join
            if (event.getPlayer().hasPermission("kirosutilities.joinfullservers")) {
                event.setResult(Result.ALLOWED);
            }
        }
    }
}