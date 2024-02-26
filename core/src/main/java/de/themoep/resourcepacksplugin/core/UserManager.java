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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        return userPacksMap.put(playerId, pack.getName());
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
    }

    /**
     * Remove all stored user pack data
     */
    public void clearUserPacks() {
        userPacksMap.clear();
        userPackTime.clear();
    }
    
    /**
     * What should happen when a player connects?
     * @param playerId The UUID of the player
     */
    public void onConnect(UUID playerId) {
        userPackTime.remove(playerId);
        clearUserPacks(playerId);
    }

    /**
     * What should happen when a player disconnects?
     * @param playerId The UUID of the player
     */
    public void onDisconnect(UUID playerId) {
        if (checkStoredPack(playerId)) {
            plugin.log(plugin.getLogLevel(), "Removed stored pack from " + playerId + " as he logged out in under " + plugin.getPermanentPackRemoveTime() + " seconds after it got applied!");
        }
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
}
