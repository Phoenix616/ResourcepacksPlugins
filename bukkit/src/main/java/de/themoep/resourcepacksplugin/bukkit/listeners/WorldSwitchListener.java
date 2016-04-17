package de.themoep.resourcepacksplugin.bukkit.listeners;

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class WorldSwitchListener implements Listener {

    WorldResourcepacks plugin;

    public WorldSwitchListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        calculatePack(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        // Send out pack after the proxy server sent us the info about the previous pack
        // Also helps to wait till the client properly loaded to display the confirmation dialog
        new BukkitRunnable() {
            @Override
            public void run() {
                calculatePack(playerId);
            }
        }.runTaskLater(plugin, 10);

    }

    private void calculatePack(UUID playerId) {
        if(plugin.isEnabled()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if(player != null) {
                String worldName = "";
                if(player.getServer() != null) {
                    worldName = player.getWorld().getName();
                }
                plugin.getPackManager().applyPack(player.getUniqueId(), worldName);
            }
        }
    }
}