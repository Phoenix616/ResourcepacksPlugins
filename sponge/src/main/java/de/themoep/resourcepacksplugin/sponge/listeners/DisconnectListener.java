package de.themoep.resourcepacksplugin.sponge.listeners;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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

import de.themoep.resourcepacksplugin.sponge.SpongeResourcepacks;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class DisconnectListener {

    private SpongeResourcepacks plugin;

    public DisconnectListener(SpongeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        if (plugin.isEnabled()) {
            plugin.getUserManager().onDisconnect(event.getTargetEntity().getUniqueId());
        }
    }
}
