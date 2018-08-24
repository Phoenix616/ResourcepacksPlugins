package de.themoep.resourcepacksplugin.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Phoenix616 on 04.11.2016.
 */
public class UserManager {
    private final ResourcepacksPlugin plugin;

    /**
     * playerid -> packname
     */
    private final Map<UUID, String> userPackMap = new ConcurrentHashMap<>();
    
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
     */
    public ResourcePack getUserPack(UUID playerid) {
        String name = userPackMap.get(playerid);
        return (name == null) ? null : plugin.getPackManager().getByName(name);
    }

    /**
     * Set the resourcepack of a user
     * @param playerid The UUID of this player
     * @param pack The resourcepack of the user
     * @return The resourcepack the player had selected previous, null if he had none before
     */
    public ResourcePack setUserPack(UUID playerid, ResourcePack pack) {
        String previous = userPackMap.put(playerid, pack.getName());
        return (previous == null) ? null : plugin.getPackManager().getByName(previous);
    }

    /**
     * Clear the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player had selected previous, null if he had none before
     */
    public ResourcePack clearUserPack(UUID playerid) {
        String previous = userPackMap.remove(playerid);
        return (previous == null) ? null : plugin.getPackManager().getByName(previous);
    }
    
    /**
     * What should happen when a player disconnects?
     * @param playerId The UUID of the player
     */
    public void onDisconnect(UUID playerId) {
        if (checkStoredPack(playerId)) {
            plugin.getLogger().log(plugin.getLogLevel(), "Removed stored pack from " + playerId + " as he logged out in under " + plugin.getPermanentPackRemoveTime() + " seconds after it got applied!");
        }
        userPackTime.remove(playerId);
        plugin.clearPack(playerId); //call plugin method because that might send a clear info
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
        
        String currentPack = userPackMap.get(playerId);
        if (!storedPackName.equalsIgnoreCase(currentPack)) {
            return false;
        }
        
        plugin.setStoredPack(playerId, null);
        return true;
    }
}
