package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
 * Copyright (C) 2021 Max Lee aka Phoenix616 (max@themoep.de)
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

import com.nickuc.openlogin.bukkit.api.events.AsyncAuthenticateEvent;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class OpeNLoginListener extends AbstractAuthListener implements Listener {

    public OpeNLoginListener(WorldResourcepacks plugin) {
        super(plugin);
    }

    /**
     * Send a plugin message to the Bungee when a player logged into OpeNLogin on subchannel authMeLogin with the name and UUID
     * @param event OpeNLogin's auth event
     */
    @EventHandler
    public void onOpeNLoginAuth(AsyncAuthenticateEvent event) {
        onAuth(event.getPlayer(), true);
    }
}
