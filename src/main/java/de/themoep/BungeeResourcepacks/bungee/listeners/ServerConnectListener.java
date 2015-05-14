package de.themoep.BungeeResourcepacks.bungee.listeners;

import de.themoep.BungeeResourcepacks.bungee.BungeeResourcepacks;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ServerConnectListener implements Listener {
    
    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        BungeeResourcepacks plugin = BungeeResourcepacks.getInstance();
        if(!plugin.enabled) return;
        
        ResourcePack pack = plugin.getPackManager().getServerPack(event.getTarget().getName());
        if(pack == null) {
            pack = plugin.getPackManager().getGlobalPack();
        }
        if(pack == null && plugin.getPackManager().getUserPack(event.getPlayer().getUniqueId()) != null) {
            pack = plugin.getPackManager().getEmptyPack();
        }
        if(pack != null) {
            ResourcePack prev = plugin.getPackManager().getUserPack(event.getPlayer().getUniqueId());
            if(!pack.equals(prev)) {
                plugin.setPack(event.getPlayer(), pack);
            }
        }
    }
}
