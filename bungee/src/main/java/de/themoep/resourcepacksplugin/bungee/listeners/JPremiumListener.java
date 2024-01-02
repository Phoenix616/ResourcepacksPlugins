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

import com.jakub.premium.api.event.UserEvent;
import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class JPremiumListener extends AuthHandler implements Listener {

    public JPremiumListener(BungeeResourcepacks plugin) {
        super(plugin);
    }

    @EventHandler
    public void onAuth(UserEvent.Login event) {
        onAuth(event.getUserProfile().getProxiedPlayer());
    }
}
