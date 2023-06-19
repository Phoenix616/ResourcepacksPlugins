package de.themoep.resourcepacksplugin.bungee.listeners;

/*
 * ResourcepacksPlugins - bungee
 * Copyright (C) 2022 Max Lee aka Phoenix616 (mail@moep.tv)
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
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.concurrent.TimeUnit;

public abstract class AbstractAuthListener {
    protected final BungeeResourcepacks plugin;

    public AbstractAuthListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    protected void onAuth(ProxiedPlayer player) {
        if (!plugin.isEnabled() || player == null || plugin.isAuthenticated(player.getUniqueId()))
            return;

        plugin.setAuthenticated(player.getUniqueId(), true);
        if (!plugin.hasBackend(player.getUniqueId()) && plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
            String serverName = "";
            if (player.getServer() != null) {
                serverName = player.getServer().getInfo().getName();
            }
            long sendDelay = plugin.getPackManager().getAssignment(serverName).getSendDelay();
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }
            plugin.logDebug(player.getName() + " authenticated on the backend server " + serverName + "! Sending pack in " + sendDelay + " ticks...");
            if (sendDelay > 0) {
                String finalServerName = serverName;
                plugin.getProxy().getScheduler().schedule(plugin, () -> plugin.getPackManager().applyPack(player.getUniqueId(), finalServerName), sendDelay * 20, TimeUnit.MILLISECONDS);
            } else {
                plugin.getPackManager().applyPack(player.getUniqueId(), serverName);
            }
        }
    }

}
