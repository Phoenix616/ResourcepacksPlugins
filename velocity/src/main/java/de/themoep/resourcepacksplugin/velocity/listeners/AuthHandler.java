package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - velocity
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

import com.velocitypowered.api.proxy.Player;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.util.concurrent.TimeUnit;

public class AuthHandler {
    protected final VelocityResourcepacks plugin;

    public AuthHandler(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    public void onAuth(Player player) {
        if (!plugin.isEnabled() || player == null)
            return;

        plugin.setAuthenticated(player.getUniqueId(), true);
        if (!plugin.hasBackend(player.getUniqueId()) && plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
            String serverName = "";
            if (player.getCurrentServer().isPresent()) {
                serverName = player.getCurrentServer().get().getServerInfo().getName();
            }
            long sendDelay = plugin.getPackManager().getAssignment(serverName).getSendDelay();
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }
            plugin.logDebug(player.getUsername() + " authenticated on the backend server " + serverName + "! Sending pack in " + sendDelay + " ticks...");
            if (sendDelay > 0) {
                String finalServerName = serverName;
                plugin.getProxy().getScheduler()
                        .buildTask(plugin, () -> plugin.getPackManager().applyPack(player.getUniqueId(), finalServerName))
                        .delay(sendDelay * 20, TimeUnit.MILLISECONDS)
                        .schedule();
            } else {
                plugin.getPackManager().applyPack(player.getUniqueId(), serverName);
            }
        }
    }

}
