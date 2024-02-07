package de.themoep.resourcepacksplugin.bungee.listeners;

/*
 * ResourcepacksPlugins - bungee
 * Copyright (C) 2024 Max Lee aka Phoenix616 (max@themoep.de)
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
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ConnectListener implements Listener {

    private final BungeeResourcepacks plugin;

    public ConnectListener(BungeeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConnect(LoginEvent event) {
        if (plugin.isEnabled() && !event.isCancelled()) {
            plugin.getUserManager().onConnect(event.getConnection().getUniqueId());
        }
    }
}
