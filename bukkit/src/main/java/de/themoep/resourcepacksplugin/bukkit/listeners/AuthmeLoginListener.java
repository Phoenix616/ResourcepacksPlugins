package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
 * Copyright (C) 2018 Max Lee aka Phoenix616 (mail@moep.tv)
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
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class AuthmeLoginListener extends AbstractAuthListener implements Listener {

    public AuthmeLoginListener(WorldResourcepacks plugin) {
        super(plugin);
    }

    /**
     * Send a plugin message to the Bungee when a player logged into AuthMe
     * @param event AuthMe's LoginEvent
     */
    @EventHandler
    public void onAuthMeLogin(LoginEvent event) {
        onAuth(event.getPlayer(), true);
    }

    /**
     * Send a plugin message to the Bungee when a player registered with AuthMe.
     * This just auto-logins the player so treat it the same way.
     * For some reason that doesn't call the login event...
     * @param event AuthMe's RegisterEvent
     */
    @EventHandler
    public void onAuthMeLogin(RegisterEvent event) {
        onAuth(event.getPlayer(), true);
    }
}
