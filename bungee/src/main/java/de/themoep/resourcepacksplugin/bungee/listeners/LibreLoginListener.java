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
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.kyngs.librelogin.api.event.EventProvider;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;

public class LibreLoginListener extends AbstractAuthListener {

    public LibreLoginListener(BungeeResourcepacks plugin, Plugin libreLogin) {
        super(plugin);
        EventProvider<ProxiedPlayer, ServerInfo> eventProvider = ((LibreLoginProvider<ProxiedPlayer, ServerInfo>) libreLogin).getLibreLogin().getEventProvider();
        eventProvider.subscribe(eventProvider.getTypes().authenticated, event -> onAuth(event.getPlayer()));
    }
}
