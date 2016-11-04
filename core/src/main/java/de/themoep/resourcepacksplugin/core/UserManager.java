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
}
