package de.themoep.resourcepacksplugin.bukkit.listeners;

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
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

    private final WorldResourcepacks plugin;

    public WorldSwitchListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        handleEvent(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleEvent(event.getPlayer());
    }

    private void handleEvent(Player player) {
        final UUID playerId = player.getUniqueId();

        long sendDelay = -1;
        if (player.getWorld() != null) {
            sendDelay = plugin.getPackManager().getAssignment(player.getWorld().getName()).getSendDelay();
        }
        if (sendDelay < 0) {
            sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
        }

        if (sendDelay > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> calculatePack(playerId), sendDelay);
        } else {
            calculatePack(playerId);
        }
    }

    private void calculatePack(UUID playerId) {
        if(plugin.isEnabled() && plugin.isAuthenticated(playerId)) {
            Player player = plugin.getServer().getPlayer(playerId);
            if(player != null) {
                String worldName = "";
                if(player.getWorld() != null) {
                    worldName = player.getWorld().getName();
                }
                plugin.getPackManager().applyPack(player.getUniqueId(), worldName);
            }
        }
    }
}