package de.themoep.resourcepacksplugin.velocity.listeners;

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

import com.nickuc.login.api.event.velocity.auth.AuthenticateEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NLoginListener {

    private final VelocityResourcepacks plugin;

    public NLoginListener(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onAuth(AuthenticateEvent event) {
        if (!plugin.isEnabled())
            return;

        UUID playerId = event.getPlayer().getUniqueId();
        plugin.setAuthenticated(playerId, true);
        if (!plugin.hasBackend(playerId) && plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
            Player player = event.getPlayer();
            String serverName = "";
            if (player.getCurrentServer().isPresent()) {
                serverName = player.getCurrentServer().get().getServerInfo().getName();
            }
            long sendDelay = plugin.getPackManager().getAssignment(serverName).getSendDelay();
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }
            if (sendDelay > 0) {
                String finalServerName = serverName;
                plugin.getProxy().getScheduler().buildTask(plugin, () -> plugin.getPackManager().applyPack(playerId, finalServerName)).delay(sendDelay * 20, TimeUnit.MILLISECONDS);
            } else {
                plugin.getPackManager().applyPack(playerId, serverName);
            }
            plugin.getPackManager().applyPack(playerId, serverName);
        }
    }
}
