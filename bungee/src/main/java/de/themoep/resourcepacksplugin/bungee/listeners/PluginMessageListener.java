package de.themoep.resourcepacksplugin.bungee.listeners;

/*
 * ResourcepacksPlugins - bungee
 * Copyright (C) 2018 Max Lee aka Phoenix616 (mail@moep.tv)
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

import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PluginMessageListener extends SubChannelHandler<Server> implements Listener {
    private final BungeeResourcepacks plugin;
    private final AuthHandler authHandler;

    public PluginMessageListener(BungeeResourcepacks plugin) {
        super(plugin);
        this.plugin = plugin;
        authHandler = new AuthHandler(plugin);
        registerSubChannel("authMeLogin", (s, in) -> {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());
            ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
            if (player != null && !plugin.isAuthenticated(playerId)) {
                authHandler.onAuth(player);
            }
        });
    }

    @EventHandler
    public void pluginMessageReceived(PluginMessageEvent event) {
        if (!plugin.isEnabled() || !event.getTag().equals("rp:plugin") || !(event.getSender() instanceof Server))
            return;

        handleMessage((Server) event.getSender(), event.getData());
    }
}
