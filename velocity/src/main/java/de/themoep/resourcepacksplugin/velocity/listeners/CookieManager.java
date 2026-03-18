package de.themoep.resourcepacksplugin.velocity.listeners;

/*
 * ResourcepacksPlugins - VelocityResourcepacks
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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;
import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CookieManager {
    private final VelocityResourcepacks plugin;

    private final Map<UUID, Multimap<Key, CompletableFuture<byte[]>>> cookieRequests = new HashMap<>();

    public CookieManager(VelocityResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onCookieReceive(CookieReceiveEvent event) {
        Multimap<Key, CompletableFuture<byte[]>> requests = cookieRequests.get(event.getPlayer().getUniqueId());
        if (requests != null) {
            for (CompletableFuture<byte[]> completableFuture : requests.removeAll(event.getResult().getKey())) {
                completableFuture.complete(event.getResult().getData());
            }
        }
    }

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        cookieRequests.remove(event.getPlayer().getUniqueId());
    }

    public CompletableFuture<byte[]> retrieveCookie(UUID playerId, String stringKey) {
        if (!plugin.supportsCookies(playerId)) {
            return CompletableFuture.completedFuture(new byte[0]);
        }

        return plugin.getProxy().getPlayer(playerId).map(player -> {
            Multimap<Key, CompletableFuture<byte[]>> requests = cookieRequests.computeIfAbsent(playerId, id -> MultimapBuilder.hashKeys().arrayListValues().build());
            Key key = Key.key(stringKey);
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            requests.put(key, future);
            player.requestCookie(key);
            return future;
        }).orElse(CompletableFuture.completedFuture(new byte[0]));
    }
}
