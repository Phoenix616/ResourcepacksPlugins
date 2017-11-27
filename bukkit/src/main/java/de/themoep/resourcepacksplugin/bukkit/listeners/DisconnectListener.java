package de.themoep.resourcepacksplugin.bukkit.listeners;

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class DisconnectListener implements Listener {

    private WorldResourcepacks plugin;

    public DisconnectListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        if(plugin.isEnabled()) {
            plugin.getUserManager().onDisconnect(event.getPlayer().getUniqueId());
        }
    }
}
