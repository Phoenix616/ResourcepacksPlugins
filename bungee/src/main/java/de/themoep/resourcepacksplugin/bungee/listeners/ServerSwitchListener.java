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
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class ServerSwitchListener implements Listener {

    private final BungeeResourcepacks plugin;

    public ServerSwitchListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        if (plugin.isEnabled()) {
            final UUID playerId = event.getPlayer().getUniqueId();
            plugin.unsetBackend(playerId);

            plugin.sendPackInfo(playerId);

            if (plugin.getPlayerProtocol(playerId) == MinecraftVersion.MINECRAFT_1_20_2.getProtocolNumber()) {
                // In 1.20.2 the pack needs to be resent on server switch -> we remove the user pack which forces a resend
                plugin.getUserManager().clearUserPacks(playerId);
            }

            long sendDelay = -1;
            if (event.getPlayer().getServer() != null) {
                sendDelay = plugin.getPackManager().getAssignment(event.getPlayer().getServer().getInfo().getName()).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay > 0) {
                plugin.getProxy().getScheduler().schedule(plugin, () -> calculatePack(playerId), sendDelay * 50, TimeUnit.MILLISECONDS);
            } else {
                calculatePack(playerId);
            }
        }
    }

    private void calculatePack(UUID playerId) {
        if (plugin.hasBackend(playerId)) {
            plugin.logDebug("Player " + playerId + " has backend pack, not attempting to send a new one.");
            return;
        }
        if (!plugin.isAuthenticated(playerId)) {
            plugin.logDebug("Player " + playerId + " is not authenticated, not attempting to send a pack yet.");
            return;
        }
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
        if(player != null) {
            String serverName = "";
            if(player.getServer() != null) {
                serverName = player.getServer().getInfo().getName();
            }
            plugin.getPackManager().applyPack(plugin.getPlayer(player), serverName);
        }
    }
}
