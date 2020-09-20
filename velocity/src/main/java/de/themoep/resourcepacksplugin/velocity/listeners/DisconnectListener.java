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
import com.velocitypowered.api.event.connection.DisconnectEvent;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class DisconnectListener {

    private VelocityResourcepacks plugin;

    public DisconnectListener(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        if (plugin.isEnabled()) {
            plugin.getUserManager().onDisconnect(event.getPlayer().getUniqueId());
            plugin.setAuthenticated(event.getPlayer().getUniqueId(), false);
        }
    }
}
