package de.themoep.resourcepacksplugin.core;


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
     * @param player The UUID of the player
     * @param message The message to send
     * @return <tt>true</tt> if the message was sent; <tt>false</tt> if the player was offline
     */
    boolean sendMessage(ResourcepacksPlayer player, String message);

    boolean checkPermission(ResourcepacksPlayer resourcepacksPlayer, String perm);
}
