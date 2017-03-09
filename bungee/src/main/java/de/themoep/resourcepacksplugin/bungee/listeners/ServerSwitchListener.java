package de.themoep.resourcepacksplugin.bungee.listeners;

import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class ServerSwitchListener implements Listener {

    private final BungeeResourcepacks plugin;

    public ServerSwitchListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        if(plugin.isEnabled()) {
            final UUID playerId = event.getPlayer().getUniqueId();
            plugin.unsetBackend(playerId);

            ResourcePack pack = plugin.getUserManager().getUserPack(playerId);
            plugin.sendPackInfo(event.getPlayer(), pack);

            long sendDelay = -1;
            if (event.getPlayer().getServer() != null) {
                sendDelay = plugin.getPackManager().getAssignment(event.getPlayer().getServer().getInfo().getName()).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay > 0) {
                plugin.getProxy().getScheduler().schedule(plugin, () -> calculatePack(playerId), sendDelay * 50, TimeUnit.MILLISECONDS);
            } else {
                calculatePack(playerId);
            }
        }
    }

    private void calculatePack(UUID playerId) {
        if(!plugin.hasBackend(playerId) && plugin.isAuthenticated(playerId)) {
            ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
            if(player != null) {
                String serverName = "";
                if(player.getServer() != null) {
                    serverName = player.getServer().getInfo().getName();
                }
                plugin.getPackManager().applyPack(playerId, serverName);
            }
        }
    }
}
