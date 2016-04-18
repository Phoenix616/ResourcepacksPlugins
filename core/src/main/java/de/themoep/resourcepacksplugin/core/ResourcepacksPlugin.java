package de.themoep.resourcepacksplugin.core;


import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public interface ResourcepacksPlugin {

    boolean loadConfig();

    /**
     * Reloads the configuration from the file and resend the resource pack to all online players
     * @param resend Whether or not all players should get their applicable pack resend
     */
    void reloadConfig(boolean resend);

    /**
     * Get whether the plugin successful enabled or not
     * @return <tt>true</tt> if the plugin was proberly enabled
     */
    boolean isEnabled();

    /**
     * Resends the pack that corresponds to the player's server
     * @param playerId The UUID of the player to resend the pack for
     */
    void resendPack(UUID playerId);

    /**
     * Set the resoucepack of a connected player
     * @param playerId The UUID of the player to set the pack for
     * @param pack The resourcepack to set for the player
     */
    void setPack(UUID playerId, ResourcePack pack);

    void clearPack(UUID playerId);

    PackManager getPackManager();

    /**
     * Get a message from the config
     * @param key The message's key
     * @return The defined message string or an error message if the variable isn't known.
     */
    String getMessage(String key);

    /**
     * Get a message from the config and replace variables
     * @param key The message's key
     * @param replacements The replacements in a mapping variable-replacement
     * @return The defined message string or an error message if the variable isn't known.
     */
    String getMessage(String key, Map<String, String> replacements);

    /**
     * Get the name of the plugin
     * @return The plugin's name as a string
     */
    String getName();

    /**
     * Get the version of the plugin
     * @return The plugin's version as a string
     */
    String getVersion();

    Logger getLogger();

    Level getLogLevel();

    ResourcepacksPlayer getPlayer(UUID playerId);

    ResourcepacksPlayer getPlayer(String playerName);

    /**
     * Send a message to a player
     * @param player The the player
     * @param message The message to send
     * @return <tt>true</tt> if the message was sent; <tt>false</tt> if the player was offline
     */
    boolean sendMessage(ResourcepacksPlayer player, String message);

    /**
     * Check whether or not a player has a permission
     * @param resourcepacksPlayer
     * @param perm
     * @return
     */
    boolean checkPermission(ResourcepacksPlayer resourcepacksPlayer, String perm);

    /**
     * Check whether or not a player has a permission
     * @param playerId
     * @param perm
     * @return
     */
    boolean checkPermission(UUID playerId, String perm);

    /**
     * Get the format of the pack this player can maximally use
     * @param playerId The UUID of the player
     * @return The pack format, <tt>0</tt> if he can't use any, <tt>MAX_INT</tt> when he can use all
     */
    int getPlayerPackFormat(UUID playerId);

    /**
     * Call the ResourcePackSelectEvent on the corresponding server
     * @param playerId The UUID of the player
     * @param pack The ResourcePack that was selected or null if none was selected
     * @param status The status of the selection
     * @return The ResourcePackSelectEvent interface which might have been modified (especially the pack)
     */
    IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status);
}
