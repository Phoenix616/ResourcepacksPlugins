package de.themoep.resourcepacksplugin.sponge.listeners;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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

import de.themoep.resourcepacksplugin.sponge.SpongeResourcepacks;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.UUID;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class WorldSwitchListener {

    private final SpongeResourcepacks plugin;

    public WorldSwitchListener(SpongeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onWorldSwitch(MoveEntityEvent.Teleport event, @First Player player) {
        handleEvent(player);
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        handleEvent(event.getTargetEntity());
    }

    private void handleEvent(Player player) {
        final UUID playerId = player.getUniqueId();

        long sendDelay = plugin.getPackManager().getAssignment(player.getWorld().getName()).getSendDelay();
        if (sendDelay < 0) {
            sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
        }

        if (sendDelay > 0) {
            Sponge.getScheduler().createTaskBuilder().delayTicks(sendDelay).execute(() -> calculatePack(playerId)).submit(plugin);
        } else {
            calculatePack(playerId);
        }
    }

    private void calculatePack(UUID playerId) {
        if (plugin.isEnabled()) {
            if (!plugin.isAuthenticated(playerId)) {
                plugin.logDebug("Player " + playerId + " is not authenticated, not attempting to send a pack yet.");
                return;
            }
            Sponge.getServer().getPlayer(playerId)
                    .ifPresent(p -> plugin.getPackManager().applyPack(plugin.getPlayer(p), p.getWorld().getName()));
        }
    }
}
