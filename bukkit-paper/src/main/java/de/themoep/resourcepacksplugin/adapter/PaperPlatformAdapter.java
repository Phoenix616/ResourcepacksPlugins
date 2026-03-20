package de.themoep.resourcepacksplugin.adapter;

/*
 * ResourcepacksPlugins
 * Copyright (C) 2026 Max Lee aka Phoenix616 (mail@moep.tv)
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

import com.destroystokyo.paper.profile.PlayerProfile;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.bukkit.adapter.PlatformAdapter;
import de.themoep.resourcepacksplugin.bukkit.listeners.ConnectListener;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.listeners.PaperConnectionListener;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static de.themoep.minedown.Util.hasClass;

public class PaperPlatformAdapter implements PlatformAdapter {

    public PaperPlatformAdapter(WorldResourcepacks plugin) {
        if (hasClass("io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent")) {
            PaperConnectionListener listener = new PaperConnectionListener(plugin);
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);

            if (hasClass("io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent")) {
                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
                        listener.onConfigure(getPlayer(event.getConnection().getProfile()), event.getSpawnLocation().getWorld().getName());
                    }
                }, plugin);
            } else {
                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onSpawnLocation(AsyncPlayerConnectionConfigureEvent event) {
                        listener.onConfigure(getPlayer(event.getConnection().getProfile()), null);
                    }
                }, plugin);
            }
        } else {
            plugin.getServer().getPluginManager().registerEvents(new ConnectListener(plugin), plugin);
        }
    }

    private ResourcepacksPlayer getPlayer(PlayerProfile profile) {
        return new ResourcepacksPlayer(profile.getName(), profile.getId());
    }
}
