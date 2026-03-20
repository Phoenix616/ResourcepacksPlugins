package de.themoep.resourcepacksplugin.listeners;

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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class PaperConnectionListener implements Listener {
    private final WorldResourcepacks plugin;

    private final Set<UUID> appliedInConfigPhase = ConcurrentHashMap.newKeySet();
    private final Multimap<UUID, UUID> alreadyAppliedPacks = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    private final Table<UUID, UUID, CompletableFuture<Boolean>> playersLoadingPacks = Tables.synchronizedTable(HashBasedTable.create());

    public PaperConnectionListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInitialConfigure(PlayerConnectionInitialConfigureEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }

        plugin.getUserManager().clearUserData(event.getConnection().getProfile().getId(), !event.getConnection().isTransferred());
    }

    public void onConfigure(ResourcepacksPlayer player, String worldName) {
        CompletableFuture<Boolean> lockFuture = new CompletableFuture<>();

        plugin.getUserManager().retrieveUserPacks(player.getUniqueId()).thenAccept(isTransfer -> {
            long sendDelay = -1;
            if (worldName != null) {
                sendDelay = plugin.getPackManager().getAssignment(worldName).getSendDelay();
            }
            if (sendDelay < 0) {
                sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
            }

            if (sendDelay <= 0) {
                Set<ResourcePack> packs = plugin.getPackManager().applyPack(player, worldName);
                if (!packs.isEmpty()) {
                    CompletableFuture<Boolean> packsFuture = CompletableFuture.completedFuture(true);
                    for (ResourcePack pack : packs) {
                        if (hasPack(player, pack)) {
                            plugin.logDebug("Player " + player.getName() + " already has the pack " + pack.getUuid() + " applied");
                        } else {
                            CompletableFuture<Boolean> future = new CompletableFuture<>();
                            future.whenComplete((success, throwable) -> {
                                if (success) {
                                    plugin.logDebug("Successfully sent pack " + pack.getUuid() + " to " + player.getName());
                                } else {
                                    plugin.logDebug("Failed to send pack " + pack.getUuid() + " to " + player.getName());
                                }
                            });
                            playersLoadingPacks.put(player.getUniqueId(), pack.getUuid(), future);
                            packsFuture = packsFuture.thenCombine(future, (a, b) -> a && b);
                        }
                    }
                    packsFuture.thenAccept(success -> {
                        alreadyAppliedPacks.removeAll(player.getUniqueId());
                        appliedInConfigPhase.add(player.getUniqueId());
                        if (success) {
                            plugin.logDebug("Allowing Configuration phase to continue for " + player.getName());
                        } else {
                            plugin.logDebug("Allowing Configuration phase even through we failed to send all packs to " + player.getName());
                        }
                        lockFuture.complete(true);
                    });
                    return;
                }
            }
            lockFuture.complete(true);
        });

        try {
            lockFuture.get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.getLogger().log(Level.WARNING, player.getName() + " failed to finish loading packs to in 120s during the configuration phase! " + e.getMessage());
            plugin.logDebug("Detailed error for " + player.getName(), e);
        }
    }

    private boolean hasPack(ResourcepacksPlayer player, ResourcePack pack) {
        if (alreadyAppliedPacks.containsEntry(player.getUniqueId(), pack.getUuid())) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPackStatusFirst(PlayerResourcePackStatusEvent event) {
        if (!isIntermediate(event.getStatus())) {
            alreadyAppliedPacks.put(event.getPlayer().getUniqueId(), event.getID());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatusLast(PlayerResourcePackStatusEvent event) {
        if (!isIntermediate(event.getStatus())) {
            CompletableFuture<Boolean> future = playersLoadingPacks.remove(event.getPlayer().getUniqueId(), event.getID());
            if (future != null) {
                future.complete(event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);
            }
        }
    }

    private boolean isIntermediate(PlayerResourcePackStatusEvent.Status status) {
        return status == PlayerResourcePackStatusEvent.Status.ACCEPTED || status == PlayerResourcePackStatusEvent.Status.DOWNLOADED;
    }

}
