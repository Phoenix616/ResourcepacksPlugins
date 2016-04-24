package de.themoep.resourcepacksplugin.bungee.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
        if(!plugin.isEnabled() || !event.getTag().equals("Resourcepack"))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        if("detectedAuthMe".equals(subchannel)) {
            plugin.useAuth(true);
        } else if("authMeLogin".equals(subchannel)) {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());

            plugin.setAuthenticated(playerId, true);
            if(!plugin.hasBackend(playerId) && plugin.getConfig().getBoolean("useauthme", true) && plugin.isAuthenticated(playerId)) {
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
}
