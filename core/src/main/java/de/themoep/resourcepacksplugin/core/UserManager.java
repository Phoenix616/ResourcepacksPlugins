package de.themoep.resourcepacksplugin.core;

/*
 * ResourcepacksPlugins - core
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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 04.11.2016.
 */
public class UserManager {
    private final ResourcepacksPlugin plugin;

    /**
     * playerid -> packname
     */
    private final Multimap<UUID, String> userPacksMap = MultimapBuilder.hashKeys().linkedHashSetValues().build();

    /**
     * What cookies a user replied to already
     */
    private final Multimap<UUID, String> cookieReplies = MultimapBuilder.hashKeys().hashSetValues().build();

    /**
     * The packs that were manually selected by the user
     */
    private final Map<UUID, String> selectedPacks = new HashMap<>();
    
    /**
     * playerid -> logintime
     */
    private Map<UUID, Long> userPackTime = new ConcurrentHashMap<>();
    
    /**
     * Manage user packs and settings
     * @param plugin The plugin instance
     */
    public UserManager(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player has selected, null if he has none/isn't known
     * @deprecated Use {@link #getUserPacks(UUID)}
     */
    @Deprecated
    public ResourcePack getUserPack(UUID playerid) {
        return getUserPacks(playerid).stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the resourcepacks of a user
     * @param playerid The UUID of this player
     * @return The resourcepack sthe player has selected, an empty list if there is none
     */
    public List<ResourcePack> getUserPacks(UUID playerid) {
        return userPacksMap.get(playerid).stream()
                .map(plugin.getPackManager()::getByName)
                .collect(Collectors.toList());
    }

    /**
     * Set the resourcepack of a user
     * @param playerid The UUID of this player
     * @param pack The resourcepack of the user
     * @return null for legacy reasons
     * @deprecated Use {@link #addUserPack(UUID, ResourcePack)}
     */
    @Deprecated
    public ResourcePack setUserPack(UUID playerid, ResourcePack pack) {
        clearUserPacks(playerid);
        addUserPack(playerid, pack);
        return null;
    }

    /**
     * Set the resourcepack of a user
     * @param playerId The UUID of this player
     * @param pack     The resourcepack of the user
     * @return Whether the user already had that pack before
     */
    public boolean addUserPack(UUID playerId, ResourcePack pack) {
        if (userPacksMap.put(playerId, pack.getName())) {
            storePacksInCookie(playerId);
            return true;
        }
        return false;
    }

    /**
     * Get the map of user IDs to pack names
     * @return The pack map
     */
    public Multimap<UUID, String> getUserPacks() {
        return userPacksMap;
    }

    /**
     * Clear the resourcepack of a user
     * @param playerid The UUID of this player
     * @return Always null for legacy reasons
     * @deprecated Use {@link #clearUserPacks(UUID)}
     */
    @Deprecated
    public ResourcePack clearUserPack(UUID playerid) {
        clearUserPacks(playerid);
        return null;
    }

    /**
     * Clear the resourcepacks of a user
     * @param playerId The UUID of this player
     * @return The list of resourcepacks the player had selected previous, an empty list if he had none before
     */
    public Collection<String> clearUserPacks(UUID playerId) {
        return clearUserPacks(playerId, true);
    }

    /**
     * Clear the resourcepacks of a user
     * @param playerId The UUID of this player
     * @param store Whether to store the pack list in the cookie
     * @return The list of resourcepacks the player had selected previous, an empty list if he had none before
     */
    public Collection<String> clearUserPacks(UUID playerId, boolean store) {
        selectedPacks.remove(playerId);
        if (store) storePacksInCookie(playerId);
        return userPacksMap.removeAll(playerId);
    }

    /**
     * Remove a specific pack from a user
     * @param playerId The UUID of the player
     * @param pack The pack to remove
     */
    public void removeUserPack(UUID playerId, ResourcePack pack) {
        if (pack == null) {
            clearUserPacks(playerId);
        } else {
            removeUserPack(playerId, pack.getName());
        }
    }

    /**
     * Remove a specific pack from a user
     * @param playerId The UUID of the player
     * @param packName The name of the pack
     */
    public void removeUserPack(UUID playerId, String packName) {
        userPacksMap.remove(playerId, packName);
        if (packName.equals(selectedPacks.get(playerId))) {
            selectedPacks.remove(playerId);
        }
        storePacksInCookie(playerId);
    }

    /**
     * Remove all stored user pack data
     */
    public void clearUserPacks() {
        userPacksMap.clear();
        userPackTime.clear();
        selectedPacks.clear();
    }

    /**
     * Get the pack a user has selected
     * @param playerId  The UUID of this player
     * @return The selected pack or <code>null</code> if none was manually selected
     */
    public ResourcePack getSelectedPack(UUID playerId) {
        return plugin.getPackManager().getByName(selectedPacks.get(playerId));
    }

    /**
     * Set the pack a user has selected
     * @param playerId  The UUID of this player
     * @param pack      The pack
     */
    public void setSelectedPack(UUID playerId, ResourcePack pack) {
        selectedPacks.put(playerId, pack.getName());
    }
    
    /**
     * What should happen when a player connects?
     * @param playerId The UUID of the player
     */
    public void clearUserData(UUID playerId) {
        clearUserData(playerId, true);
    }


    /**
     * What should happen when a player connects?
     * @param playerId The UUID of the player
     * @param store Whether to store the pack list in the cookie
     */
    public void clearUserData(UUID playerId, boolean store) {
        userPackTime.remove(playerId);
        clearUserPacks(playerId, store);
    }

    /**
     * What should happen when a player disconnects?
     * @param playerId The UUID of the player
     */
    public void onDisconnect(UUID playerId) {
        if (checkStoredPack(playerId)) {
            plugin.log(plugin.getLogLevel(), "Removed stored pack from " + playerId + " as he logged out in under " + plugin.getPermanentPackRemoveTime() + " seconds after it got applied!");
        }
        cookieReplies.removeAll(playerId);
        userPackTime.remove(playerId);
        clearUserPacks(playerId);
        plugin.sendPackInfo(playerId);
    }
    
    /**
     * Update the time that the player got his pack
     * @param playerId The UUID of the player
     */
    public void updatePackTime(UUID playerId) {
        userPackTime.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Check whether or not we should remove a permanent pack
     * @param playerId The UUID of the player
     * @return Whether or not the pack was removed
     */
    private boolean checkStoredPack(UUID playerId) {
        int packRemoveTime = plugin.getPermanentPackRemoveTime();
        if (packRemoveTime <= 0) {
            return false;
        }
        
        Long packSet = userPackTime.get(playerId);
        if (packSet == null) {
            return false;
        }
        
        if (packSet + packRemoveTime * 1000 < System.currentTimeMillis()) {
            return false;
        }
        
        String storedPackName = plugin.getStoredPack(playerId);
        if (storedPackName == null) {
            return false;
        }
        
        List<ResourcePack> currentPack = getUserPacks(playerId);
        if (currentPack.stream().noneMatch(p -> p.getName().equalsIgnoreCase(storedPackName))) {
            return false;
        }
        
        plugin.setStoredPack(playerId, null);
        return true;
    }

    /**
     * Store the player's packs in the client cookies
     * @param playerId The UUID of the player
     */
    public void storePacksInCookie(UUID playerId) {
        if (!plugin.supportsCookies(playerId)) {
            return;
        }
        List<ResourcePack> packs = getUserPacks(playerId);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(packs.size());
        for (ResourcePack pack : packs) {
            output.writeUTF(pack.getName());
        }
        plugin.storeCookie(playerId, ResourcepacksPlugin.USERPACKS_KEY, output.toByteArray());
    }

    /**
     * Retreive the stored packs from the client cookies
     * @param playerId The UUID of the player
     * @return A future completed when the packs got retrieved holding whether there was information stored
     */
    public CompletableFuture<Boolean> retrieveUserPacks(UUID playerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        plugin.retrieveCookie(playerId, ResourcepacksPlugin.USERPACKS_KEY).thenAccept(data -> {
            cookieReplies.put(playerId, ResourcepacksPlugin.USERPACKS_KEY);
            if (data == null) {
                future.complete(false);
                return;
            }
            ByteArrayDataInput input = ByteStreams.newDataInput(data);
            int packCount = input.readInt();
            for (int i = 0; i < packCount; i++) {
                String packName = input.readUTF();
                ResourcePack pack = plugin.getPackManager().getByName(packName);
                if (pack != null) {
                    addUserPack(playerId, pack);
                    plugin.logDebug("Player " + playerId + " had a pack '" + pack.getName() + "' stored in their cookies");
                } else {
                    plugin.logDebug("Player " + playerId + " had a pack with name '" + packName + "' stored in their cookies which does not exist on this server?");
                }
            }
            future.complete(packCount > -1);
        });
        return future;
    }

    /**
     * Check whether a player has replied to a cookie request
     * @param playerId The UUID of the player
     * @param key The cookie key to check
     * @return Whether the player has replied to a cookie request with that key before
     */
    public boolean hasRepliedToCookieRequest(UUID playerId, String key) {
        return !plugin.supportsCookies(playerId) || cookieReplies.containsEntry(playerId, key);
    }
}
