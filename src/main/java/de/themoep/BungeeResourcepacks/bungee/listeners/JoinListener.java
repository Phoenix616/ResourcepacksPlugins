package de.themoep.BungeeResourcepacks.bungee.listeners;

import de.themoep.BungeeResourcepacks.bungee.BungeeResourcepacks;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class JoinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ResourcePack pack = BungeeResourcepacks.getInstance().getPackManager().getServerPack(event.getPlayer().getPendingConnection().getListener().getDefaultServer());
        if(pack == null) {
            pack = BungeeResourcepacks.getInstance().getPackManager().getServerPack("!global");
        }
        if(pack != null) {
            BungeeResourcepacks.getInstance().setPack(event.getPlayer(), pack);
        }
    }
}
