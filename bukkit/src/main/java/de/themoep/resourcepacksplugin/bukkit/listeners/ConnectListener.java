package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
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

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class ConnectListener implements Listener {

    private final WorldResourcepacks plugin;

    public ConnectListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerConnect(PlayerLoginEvent event) {
        if (plugin.isEnabled()) {
            plugin.getUserManager().onConnect(event.getPlayer().getUniqueId());
        }
    }
}
