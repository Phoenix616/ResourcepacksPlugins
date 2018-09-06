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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class WorldSwitchListener implements Listener {

    private final WorldResourcepacks plugin;

    public WorldSwitchListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        handleEvent(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleEvent(event.getPlayer());
    }

    private void handleEvent(Player player) {
        final UUID playerId = player.getUniqueId();

        long sendDelay = -1;
        if (player.getWorld() != null) {
            sendDelay = plugin.getPackManager().getAssignment(player.getWorld().getName()).getSendDelay();
        }
        if (sendDelay < 0) {
            sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
        }

        if (sendDelay > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> calculatePack(playerId), sendDelay);
        } else {
            calculatePack(playerId);
        }
    }

    private void calculatePack(UUID playerId) {
        if(plugin.isEnabled() && plugin.isAuthenticated(playerId)) {
            Player player = plugin.getServer().getPlayer(playerId);
            if(player != null) {
                String worldName = "";
                if(player.getWorld() != null) {
                    worldName = player.getWorld().getName();
                }
                plugin.getPackManager().applyPack(player.getUniqueId(), worldName);
            }
        }
    }
}