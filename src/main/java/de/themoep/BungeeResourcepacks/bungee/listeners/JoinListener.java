package de.themoep.BungeeResourcepacks.bungee.listeners;

import de.themoep.BungeeResourcepacks.bungee.BungeeResourcepacks;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class JoinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(final PostLoginEvent event) {
        BungeeResourcepacks plugin = BungeeResourcepacks.getInstance();
        plugin.clearPack(event.getPlayer());
        final UUID playerid = event.getPlayer().getUniqueId();
        plugin.setJoining(playerid);
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                BungeeResourcepacks plugin = BungeeResourcepacks.getInstance();
                ProxiedPlayer player = plugin.getProxy().getPlayer(playerid);
                if(player != null) {
                    ResourcePack pack = null;
                    Server server = player.getServer();
                    if(server != null) {
                        pack = plugin.getPackManager().getServerPack(server.getInfo().getName());
                    }
                    if (pack == null) {
                        pack = plugin.getPackManager().getServerPack("!global");
                    }
                    if (pack != null) {
                        plugin.setPack(player, pack);
                    }
                }
                plugin.unsetJoining(playerid);
            }
        }, 300L, TimeUnit.MILLISECONDS);
    }
}
