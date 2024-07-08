package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - velocity
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
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

import com.google.common.collect.*;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishConfigurationEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class ServerSwitchListener {

    private final VelocityResourcepacks plugin;

    private final Set<UUID> appliedInConfigPhase = ConcurrentHashMap.newKeySet();
    private final Multimap<UUID, UUID> alreadyAppliedPacks = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    private final Table<UUID, UUID, CompletableFuture<Boolean>> playersLoadingPacks = Tables.synchronizedTable(HashBasedTable.create());

    public ServerSwitchListener(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public EventTask onFinishConfigPhase(PlayerFinishConfigurationEvent event) {
        if (plugin.isEnabled()) {
            final UUID playerId = event.player().getUniqueId();
            plugin.unsetBackend(playerId);

            long sendDelay = -1;
            String serverName = plugin.getCurrentServerTracker().getCurrentServer(event.player());
            if (serverName != null) {
                sendDelay = plugin.getPackManager().getAssignment(serverName).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay <= 0) {
                Set<ResourcePack> packs = calculatePack(playerId);
                if (!packs.isEmpty()) {
                    CompletableFuture<Boolean> lockFuture = CompletableFuture.completedFuture(true);
                    for (ResourcePack pack : packs) {
                        if (hasPack(event.player(), pack)) {
                            plugin.logDebug("Player " + event.player().getUsername() + " already has the pack " + pack.getUuid() + " applied");
                        } else {
                            CompletableFuture<Boolean> future = new CompletableFuture<>();
                            future.whenComplete((success, throwable) -> {
                                if (success) {
                                    plugin.logDebug("Successfully sent pack " + pack.getUuid() + " to " + event.player().getUsername());
                                } else {
                                    plugin.logDebug("Failed to send pack " + pack.getUuid() + " to " + event.player().getUsername());
                                }
                            });
                            playersLoadingPacks.put(playerId, pack.getUuid(), future);
                            lockFuture = lockFuture.thenCombine(future, (a, b) -> a && b);
                        }
                    }
                    String playerName = event.player().getUsername();
                    return EventTask.resumeWhenComplete(lockFuture.thenAccept(success -> {
                        alreadyAppliedPacks.removeAll(playerId);
                        appliedInConfigPhase.add(playerId);
                        if (success) {
                            plugin.logDebug("Allowing Configuration phase to continue for " + playerName);
                        } else {
                            plugin.logDebug("Allowing Configuration phase even through we failed to send all packs to " + playerName);
                        }
                    }));
                }
            }
        }
        return null;
    }

    private boolean hasPack(Player player, ResourcePack pack) {
        if (alreadyAppliedPacks.containsEntry(player.getUniqueId(), pack.getUuid())) {
            return true;
        }

        for (ResourcePackInfo resourcePack : player.getAppliedResourcePacks()) {
            if (resourcePack.getId().equals(pack.getUuid()) && resourcePack.getUrl().equals(pack.getUrl())) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        appliedInConfigPhase.remove(playerId);
        alreadyAppliedPacks.removeAll(playerId);
        Map<UUID, CompletableFuture<Boolean>> futures = playersLoadingPacks.rowMap().remove(playerId);
        if (futures != null) {
            for (CompletableFuture<Boolean> future : futures.values()) {
                future.complete(false);
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPackStatusFirst(PlayerResourcePackStatusEvent event) {
        if (!event.getStatus().isIntermediate() && event.getPackId() != null) {
            alreadyAppliedPacks.put(event.getPlayer().getUniqueId(), event.getPackId());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPackStatusLast(PlayerResourcePackStatusEvent event) {
        if (!event.getStatus().isIntermediate()) {
            CompletableFuture<Boolean> future = playersLoadingPacks.remove(event.getPlayer().getUniqueId(), event.getPackId());
            if (future != null) {
                future.complete(event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFUL);
            }
        }
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        if (plugin.isEnabled()) {
            final UUID playerId = event.getPlayer().getUniqueId();
            plugin.unsetBackend(playerId);

            plugin.sendPackInfo(playerId);

            long sendDelay = -1;
            String serverName = plugin.getCurrentServerTracker().getCurrentServer(event.getPlayer());
            if (serverName != null) {
                sendDelay = plugin.getPackManager().getAssignment(serverName).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay > 0) {
                plugin.getProxy().getScheduler().buildTask(plugin, () -> calculatePack(playerId)).delay(sendDelay * 50, TimeUnit.MILLISECONDS).schedule();
            } else if (!appliedInConfigPhase.contains(playerId)){
                calculatePack(playerId);
            }
            appliedInConfigPhase.remove(playerId);
        }
    }

    private Set<ResourcePack> calculatePack(UUID playerId) {
        if (plugin.hasBackend(playerId)) {
            plugin.logDebug("Player " + playerId + " has backend pack, not attempting to send a new one.");
            return Collections.emptySet();
        }
        if (!plugin.isAuthenticated(playerId)) {
            plugin.logDebug("Player " + playerId + " is not authenticated, not attempting to send a pack yet.");
            return Collections.emptySet();
        }
        Optional<Player> player = plugin.getProxy().getPlayer(playerId);
        if (player.isPresent()) {
            Player p = player.get();
            String serverName = plugin.getCurrentServerTracker().getCurrentServer(p);
            return plugin.getPackManager().applyPack(plugin.getPlayer(p), serverName);
        }
        return Collections.emptySet();
    }
}
