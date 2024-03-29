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
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

public class NLoginListener extends AuthHandler {

    public NLoginListener(VelocityResourcepacks plugin) {
        super(plugin);
    }

    @Subscribe
    public void onAuth(AuthenticateEvent event) {
        onAuth(event.getPlayer());
    }
}
