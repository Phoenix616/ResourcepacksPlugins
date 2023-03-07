package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
 * Copyright (C) 2023 Max Lee aka Phoenix616 (max@themoep.de)
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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.event.events.AuthenticatedEvent;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class LibreLoginListener extends AbstractAuthListener implements Listener {

    public LibreLoginListener(WorldResourcepacks plugin, LibreLoginPlugin<Player, World> libreLogin) {
        super(plugin);
        libreLogin.getEventProvider()
                .subscribe(AuthenticatedEvent.class, event -> onAuth((Player) event.getPlayer(), false));
    }
}
