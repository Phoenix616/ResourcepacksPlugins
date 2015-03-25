package de.themoep.BungeeResourcepacks.bungee.listeners;

import de.themoep.BungeeResourcepacks.bungee.BungeeResourcepacks;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ServerSwitchListener implements Listener {
    
    @EventHandler
    public void onServerSwitch(ServerConnectEvent event) {
        ResourcePack pack = BungeeResourcepacks.getInstance().getPackManager().getServerPack(event.getTarget().getName());
        if(pack == null) {
            pack = BungeeResourcepacks.getInstance().getPackManager().getServerPack("!global");
        }
        if(pack != null) {
            ResourcePack prev = BungeeResourcepacks.getInstance().getPackManager().getUserPack(event.getPlayer().getUniqueId());
            if(prev != null && !pack.equals(prev)) {
                BungeeResourcepacks.getInstance().setPack(event.getPlayer(), pack);
            }
        }
    }
}
