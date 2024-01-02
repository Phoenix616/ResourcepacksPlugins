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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.util.UUID;

public class PluginMessageListener extends SubChannelHandler<ServerConnection> {
    private final VelocityResourcepacks plugin;
    private final AuthHandler authHandler;

    public PluginMessageListener(VelocityResourcepacks plugin) {
        super(plugin);
        this.plugin = plugin;
        authHandler = new AuthHandler(plugin);
        registerSubChannel("authMeLogin", (s, in) -> {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());
            if (!plugin.isAuthenticated(playerId)) {
                plugin.getProxy().getPlayer(playerId).ifPresent(authHandler::onAuth);
            }
        });
    }

    @Subscribe
    public void pluginMessageReceived(PluginMessageEvent event) {
        if (!plugin.isEnabled() || !event.getIdentifier().getId().equals("rp:plugin") || !(event.getSource() instanceof ServerConnection))
            return;

        handleMessage((ServerConnection) event.getSource(), event.getData());
    }
}
