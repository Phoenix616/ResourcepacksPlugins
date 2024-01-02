package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - velocity
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

import com.velocitypowered.api.event.Subscribe;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;
import io.github._4drian3d.authmevelocity.api.velocity.event.ProxyLoginEvent;
import io.github._4drian3d.authmevelocity.api.velocity.event.ProxyRegisterEvent;

public class AuthMeVelocityListener extends AuthHandler {

    public AuthMeVelocityListener(VelocityResourcepacks plugin) {
        super(plugin);
    }

    @Subscribe
    public void onAuth(ProxyLoginEvent event) {
        onAuth(event.player());
    }

    @Subscribe
    public void onAuth(ProxyRegisterEvent event) {
        onAuth(event.player());
    }
}
