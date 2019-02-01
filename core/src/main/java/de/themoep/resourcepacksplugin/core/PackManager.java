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

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Phoenix616 on 25.03.2015.
 */
public class PackManager {

    public static final String EMPTY_IDENTIFIER = "empty";

    private final ResourcepacksPlugin plugin;
    /**
     * packname -> ResourcePack
     */
    private Map<String, ResourcePack> packNames;

    /**
     * packhash -> packname 
     */
    private Map<String, ResourcePack> packHashes;
    
    /**
     * packurl -> packname 
     */
    private Map<String, ResourcePack> packUrls;

    /**
     * The empty pack, null if none is set
     */
    private ResourcePack empty = null;
    
    /**
     * Name of the global pack, null if none is set
     */
    private PackAssignment global = new PackAssignment("global");
    
    /**
     * server-/worldname -> pack assignment
     */
    private Map<String, PackAssignment> literalAssignments;

    /**
     * server-/worldname -> pack assignment
     */
    private Map<String, PackAssignment> regexAssignments;

    /**
     * Whether or not to save the config on the next modification of the manager state
     */
    private boolean dirty = false;


    public PackManager(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize this pack manager
     */
    public void init() {
        packNames = new LinkedHashMap<>();
        packHashes = new HashMap<>();
        packUrls = new HashMap<>();
        empty = null;
        global = new PackAssignment("global");
        literalAssignments = new LinkedHashMap<>();
        regexAssignments = new LinkedHashMap<>();
    }

    /**
     * Registers a new resource pack with the packmanager
     * @param pack The resourcepack to register
     * @return If a pack with that name was known before it returns the past pack, null if none was known
     * @throws IllegalArgumentException when there already is a pack with the same url or hash but not name defined
     */
    public ResourcePack addPack(ResourcePack pack) throws IllegalArgumentException {
        ResourcePack byHash = getByHash(pack.getHash());
        if (byHash != null && !byHash.getName().equalsIgnoreCase(pack.getName())) {
            throw new IllegalArgumentException("Could not add pack '" + pack.getName() + "'. There is already a pack with the hash '" + pack.getHash() + "' but a different name defined! (" + byHash.getName() + ")");
        }
        ResourcePack byUrl = getByUrl(pack.getUrl());
        if (byUrl != null && !byUrl.getName().equalsIgnoreCase(pack.getName())) {
            throw new IllegalArgumentException("Could not add pack '" + pack.getName() + "'. There is already a pack with the url '" + pack.getUrl() + "' but a different name defined! (" + byUrl.getName() + ")");
        }
        packHashes.put(pack.getHash(), pack);
        packUrls.put(pack.getUrl(), pack);
        return packNames.put(pack.getName().toLowerCase(), pack);
    }

    /**
     * Set the hash of a pack to a new value
     * @param pack The pack to update
     * @param hash The new hash to set
     * @return Whether or not the hash changed
     */
    public boolean setPackHash(ResourcePack pack, String hash) {
        if (pack.getHash().equals(hash)) {
            return false;
        }
        packHashes.remove(pack.getHash());
        pack.setHash(hash);
        packHashes.put(pack.getHash(), pack);
        return true;
    }

    /**
     * Set the url of a pack to a new value
     * @param pack The pack to update
     * @param url The new url to set
     * @return Whether or not the url changed
     */
    public boolean setPackUrl(ResourcePack pack, String url) {
        if (pack.getUrl().equals(url)) {
            return false;
        }
        packUrls.remove(pack.getUrl());
        pack.setUrl(url);
        packUrls.put(pack.getUrl(), pack);
        return true;
    }

    /**
     * Get the resourcepack by its name
     * @param name The name of the pack to get
     * @return The resourcepack with that name, null if there is none
     */
    public ResourcePack getByName(String name) {
        return name != null ? packNames.get(name.toLowerCase()) : null;
    }
    
    /**
     * Get the resourcepack by its hash
     * @param hash The hash of the pack to get
     * @return The resourcepack with that hash, null if there is none
     */
    public ResourcePack getByHash(String hash) {
        return packHashes.get(hash);
    }

    /**
     * Get the resourcepack by its hash
     * @param hash The hash of the pack to get
     * @return The resourcepack with that hash, null if there is none
     */
    public ResourcePack getByHash(byte[] hash) {
        return packHashes.get(BaseEncoding.base16().lowerCase().encode(hash));
    }

    /**
     * Get the resourcepack by its url
     * @param url The url of the pack to get
     * @return The resourcepack with that url, null if there is none
     */
    public ResourcePack getByUrl(String url) {
        return packUrls.get(url);
    }

    /**
     * Set the empty Resource Pack
     * @param pack The pack to set as empty pack
     * @return The previous empty pack, null if none was set
     */
    public ResourcePack setEmptyPack(ResourcePack pack) {
        ResourcePack rp = getEmptyPack();
        empty = pack;
        return rp;
    }

    /**
     * Set the empty Resource Pack
     * @param packname The name of the pack to set as empty pack
     * @return The previous empty pack, null if none was set
     */
    public ResourcePack setEmptyPack(String packname) {
        return setEmptyPack(getByName(packname));
    }

    /**
     * Get the empty Resource Pack
     * @return The empty pack, null if none is set
     */
    public ResourcePack getEmptyPack() {
        return empty;
    }
    
    /**
     * Set the global Resource Pack
     * @param pack The pack to set as global
     * @return The previous global pack, null if none was set
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#setPack(ResourcePack)}
     */
    @Deprecated
    public ResourcePack setGlobalPack(ResourcePack pack) {
        ResourcePack rp = getGlobalPack();
        getGlobalAssignment().setPack(pack);
        return rp;
    }

    /**
     * Set the global Resource Pack
     * @param packname The name of the pack to set as global
     * @return The previous global pack, null if none was set
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#setPack(String)}
     */
    @Deprecated
    public ResourcePack setGlobalPack(String packname) {
        return setGlobalPack(getByName(packname));
    }

    /**
     * Get the global Resource Pack
     * @return The global pack, null if none is set
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#getPack()}
     */
    @Deprecated
    public ResourcePack getGlobalPack() {
        return getByName(getGlobalAssignment().getPack());
    }

    /**
     * Add a secondary global Resource Pack
     * @param pack The pack to add to the list of secondary ones
     * @return False if the pack already was in the list; True if not
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#addSecondary(ResourcePack)}
     */
    @Deprecated
    public boolean addGlobalSecondary(ResourcePack pack) {
        return getGlobalAssignment().addSecondary(pack);
    }

    /**
     * Add a secondary global Resource Pack
     * @param packname The name of the pack to add to the list of secondary ones
     * @return False if the pack already was in the list; True if not
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#addSecondary(String)}
     */
    @Deprecated
    public boolean addGlobalSecondary(String packname) {
        return getGlobalAssignment().addSecondary(packname);
    }

    /**
     * Get if a pack is in the list of secondary global Resource Packs
     * @param pack The pack to check
     * @return True if it is a global secondary pack, false if not
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#isSecondary(ResourcePack)}
     */
    @Deprecated
    public boolean isGlobalSecondary(ResourcePack pack) {
        return getGlobalAssignment().isSecondary(pack);
    }

    /**
     * Get if a pack is in the list of secondary global Resource Packs
     * @param packname The name of the pack to check
     * @return True if it is a global secondary pack, false if not
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#isSecondary(String)}
     */
    @Deprecated
    public boolean isGlobalSecondary(String packname) {
        return getGlobalAssignment().isSecondary(packname);
    }

    /**
     * Get the list of global seconday packs
     * @return A list of packnames that are global secondary packs
     * @deprecated Use {@link PackManager#getGlobalAssignment()} and {@link PackAssignment#getSecondaries()}
     */
    @Deprecated
    public List<String> getGlobalSecondary() {
        return new ArrayList<>(global.getSecondaries());
    }
    
    /**
     * Get the resourcepack of a server
     * @param server The name of the server, "!global" for the global pack
     * @return The resourcepack of the server, null if there is none
     * @deprecated Use {@link PackManager#getAssignment(String)} ()} and {@link PackAssignment#getPack()}
     */
    @Deprecated
    public ResourcePack getServerPack(String server) {
        return getByName(getAssignment(server).getPack());
    }
    
    /**
     * Get the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player has selected, null if he has none/isn't known
     * @deprecated Use {@link UserManager#getUserPack} instead!
     */
    @Deprecated
    public ResourcePack getUserPack(UUID playerid) {
        return plugin.getUserManager().getUserPack(playerid);
    }
    
    /**
     * Set the resourcepack of a user
     * @param playerid The UUID of this player
     * @param pack The resourcepack of the user
     * @return The resourcepack the player had selected previous, null if he had none before
     * @deprecated Use {@link UserManager#setUserPack} instead!
     */
    @Deprecated
    public ResourcePack setUserPack(UUID playerid, ResourcePack pack) {
        return plugin.getUserManager().setUserPack(playerid, pack);
    }

    /**
     * Clear the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player had selected previous, null if he had none before
     * @deprecated Use {@link UserManager#clearUserPack} instead!
     */
    @Deprecated
    public ResourcePack clearUserPack(UUID playerid) {
        return plugin.getUserManager().clearUserPack(playerid);
    }
    

    /**
     * Add a server to a resourcepack
     * @param server The server this pack should be active on
     * @param pack The resourcepack
     * @deprecated Use the {@link PackManager#getAssignment(String)} and {@link PackAssignment#setPack(ResourcePack)}
     */
    @Deprecated
    public void addServer(String server, ResourcePack pack) {
        getAssignment(server).setPack(pack);
    }

    /**
     * Get the global assignment
     * @return  The global PackAssignment
     */
    public PackAssignment getGlobalAssignment() {
        return global;
    }

    /**
     * Set the global assignment
     * @param assignment    The PackAssignment that you want to set
     */
    public void setGlobalAssignment(PackAssignment assignment) {
        this.global = assignment;
    }

    /**
     * Add a new assignment to a server/world
     * @param assignment    The new PackAssignment
     * @return              The previous assignment or null if there was none
     */
    public PackAssignment addAssignment(PackAssignment assignment) {
        PackAssignment previous;
        if (assignment.getRegex() != null) {
            previous = regexAssignments.put(assignment.getName().toLowerCase(), assignment);
        } else {
            previous = literalAssignments.put(assignment.getName().toLowerCase(), assignment);
        }
        checkDirty();
        return previous;
    }

    /**
     * Get the assignment of a server/world
     * @param server    The name of the server/world
     * @return          The PackAssignment; an empty one if there is none
     */
    public PackAssignment getAssignment(String server) {
        PackAssignment assignment = literalAssignments.get(server.toLowerCase());
        if (assignment != null) {
            return assignment;
        }
        for (PackAssignment regexAssignment : regexAssignments.values()) {
            if (regexAssignment.getRegex().matcher(server).matches()) {
                return regexAssignment;
            }
        }
        return new PackAssignment("empty");
    }

    /**
     * Get an assignment by its name
     * @param name  The name of the assignment
     * @return      The PackAssignment or null if not found
     */
    public PackAssignment getAssignmentByName(String name) {
        PackAssignment assignment = literalAssignments.get(name.toLowerCase());
        if (assignment == null) {
            assignment = regexAssignments.get(name.toLowerCase());
        }
        return assignment;
    }
    
    /**
     * Get all assignments
     * @return The all PackAssignments
     */
    public Collection<? extends PackAssignment> getAssignments() {
        List<PackAssignment> assignments = new ArrayList<>(literalAssignments.values());
        assignments.addAll(regexAssignments.values());
        return assignments;
    }

    /**
     * Load an assignment from a map representing the section in the config
     * @param name      The name of the assignment
     * @param config    A map representing the config section
     * @return          The PackAssignment
     */
    public PackAssignment loadAssignment(String name, Map<String, Object> config) {
        PackAssignment assignment = new PackAssignment(name);
        if (config.get("regex") != null) {
            if (!(config.get("regex") instanceof String)) {
                plugin.getLogger().log(Level.WARNING, "'regex' option has to be a String!");
            } else {
                try {
                    assignment.setRegex(Pattern.compile(((String) config.get("regex"))));
                    plugin.getLogger().log(plugin.getLogLevel(), "Regex: " + assignment.getRegex().toString());
                } catch (PatternSyntaxException e) {
                    plugin.getLogger().log(Level.WARNING, "The assignment's regex '" + config.get("regex") + "' isn't valid! Using the key name literally! (" + e.getMessage() + ")");
                }
            }
        }
        if(config.get("pack") != null) {
            if (!(config.get("pack") instanceof String)) {
                plugin.getLogger().log(Level.WARNING, "'pack' option has to be a String!");
            } else if (!((String) config.get("pack")).isEmpty()) {
                ResourcePack pack = getByName((String) config.get("pack"));
                if (pack != null) {
                    assignment.setPack(pack);
                    plugin.getLogger().log(plugin.getLogLevel(), "Pack: " + pack.getName());
                } else {
                    plugin.getLogger().log(Level.WARNING, "No pack with the name " + config.get("pack") + " defined?");
                }
            }
        }
        if(config.get("secondary") != null) {
            if (!(config.get("secondary") instanceof List)
                    || !((List) config.get("secondary")).isEmpty()
                    && !(((List) config.get("secondary")).get(0) instanceof String)){
                plugin.getLogger().log(Level.WARNING, "'secondary' option has to be a String List!");
            } else {
                plugin.getLogger().log(plugin.getLogLevel(), "Secondary packs:");
                List<String> secondary = (List<String>) config.get("secondary");
                for(String secondaryPack : secondary) {
                    ResourcePack pack = getByName(secondaryPack);
                    if (pack != null) {
                        assignment.addSecondary(pack);
                        plugin.getLogger().log(plugin.getLogLevel(), "- " + pack.getName());
                    } else {
                        plugin.getLogger().log(Level.WARNING, "No pack with the name " + config.get("pack") + " defined?");
                    }
                }
            }
        }
        if (config.get("send-delay") != null) {
            if (!(config.get("send-delay") instanceof Number)) {
                plugin.getLogger().log(Level.WARNING, "'send-delay' option has to be a number!");
            } else {
                assignment.setSendDelay(((Number) config.get("send-delay")).longValue());
                plugin.getLogger().log(plugin.getLogLevel(), "Send delay: " + assignment.getSendDelay());
            }
        }
        return assignment;
    }

    /**
     * Removes the pack of a server
     * @param server The server the pack should get removed from
     * @return True if the server had a pack, false if not
     */
    @Deprecated
    public boolean removeServer(String server) {
        return removeAssignment(server);
    }

    /**
     * Removes the assignment of a server/world
     * @param key   The name of the server/world the pack should get removed from
     * @return True if there was a assignment for that key, false if not
     */
    public boolean removeAssignment(String key) {
        if (literalAssignments.remove(key.toLowerCase()) != null) {
            regexAssignments.remove(key.toLowerCase());
            checkDirty();
            return true;
        }
        return false;
    }

    /**
     * Removes the assignment of a server/world
     * @param assignment    The assigned to remove
     * @return True if there was a assignment for that key, false if not
     */
    public boolean removeAssignment(PackAssignment assignment) {
        boolean removed;
        if (assignment.getRegex() != null) {
            removed = regexAssignments.remove(assignment.getName().toLowerCase()) != null;
        } else {
            removed = literalAssignments.remove(assignment.getName().toLowerCase()) != null;
        }
        checkDirty();
        return removed;
    }

    /**
     * Add a secondary server Resource Pack
     * @param server The server to add a secondary pack to
     * @param pack The pack to add to the list of secondary ones
     * @return False if the pack already was in the list; True if not
     * @deprecated Use the {@link PackManager#getAssignment(String)} and {@link PackAssignment#addSecondary(ResourcePack)}
     */
    @Deprecated
    public boolean addServerSecondary(String server, ResourcePack pack) {
        return getAssignment(server).addSecondary(pack);
    }

    /**
     * Add a secondary server Resource Pack
     * @param server The server to add a secondary pack to
     * @param packname The name of the pack to add to the list of secondary ones
     * @return False if the pack already was in the list; True if not
     * @deprecated Use the {@link PackManager#getAssignment(String)} and {@link PackAssignment#addSecondary(String)}
     */
    @Deprecated
    public boolean addServerSecondary(String server, String packname) {
        return getAssignment(server).addSecondary(packname);
    }

    /**
     * Get if a pack is in the list of secondary Resource Packs for this server
     * @param server The check the secondary pack of
     * @param pack The pack to check
     * @return True if it is a global secondary pack, false if not
     * @deprecated Use the {@link PackManager#getAssignment(String)} and {@link PackAssignment#isSecondary(ResourcePack)}
     */
    @Deprecated
    public boolean isServerSecondary(String server, ResourcePack pack) {
        return getAssignment(server).isSecondary(pack);
    }

    /**
     * Get if a pack is in the list of secondary Resource Packs for this server
     * @param server The server to add a secondary pack to
     * @param packname The name of the pack to check
     * @return True if it is a global secondary pack, false if not
     * @deprecated Use {@link PackManager#getAssignment(String)} and {@link PackAssignment#isSecondary(String)}
     */
    @Deprecated
    public boolean isServerSecondary(String server, String packname) {
        return getAssignment(server).isSecondary(packname);
    }

    /**
     * Get the list of secondary packs of a specific server
     * @param server The name of the server
     * @return The list of secondary packs; empty if none found
     * @deprecated Use {@link PackManager#getAssignment(String)} and {@link PackAssignment#getSecondaries()}
     */
    @Deprecated
    public List<String> getServerSecondary(String server) {
        return new ArrayList<>(getAssignment(server).getSecondaries());
    }

    /**
     * Set the pack of a player and send it to him, calls a ResourcePackSendEvent
     * @param playerId  The UUID of the player to set the pack for
     * @param pack      The ResourcePack to set, if it is null it will reset to empty if the player has a pack applied
     * @return <tt>true</tt> if the pack was set; <tt>false</tt> if not
     */
    public boolean setPack(UUID playerId, ResourcePack pack) {
        return setPack(playerId, pack, true);
    }

    /**
     * Set the pack of a player and send it to him, calls a ResourcePackSendEvent
     * @param playerId  The UUID of the player to set the pack for
     * @param pack      The ResourcePack to set, if it is null it will reset to empty if the player has a pack applied
     * @param temporary Should the pack be removed on log out or stored?
     * @return <tt>true</tt> if the pack was set; <tt>false</tt> if not
     */
    public boolean setPack(UUID playerId, ResourcePack pack, boolean temporary) {
        ResourcePack prev = plugin.getUserManager().getUserPack(playerId);
        if (!temporary) {
            if (pack == null) {
                plugin.setStoredPack(playerId, null);
            } else {
                plugin.setStoredPack(playerId, pack.getName());
                plugin.getUserManager().updatePackTime(playerId);
            }
        }
        if (pack == null) {
            pack = getByName(plugin.getStoredPack(playerId));
            if (pack != null) {
                plugin.getLogger().log(plugin.getLogLevel(), playerId + " has the pack " + pack.getName() + " stored!");
            }
        }
        if (pack != null && pack.equals(prev)) {
            return false;
        }
        if (prev == null && (pack == null || pack.equals(getEmptyPack()))) {
            return false;
        }
        IResourcePackSendEvent sendEvent = plugin.callPackSendEvent(playerId, pack);
        if (sendEvent.isCancelled()) {
            plugin.getLogger().log(plugin.getLogLevel(), "Pack send event for " + playerId + " was cancelled!");
            return false;
        }
        pack = sendEvent.getPack();
        if (pack == null && prev != null) {
            pack = getEmptyPack();
        }
        if (pack != null && !pack.equals(prev)) {
            plugin.getUserManager().setUserPack(playerId, pack);
            plugin.sendPack(playerId, pack);
            return true;
        }
        return false;
    }

    /**
     * Apply the pack that a player should have on that server/world
     * @param playerId      The UUID of the player
     * @param serverName    The name of the server/world
     */
    public void applyPack(UUID playerId, String serverName) {
        ResourcePack pack = getApplicablePack(playerId, serverName);
        setPack(playerId, pack);
    }

    /**
     * Get the pack the player should have on that server
     * @param playerId The UUID of the player
     * @param serverName The name of the server
     * @return The pack for that server; <tt>null</tt> if he should have none
     */
    public ResourcePack getApplicablePack(UUID playerId, String serverName) {
        ResourcePack prev = plugin.getUserManager().getUserPack(playerId);
        ResourcePack pack = null;
        ResourcepacksPlayer player = plugin.getPlayer(playerId);
        if (player == null) {
            player = new ResourcepacksPlayer("uuid:" + playerId, playerId);
        }
        IResourcePackSelectEvent.Status status = IResourcePackSelectEvent.Status.UNKNOWN;
        if(getGlobalAssignment().isSecondary(prev) && checkPack(playerId, prev, IResourcePackSelectEvent.Status.SUCCESS) == IResourcePackSelectEvent.Status.SUCCESS) {
            plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched global assignment");
            return prev;
        }
        if(serverName != null && !serverName.isEmpty()) {
            PackAssignment assignment = getAssignment(serverName);
            if(assignment.isSecondary(prev) && checkPack(playerId, prev, IResourcePackSelectEvent.Status.SUCCESS) == IResourcePackSelectEvent.Status.SUCCESS) {
                plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched assignment " + assignment.getName());
                return prev;
            }
            ResourcePack serverPack = getByName(assignment.getPack());
            status = checkPack(playerId, serverPack, status);
            if(status == IResourcePackSelectEvent.Status.SUCCESS) {
                pack = serverPack;
                plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched assignment " + assignment.getName());
            } else if(prev != null || serverPack != null){
                for(String secondaryName : assignment.getSecondaries()) {
                    ResourcePack secondaryPack = getByName(secondaryName);
                    status = checkPack(playerId, secondaryPack, status);
                    if(status == IResourcePackSelectEvent.Status.SUCCESS) {
                        pack = secondaryPack;
                        plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched assignment " + assignment.getName());
                        break;
                    }
                }
            }
        }
        if(pack == null) {
            ResourcePack globalPack = getByName(getGlobalAssignment().getPack());
            status = checkPack(playerId, globalPack, status);
            if(status == IResourcePackSelectEvent.Status.SUCCESS) {
                pack = globalPack;
                plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched global assignment");
            } else if(prev != null || globalPack != null){
                for (String secondaryName : getGlobalAssignment().getSecondaries()) {
                    ResourcePack secondaryPack = getByName(secondaryName);
                    status = checkPack(playerId, secondaryPack, status);
                    if(status == IResourcePackSelectEvent.Status.SUCCESS) {
                        plugin.getLogger().log(plugin.getLogLevel(), player.getName() + " matched global assignment");
                        pack = secondaryPack;
                        break;
                    }
                }
            }
        }

        if(pack != null) {
            status = IResourcePackSelectEvent.Status.SUCCESS;
        }

        IResourcePackSelectEvent selectEvent = plugin.callPackSelectEvent(playerId, pack, status);
        return selectEvent.getPack();
    }

    protected IResourcePackSelectEvent.Status checkPack(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status) {
        if(pack == null) {
            return status;
        }
        boolean rightFormat = pack.getFormat() <= plugin.getPlayerPackFormat(playerId);
        boolean hasPermission = !pack.isRestricted() || plugin.checkPermission(playerId, pack.getPermission());
        if(rightFormat && hasPermission) {
            return IResourcePackSelectEvent.Status.SUCCESS;
        }
        if(status != IResourcePackSelectEvent.Status.NO_PERM_AND_WRONG_VERSION) {
            if(!rightFormat) {
                if(!hasPermission || status == IResourcePackSelectEvent.Status.NO_PERMISSION) {
                    status = IResourcePackSelectEvent.Status.NO_PERM_AND_WRONG_VERSION;
                } else {
                    status = IResourcePackSelectEvent.Status.WRONG_VERSION;
                }
            }
            if(!hasPermission) {
                if(!rightFormat || status == IResourcePackSelectEvent.Status.WRONG_VERSION) {
                    status = IResourcePackSelectEvent.Status.NO_PERM_AND_WRONG_VERSION;
                } else {
                    status = IResourcePackSelectEvent.Status.NO_PERMISSION;
                }
            }
        }
        return status;
    }

    /**
     * Get a list of all packs
     * @return A new array list of packs
     */
    public List<ResourcePack> getPacks() {
        return new ArrayList<>(packNames.values());
    }

    /**
     * Download the pack files and generate sha1 hashes from them,
     * also saves the changes to the config!
     * @param sender The player that executed the command, null if it was the console
     */
    public void generateHashes(final ResourcepacksPlayer sender) {
        plugin.runAsyncTask(() -> {
            plugin.sendMessage(sender, "generate-hashes.generating");
            int changed = 0;

            for (ResourcePack pack : getPacks()) {
                if (pack.getName().startsWith("backend-")) {
                    continue;
                }
                Path target = new File(plugin.getDataFolder(), pack.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + "-downloaded.zip").toPath();
                InputStream in = null;
                try {
                    URL url = new URL(pack.getUrl());
                    plugin.sendMessage(sender, "generate-hashes.downloading",
                            "pack", pack.getName(),
                            "url", pack.getUrl(),
                            "hash", pack.getHash()
                    );
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestProperty("User-Agent", plugin.getName() + "/" + plugin.getVersion());
                    in = con.getInputStream();
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

                    byte[] hash = Hashing.sha1().hashBytes(Files.readAllBytes(target)).asBytes();
                    if (!Arrays.equals(pack.getRawHash(), hash)) {
                        packHashes.remove(pack.getHash());
                        pack.setRawHash(hash);
                        packHashes.put(pack.getHash(), pack);
                        changed++;
                    }
                    plugin.sendMessage(sender, "generate-hashes.hash-sum",
                            "pack", pack.getName(),
                            "url", pack.getUrl(),
                            "hash", pack.getHash()
                    );
                    Files.deleteIfExists(target);
                } catch (MalformedURLException e) {
                    plugin.sendMessage(sender, Level.SEVERE, "generate-hashes.invalid-url",
                            "pack", pack.getName(),
                            "url", pack.getUrl(),
                            "hash", pack.getHash(),
                            "error", e.getMessage()
                    );
                } catch (IOException e) {
                    plugin.sendMessage(sender, Level.SEVERE, "generate-hashes.failed-to-load-pack",
                            "pack", pack.getName(),
                            "url", pack.getUrl(),
                            "hash", pack.getHash(),
                            "error", e.getMessage()
                    );
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            plugin.sendMessage(sender, Level.SEVERE, "generate-hashes.failed-to-load-pack",
                                    "pack", pack.getName(),
                                    "url", pack.getUrl(),
                                    "hash", pack.getHash(),
                                    "error", e.getMessage()
                            );
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (changed > 0) {
                plugin.sendMessage(sender, "generate-hashes.changed", "amount", String.valueOf(changed));
                plugin.runTask(plugin::saveConfigChanges);
            } else {
                plugin.sendMessage(sender, "generate-hashes.none-changed");
            }
        });
    }

    /**
     * Get the format of the pack a player can maximally use
     * @param version The Protocol version to get the format for
     * @return The pack format; <tt>-1</tt> if the player has an unknown version
     */
    public int getPackFormat(int version) {
        if (version >= 348) { // pre 1.13 / 17w48a
            return 4;
        } else if (version >= 210) { // pre 1.11
            return 3;
        } else if (version >= 49) { // pre 1.9 / 15w31a
            return 2;
        } else if (version >= 47) { // pre 1.8
            return 1;
        } else if (version >= 0) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Mark the manager state as dirty so it gets saved on next modification
     * @param dirty Whether or not this manager state should be considered dirty
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Check whether or not the manager state is dirty and if so save the config
     */
    public void checkDirty() {
        if (dirty) {
            dirty = false;
            plugin.saveConfigChanges();
        }
    }
}
