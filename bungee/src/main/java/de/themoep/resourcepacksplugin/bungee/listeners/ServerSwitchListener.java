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

    BungeeResourcepacks plugin;

    public ServerSwitchListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        if(plugin.isEnabled()) {
            final UUID playerId = event.getPlayer().getUniqueId();
            plugin.unsetBackend(playerId);

            ResourcePack pack = plugin.getPackManager().getUserPack(playerId);
            plugin.sendPackInfo(event.getPlayer(), pack);

            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    calculatePack(playerId);
                }
            }, 1L, TimeUnit.SECONDS);
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
