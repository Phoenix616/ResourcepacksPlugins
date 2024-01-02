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

import com.jakub.jpremium.velocity.api.event.UserEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

public class JPremiumListener extends AuthHandler {

    public JPremiumListener(VelocityResourcepacks plugin) {
        super(plugin);
    }

    @Subscribe
    public void onAuth(UserEvent.Login event) {
        if (event.getCommandSource().isPresent() && event.getCommandSource().get() instanceof Player) {
            onAuth((Player) event.getCommandSource().get());
        } else {
            plugin.getProxy().getPlayer(event.getUserProfile().getUniqueId()).ifPresent(this::onAuth);
        }
    }
}
