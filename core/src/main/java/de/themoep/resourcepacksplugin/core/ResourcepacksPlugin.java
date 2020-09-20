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

import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import net.md_5.bungee.api.chat.BaseComponent;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public interface ResourcepacksPlugin {

    default void startupMessage() {
        getLogger().log(getLogLevel(), "");
        getLogger().log(getLogLevel(), "  If you enjoy my " + getName() + " plugin then you might also be");
        getLogger().log(getLogLevel(), "  interested in the more advanced ForceResourcepacks version!");
        getLogger().log(getLogLevel(), "");
        getLogger().log(getLogLevel(), "  Besides getting additional features like WorldGuard support and");
        getLogger().log(getLogLevel(), "  the ability to force a player to accept the resources pack you");
        getLogger().log(getLogLevel(), "  will also support the continued development of this plugin!");
        getLogger().log(getLogLevel(), "");
        getLogger().log(getLogLevel(), "  Check it out here on spigotmc.org: https://s.moep.tv/frp");
        getLogger().log(getLogLevel(), "");
        getLogger().log(getLogLevel(), "  - Phoenix616");
        getLogger().log(getLogLevel(), "");
    }

    boolean loadConfig();

    /**
     * Convert a configuration object to a map
     * @param configuration The configuration object
     * @return The configuration map or null if input was null or is not supported
     */
    Map<String, Object> getConfigMap(Object configuration);

    /**
     * Reloads the configuration from the file and resend the resource pack to all online players
     * @param resend Whether or not all players should get their applicable pack resend
     */
    void reloadConfig(boolean resend);

    /**
     * Get whether the plugin successful enabled or not
     * @return <code>true</code> if the plugin was proberly enabled
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
     * @deprecated Please use {@link PackManager#setPack(UUID, ResourcePack)}!
     */
    @Deprecated
    void setPack(UUID playerId, ResourcePack pack);

    /**
     * Internal method to send a resoucepack to a player, please use {@link PackManager#setPack(UUID, ResourcePack)}!
     * @param playerId The UUID of the player to send the pack to
     * @param pack The resourcepack to send to a player
     */
    void sendPack(UUID playerId, ResourcePack pack);

    void clearPack(UUID playerId);

    PackManager getPackManager();

    UserManager getUserManager();

    /**
     * Get a message from the config
     * @param key The message's key
     * @return The defined message string or an error message if the variable isn't known.
     * @deprecated Use {@link #getMessage(ResourcepacksPlayer, String, String...)}
     */
    @Deprecated
    default String getMessage(String key) {
        return getMessage(null, key);
    }

    /**
     * Get a message from the config and replace variables
     * @param key The message's key
     * @param replacements The replacements in a mapping variable-replacement
     * @return The defined message string or an error message if the variable isn't known.
     * @deprecated Use {@link #getMessage(ResourcepacksPlayer, String, String...)}
     */
    @Deprecated
    default String getMessage(String key, Map<String, String> replacements) {
        String[] repl = new String[replacements.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> e : replacements.entrySet()) {
            repl[i] = e.getKey();
            repl[i + 1] = e.getValue();
            i+=2;
        }
        return getMessage(null, key, repl);
    }

    /**
     * Get a message from the language config
     * @param sender The sender to get the message from, will use the client language if available
     * @param key The message key
     * @param replacements Optional placeholder replacement array
     * @return The message or an error message if not available, never null
     */
    String getMessage(ResourcepacksPlayer sender, String key, String... replacements);

    /**
     * Check whether or not the language config contains a certain message
     * @param sender The sender
     * @param key The message key
     * @return <code>true</code> if the config contains the message; <code>false</code> if not
     */
    boolean hasMessage(ResourcepacksPlayer sender, String key);

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

    File getDataFolder();

    /**
     * Log a debug message
     * @param message The message
     */
    void logDebug(String message);

    /**
     * Log a debug message with a stacktrace
     * @param message   The message
     * @param throwable The throwable
     */
    void logDebug(String message, Throwable throwable);

    /**
     * The debug log level
     * @return The debug log level
     */
    Level getLogLevel();

    /**
     * Get an online player by its UUID
     * @param playerId The player ID
     * @return The ResourcepacksPlayer
     */
    ResourcepacksPlayer getPlayer(UUID playerId);

    /**
     * Get an online player by its name
     * @param playerName The player name
     * @return The ResourcepacksPlayer
     */
    ResourcepacksPlayer getPlayer(String playerName);

    /**
     * Send a message to a player
     * @param player The player
     * @param key The key of the message to send
     * @param replacements An optional array with placeholder replacements
     * @return <code>true</code> if the message was sent; <code>false</code> if the player was offline or the message empty
     */
    boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements);

    /**
     * Send a message to a sender
     * @param sender The the sender
     * @param level The level to log to if the sender is the console!
     * @param key The key of the message to send
     * @param replacements An optional array with placeholder replacements
     * @return <code>true</code> if the message was sent; <code>false</code> if the player was offline or the message empty
     */
    boolean sendMessage(ResourcepacksPlayer sender, Level level, String key, String... replacements);

    /**
     * Log a message. This strips out all color codes
     * @param level The level at which the message should be logged
     * @param message The message to log
     */
    void log(Level level, String message);

    /**
     * Check whether or not a player has a permission
     * @param resourcepacksPlayer The player to check
     * @param perm The permission to check for
     * @return <code>true</code> if the player has the permission; <code>false</code> if not
     */
    boolean checkPermission(ResourcepacksPlayer resourcepacksPlayer, String perm);

    /**
     * Check whether or not a player has a permission
     * @param playerId The UUID of the player
     * @param perm The permission to check for
     * @return <code>true</code> if the player has the permission; <code>false</code> if not
     */
    boolean checkPermission(UUID playerId, String perm);

    /**
     * Get the protocol version of a player
     * @param playerId The UUID of the player
     * @return The protocol version or -1 if the player isn't online
     */
    int getPlayerProtocol(UUID playerId);

    /**
     * Get the format of the pack this player can maximally use
     * @param playerId The UUID of the player
     * @return The pack format or -1 if the player isn't online
     */
    default int getPlayerPackFormat(UUID playerId) {
        int protocol = getPlayerProtocol(playerId);
        if (protocol > -1) {
            return getPackManager().getPackFormat(protocol);
        }
        return -1;
    }

    /**
     * Call the ResourcePackSelectEvent on the corresponding server
     * @param playerId The UUID of the player
     * @param pack The ResourcePack that was selected or null if none was selected
     * @param status The status of the selection
     * @return The ResourcePackSelectEvent interface which might have been modified (especially the pack)
     */
    IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status);

    /**
     * Call the ResourcePackSendEvent on the corresponding server
     * @param playerId The UUID of the player
     * @param pack The ResourcePack that was send
     * @return The ResourcePackSendEvent interface which might have been modified or cancelled
     */
    IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack);

    /**
     * Check whether or not a certain player is currently logged in with auth plugins (currently supports AuthMe Reloaded)
     * @param playerId The UUID of the player
     * @return <code>true</code> if he is loggedin; <code>false</code> if not or the status is unknown
     */
    boolean isAuthenticated(UUID playerId);

    /**
     * Run a sync task
     * @param runnable What to run
     * @return The task id
     */
    int runTask(Runnable runnable);

    /**
     * Run a task asynchronously
     * @param runnable What to run
     * @return The task id
     */
    int runAsyncTask(Runnable runnable);

    /**
     * Save changes made on runtime to the config
     */
    void saveConfigChanges();

    /**
     * Set the pack that the player should get when logging in when no other pack applies
     * and that gets used instead of the empty pack on reset
     * @param playerId  UUID of the player
     * @param packName  Name of the pack
     */
    void setStoredPack(UUID playerId, String packName);

    /**
     * Get the pack that a certain player has stored
     * @param playerId  The UUID of the player
     * @return The name of the pack or <code>null</code> if none was stored
     */
    String getStoredPack(UUID playerId);

    /**
     * Get whether or not the default /usepack behaviour is to apply temporary opr permanent
     * @return <code>true</code> if it's temporary, <code>false</code> if not
     */
    boolean isUsepackTemporary();
    
    /**
     * Get the time in which the permanent pack will be reset if the player disconnects
     * @return The time in seconds; 0 or below should disable that
     */
    int getPermanentPackRemoveTime();
}
