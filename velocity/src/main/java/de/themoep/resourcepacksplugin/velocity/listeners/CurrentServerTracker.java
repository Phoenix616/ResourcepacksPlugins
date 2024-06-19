package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - VelocityResourcepacks
 * Copyright (C) 2024 Max Lee aka Phoenix616 (mail@moep.tv)
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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentServerTracker {

    private final ResourcepacksPlugin plugin;

    private final Map<UUID, String> currentServer = new ConcurrentHashMap<>();

    public CurrentServerTracker(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnect(ServerPreConnectEvent event) {
        if (plugin.isEnabled() && event.getResult().getServer().isPresent()) {
            plugin.logDebug("Player " + event.getPlayer().getUsername() + " is connecting to server " + event.getResult().getServer().get().getServerInfo().getName());
            currentServer.put(event.getPlayer().getUniqueId(), event.getResult().getServer().get().getServerInfo().getName());
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        currentServer.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        if (plugin.isEnabled()) {
            currentServer.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Get the server the player is currently on or connecting to
     * @param player The player
     * @return The name of the server
     */
    public String getCurrentServer(Player player) {
        if (player.getCurrentServer().isPresent()) {
            return player.getCurrentServer().get().getServerInfo().getName();
        }
        return currentServer.get(player.getUniqueId());
    }
}
