package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
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

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import org.bukkit.entity.Player;

public class AuthHandler {
    private final WorldResourcepacks plugin;

    public AuthHandler(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    public void onAuth(Player player, boolean sendToProxy) {
        if (!plugin.isEnabled() || player == null)
            return;

        if (plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
            long sendDelay = plugin.getPackManager().getAssignment(player.getWorld().getName()).getSendDelay();
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }
            plugin.logDebug(player.getName() + " authenticated! Sending pack in " + sendDelay + " ticks...");
            if (sendDelay > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getPackManager().applyPack(player.getUniqueId(), player.getWorld().getName()), sendDelay);
            } else {
                plugin.getPackManager().applyPack(player.getUniqueId(), player.getWorld().getName());
            }
        }

        if (sendToProxy) {
            plugin.getMessageChannelHandler().sendMessage(player, "authLogin", out -> {
                out.writeUTF(player.getName());
                out.writeUTF(player.getUniqueId().toString());
            });
        }
    }

}
