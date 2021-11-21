package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - velocity
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class PluginMessageListener {

    private final VelocityResourcepacks plugin;

    public PluginMessageListener(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void pluginMessageReceived(PluginMessageEvent event) {
        if(!plugin.isEnabled() || !event.getIdentifier().getId().equals("rp:plugin") || !(event.getSource() instanceof ServerConnection))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        if ("authMeLogin".equals(subchannel)) {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());

            plugin.setAuthenticated(playerId, true);
            if (!plugin.hasBackend(playerId) && plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
                Optional<Player> player = plugin.getProxy().getPlayer(playerId);
                if (player.isPresent()) {
                    String serverName = "";
                    Optional<ServerConnection> server = player.get().getCurrentServer();
                    if (server.isPresent()) {
                        serverName = server.get().getServerInfo().getName();
                    }
                    plugin.getPackManager().applyPack(playerId, serverName);
                }
            }
        }
    }
}
