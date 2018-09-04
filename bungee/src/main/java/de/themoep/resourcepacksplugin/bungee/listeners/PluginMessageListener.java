package de.themoep.resourcepacksplugin.bungee.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class PluginMessageListener implements Listener {

    private final BungeeResourcepacks plugin;

    public PluginMessageListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void pluginMessageReceived(PluginMessageEvent event) {
        if(!plugin.isEnabled() || !event.getTag().equals("rp:plugin") || !(event.getSender() instanceof Server))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();

        switch(subchannel) {
            case "authMeLogin":
                String playerName = in.readUTF();
                UUID playerId = UUID.fromString(in.readUTF());

                plugin.setAuthenticated(playerId, true);
                if(!plugin.hasBackend(playerId) && plugin.getConfig().getBoolean("useauth", false)) {
                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
                    if(player != null) {
                        String serverName = "";
                        if(player.getServer() != null) {
                            serverName = player.getServer().getInfo().getName();
                        }
                        plugin.getPackManager().applyPack(playerId, serverName);
                    }
                }
                break;
            case "setpack":
                ProxiedPlayer player = plugin.getProxy().getPlayer(UUID.fromString(in.readUTF()));
                if (player != null) {
                    ResourcePack pack = plugin.getPackManager().getByName(in.readUTF());
                    if (pack != null) {
                        plugin.getPackManager().setPack(player.getUniqueId(), pack, false);
                    }
                }
                break;
        }
    }
}
