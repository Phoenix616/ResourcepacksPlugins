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
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class ServerSwitchListener {

    private final VelocityResourcepacks plugin;

    public ServerSwitchListener(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        if (plugin.isEnabled()) {
            final UUID playerId = event.getPlayer().getUniqueId();
            plugin.unsetBackend(playerId);

            ResourcePack pack = plugin.getUserManager().getUserPack(playerId);
            plugin.sendPackInfo(event.getPlayer(), pack);

            long sendDelay = -1;
            Optional<ServerConnection> server = event.getPlayer().getCurrentServer();
            if (server.isPresent()) {
                sendDelay = plugin.getPackManager().getAssignment(server.get().getServerInfo().getName()).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay > 0) {
                plugin.getProxy().getScheduler().buildTask(plugin, () -> calculatePack(playerId)).delay(sendDelay * 50, TimeUnit.MILLISECONDS).schedule();
            } else {
                calculatePack(playerId);
            }
        }
    }

    private void calculatePack(UUID playerId) {
        if(!plugin.hasBackend(playerId) && plugin.isAuthenticated(playerId)) {
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
