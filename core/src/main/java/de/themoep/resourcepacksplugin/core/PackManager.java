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
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent.Status;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 25.03.2015.
 */
public class PackManager {

    public static final String EMPTY_IDENTIFIER = "empty";

    public static final String HASH_KEY = "#hash=";

    private static final String[] URL_DENY_LIST = new String[] {
            "drive.google.com",
            "mediafire.com"
    };

    private static final String[] URL_WARN_LIST = new String[] {
            "dropbox.com"
    };

    private static final int[] PACK_FORMATS = new int[] {
            MinecraftVersion.MINECRAFT_1_21_6.getProtocolNumber(), 63,
            MinecraftVersion.MINECRAFT_1_21_5.getProtocolNumber(), 55,
            MinecraftVersion.MINECRAFT_1_21_4.getProtocolNumber(), 46,
            MinecraftVersion.MINECRAFT_1_21_2.getProtocolNumber(), 42,
            MinecraftVersion.MINECRAFT_1_21.getProtocolNumber(), 34,
            MinecraftVersion.MINECRAFT_1_20_5.getProtocolNumber(), 32,
            MinecraftVersion.MINECRAFT_1_20_3.getProtocolNumber(), 22,
            MinecraftVersion.MINECRAFT_1_20_2.getProtocolNumber(), 18,
            MinecraftVersion.MINECRAFT_1_20.getProtocolNumber(), 15,
            MinecraftVersion.MINECRAFT_1_19_4.getProtocolNumber(), 13,
            MinecraftVersion.MINECRAFT_1_19_3.getProtocolNumber(), 12,
            MinecraftVersion.MINECRAFT_1_19.getProtocolNumber(), 9,
            MinecraftVersion.MINECRAFT_1_18.getProtocolNumber(), 8,
            MinecraftVersion.MINECRAFT_1_17.getProtocolNumber(), 7,
            749, 6, // 1.16.2 / release candidate 1
            565, 5, // 1.15 / pre release 1
            348, 4, // pre 1.13 / 17w48a
            210, 3, // pre 1.11
            49, 2, // pre 1.9 / 15w31a
            47, 1, // pre 1.8
            0, 0
    };

    private final ResourcepacksPlugin plugin;

    private WatchService watchService = null;

    private Multimap<WatchKey, BiConsumer<Path, WatchEvent.Kind<Path>>> fileWatchers;

    /**
     * packname -> ResourcePack
     */
    private Map<String, ResourcePack> packNames;

    /**
     * packuuid -> packname
     */
    private Map<UUID, ResourcePack> packUuids;

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
    private boolean storedPacksOverride;
    private boolean appendHashToUrl = true;


    public PackManager(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            plugin.log(Level.WARNING, "Unable to create file watcher!", e);
        }
    }

    /**
     * Initialize this pack manager
     */
    public void init() {
        packNames = new LinkedHashMap<>();
        packUuids = new HashMap<>();
        packHashes = new HashMap<>();
        packUrls = new HashMap<>();
        empty = null;
        global = new PackAssignment("global");
        literalAssignments = new LinkedHashMap<>();
        regexAssignments = new LinkedHashMap<>();
        fileWatchers = MultimapBuilder.hashKeys().linkedListValues().build();
        plugin.runAsyncTask(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Collection<BiConsumer<Path, WatchEvent.Kind<Path>>> watchers = fileWatchers.get(key);
                        plugin.logDebug("Received file change event " + event.kind() + " of " + event.context() + " with " + watchers.size() + " watchers!");
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;
                        for (BiConsumer<Path, WatchEvent.Kind<Path>> watcher : watchers) {
                            watcher.accept(watchEvent.context(), watchEvent.kind());
                        }
                    }
                    key.reset();
                } catch (InterruptedException | ClosedWatchServiceException ignored) {
                    // just end the thread
                    return;
                }
            }
        });
    }

    private void registerFileWatcher(Path path, Consumer<Path> consumer) {
        // Execute consumer at least once on registration
        consumer.accept(path);
        if (watchService == null) {
            // No watch service
            return;
        }

        Path dir = path;
        if (!Files.isDirectory(dir)) {
            dir = path.toAbsolutePath().getParent();
        }
        try {
            WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            fileWatchers.put(key, (p, k) -> {
                // Check if it's our file
                if (path.getFileName().equals(p.getFileName())) {
                    consumer.accept(path);
                }
            });
        } catch (IOException e) {
            try {
                watchService.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            watchService = null;
            plugin.log(Level.WARNING, "Unable to register file watcher. Falling back to manual calculation!", e);
        }
    }

    private void registerPackHashWatcher(ResourcePack pack) {
        if (pack.getLocalPath() == null || pack.getLocalPath().isEmpty()) {
            return;
        }
        Path path = Paths.get(pack.getLocalPath());
        registerFileWatcher(path, p -> {
            try {
                if (Files.isRegularFile(p)) {
                    byte[] bytes = Files.readAllBytes(p);
                    HashCode hash = Hashing.sha1().hashBytes(bytes);
                    setPackHash(pack, hash.toString());
                }
            } catch (IOException e) {
                plugin.getPluginLogger().log(Level.WARNING, "Unable to create hash of " + path, e);
            }
        });
    }

    /**
     * Creates a new ResourcePack instance from a config. Does not add it!
     * @param name      The name of the pack
     * @param config
     * @return
     */
    public ResourcePack loadPack(String name, Map<String, Object> config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("Pack " + name + " had a null config?");
        }
        String url = get(config, "url", "");
        validateUrl(url);
        List<?> variantsList = get(config, "variants", new ArrayList<ResourcePack>());
        if (url.isEmpty() && variantsList.isEmpty()) {
            throw new IllegalArgumentException("Pack " + name + " does not have an url defined!");
        }
        UUID uuid;
        String uuidStr = get(config, "uuid", "");
        if (uuidStr.isEmpty()) {
            uuid = UUID.nameUUIDFromBytes(url.getBytes());
        } else {
            uuid = UUID.fromString(uuidStr);
        }
        String hash = get(config, "hash", "");

        String localPath = get(config, "local-path", "");

        int format = get(config, "format", 0);
        String mcVersion = get(config , "version", String.valueOf(get(config, "version", 0)));

        boolean restricted = get(config, "restricted", false);
        String perm = get(config, "permission", plugin.getName().toLowerCase(Locale.ROOT) + ".pack." + name);

        ClientType type = ClientType.valueOf(get(config, "type", "original").toUpperCase(Locale.ROOT));

        ResourcePack pack = new ResourcePack(name, uuid, url, hash, localPath, format, 0, restricted, perm, type);
        try {
            pack.setVersion(mcVersion);
        } catch (IllegalArgumentException e) {
            plugin.log(Level.WARNING, "Unable to set version of pack " + name + ". " + e.getMessage());
        }

        for (int i = 0; i < variantsList.size(); i++) {
            pack.getVariants().add(loadPack(name + "-variant-" + (i + 1), plugin.getConfigMap(variantsList.get(i))));
        }

        return pack;
    }

    private static <T> T get(Map<String, Object> config, String path, T def) {
        Object o = config.getOrDefault(path, def);
        if (o != null && def != null && def.getClass().isAssignableFrom(o.getClass())) {
            return (T) o;
        }
        return def;
    }

    /**
     * Check whether an url is in the deny or warn list and throws an exception or logs a warning
     * @param url The url to check
     * @throws IllegalArgumentException If the url is in the deny list
     */
    private void validateUrl(String url) throws IllegalArgumentException {
        for (String deny : URL_DENY_LIST) {
            if (url.contains(deny)) {
                throw new IllegalArgumentException("URL " + url + " is not allowed! " + deny + " does not work for hosting packs as they either block downloads from the Minecraft client or do not provide permanent direct links! Use your own webserver or https://mc-packs.net to host the pack instead!");
            }
        }
        for (String warn : URL_WARN_LIST) {
            if (url.contains(warn)) {
                plugin.log(Level.WARNING, "Detected a potential issue with pack URL " + url + ": " + warn + " might not reliably work for hosting packs as they tend to eventually block downloads from the Minecraft client! Use your own webserver or https://mc-packs.net to host the pack instead!");
            }
        }
    }

    /**
     * Registers a new resource pack with the packmanager
     * @param pack The resourcepack to register
     * @return If a pack with that name was known before it returns the past pack, null if none was known
     * @throws IllegalArgumentException when there already is a pack with the same url or hash but not name defined
     */
    public ResourcePack addPack(ResourcePack pack) throws IllegalArgumentException {
        if (pack.getVariants().isEmpty()) {
            cacheVariant(pack, pack);
        } else {
            for (ResourcePack variant : pack.getVariants()) {
                cacheVariant(variant, pack);
            }
        }
        return packNames.put(pack.getName().toLowerCase(Locale.ROOT), pack);
    }

    /**
     * Unregisters a resource pack from the packmanager
     * @param pack The resourcepack to unregister
     * @return If that pack was known before it returns true, if not false
     */
    public boolean removePack(ResourcePack pack) {
        boolean known = false;
        if (pack.getVariants().isEmpty()) {
            if (pack.getUrl() != null && pack.getUrl().isEmpty()) {
                known |= packUrls.remove(pack.getUrl(), pack);
            }
            if (pack.getUuid() != null) {
                known |= packUuids.remove(pack.getUuid(), pack);
            }
            if (pack.getHash().length() > 0) {
                known |= packHashes.remove(pack.getHash(), pack);
            }
        } else {
            for (ResourcePack variant : pack.getVariants()) {
                known |= uncacheVariant(variant, pack);
            }
        }
        return packNames.remove(pack.getName().toLowerCase(Locale.ROOT), pack) || known;
    }

    private void cacheVariant(ResourcePack variant, ResourcePack pack) throws IllegalArgumentException {
        if (variant.getVariants().isEmpty()) {
            try {
                ensureUniqueData(variant, pack.getName());
                if (variant.getUrl() != null && !variant.getUrl().isEmpty()) {
                    packUrls.putIfAbsent(variant.getUrl(), pack);
                }
                if (variant.getHash().length() > 0) {
                    packHashes.put(variant.getHash(), pack);
                }
                if (variant.getUuid() != null) {
                    packUuids.put(variant.getUuid(), pack);
                }
            } catch (IllegalArgumentException e) {
                plugin.log(Level.WARNING, e.getMessage() + " This might cause issues in some cases" +
                        " especially when sending packs from a Minecraft server with this plugin on the proxy!");
            }
            registerPackHashWatcher(variant);
        } else {
            for (ResourcePack variantVariant : variant.getVariants()) {
                cacheVariant(variantVariant, pack);
            }
        }
    }

    /**
     * Ensures that the pack has unique hash, uuid and url specifications
     * @param pack      The pack to check
     * @param packName  The pack name to check for if it's a different pack
     */
    private void ensureUniqueData(ResourcePack pack, String packName) {
        ResourcePack byHash = getByHash(pack.getHash());
        if (byHash != null && !byHash.getName().equalsIgnoreCase(packName)) {
            throw new IllegalArgumentException("There is already a pack with same hash '" + pack.getHash() + "' as pack '" + packName + "' (" + pack.getName() + ") defined: " + byHash.getName());
        }
        ResourcePack byUuid = getByUuid(pack.getUuid());
        if (byUuid != null && !byUuid.getName().equalsIgnoreCase(packName)) {
            throw new IllegalArgumentException("There is already a pack with same uuid '" + pack.getUuid() + "' as pack '" + packName + "' (" + pack.getName() + ") defined: " + byUuid.getName());
        }
        if (pack.getUrl() != null && !pack.getUrl().isEmpty()) {
            ResourcePack byUrl = getByUrl(pack.getUrl());
            if (byUrl != null && !byUrl.getName().equalsIgnoreCase(packName)) {
                throw new IllegalArgumentException("There is already a pack with same url '" + pack.getUrl() + "' as pack '" + packName + "' (" + pack.getName() + ") defined: " + byUrl.getName());
            }
        }
    }

    private boolean uncacheVariant(ResourcePack variant, ResourcePack pack) {
        boolean known = false;
        if (variant.getVariants().isEmpty()) {
            known |= packUrls.remove(variant.getUrl(), pack);
            known |= packHashes.remove(variant.getHash(), pack);
            known |= packUuids.remove(variant.getUuid(), pack);
        } else {
            for (ResourcePack variantVariant : variant.getVariants()) {
                known |= uncacheVariant(variantVariant, pack);
            }
        }
        return known;
    }

    /**
     * Set the uuid of a pack to a new value
     * @param pack The pack to update
     * @param uuid The uuid to set
     * @return Whether or not the hash changed
     */
    public boolean setPackUuid(ResourcePack pack, UUID uuid) {
        if (pack.getUuid().equals(uuid)) {
            return false;
        }
        packUuids.remove(pack.getUuid());
        pack.setUuid(uuid);
        packUuids.put(pack.getUuid(), pack);
        return true;
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
     * Set the url of a pack to a new value
     * @param pack The pack to update
     * @param path The new local path to set
     * @return Whether or not the url changed
     */
    public boolean setPackPath(ResourcePack pack, String path) {
        String oldPath = pack.getLocalPath();
        if (path == null && oldPath != null) {
            pack.setLocalPath(null);
            return true;
        }
        if (oldPath == null || oldPath.equals(path)) {
            return false;
        }
        pack.setLocalPath(path);
        registerPackHashWatcher(pack);
        return true;
    }

    /**
     * Get the resourcepack by its name
     * @param name The name of the pack to get
     * @return The resourcepack with that name, null if there is none
     */
    public ResourcePack getByName(String name) {
        return name != null ? packNames.get(name.toLowerCase(Locale.ROOT)) : null;
    }

    /**
     * Get the resourcepack by its uuid
     * @param uuid The uuid of the pack to get
     * @return The resourcepack with that uuid, null if there is none
     */
    public ResourcePack getByUuid(UUID uuid) {
        return packUuids.get(uuid);
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
        if (url.contains(HASH_KEY)) {
            url = url.substring(0, url.lastIndexOf(HASH_KEY));
        }
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
     * Set whether or not stored packs should override assignments
     * @param playerPacksOverride Whether or not stored packs should override assignments
     */
    public void setStoredPacksOverride(boolean playerPacksOverride) {
        this.storedPacksOverride = playerPacksOverride;
    }

    /**
     * Get whether or not stored packs should override assignments
     * @return Whether or not stored packs should override assignments
     */
    public boolean getStoredPacksOverride() {
        return storedPacksOverride;
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
            previous = regexAssignments.put(assignment.getName().toLowerCase(Locale.ROOT), assignment);
        } else {
            previous = literalAssignments.put(assignment.getName().toLowerCase(Locale.ROOT), assignment);
        }
        checkDirty();
        return previous;
    }

    /**
     * Get the assignment of a server/world
     * @param server    The name of the server/world
     * @return          The PackAssignment; an empty one if there is none
     */
    public @NonNull PackAssignment getAssignment(String server) {
        PackAssignment assignment = literalAssignments.get(server.toLowerCase(Locale.ROOT));
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
        PackAssignment assignment = literalAssignments.get(name.toLowerCase(Locale.ROOT));
        if (assignment == null) {
            assignment = regexAssignments.get(name.toLowerCase(Locale.ROOT));
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
                plugin.log(Level.WARNING, "'regex' option has to be a String!");
            } else {
                try {
                    assignment.setRegex(Pattern.compile(((String) config.get("regex"))));
                    plugin.logDebug("Regex: " + assignment.getRegex().toString());
                } catch (PatternSyntaxException e) {
                    plugin.log(Level.WARNING, "The assignment's regex '" + config.get("regex") + "' isn't valid! Using the key name literally! (" + e.getMessage() + ")");
                }
            }
        }
        if(config.get("pack") != null) {
            if (!(config.get("pack") instanceof String)) {
                plugin.log(Level.WARNING, "'pack' option has to be a String!");
            } else if (!((String) config.get("pack")).isEmpty()) {
                ResourcePack pack = getByName((String) config.get("pack"));
                if (pack != null) {
                    assignment.addPack(pack);
                    plugin.logDebug("Pack: " + pack.getName());
                } else {
                    plugin.log(Level.WARNING, "No pack with the name " + config.get("pack") + " defined?");
                }
            }
        }
        Object packs = config.getOrDefault("packs", new ArrayList<>());
        if (packs != null) {
            if (!(packs instanceof List)
                    || !((List) packs).isEmpty()
                    && !(((List) packs).get(0) instanceof String)) {
                plugin.log(Level.WARNING, "'packs' option has to be a String List!");
            } else if (!((List) packs).isEmpty()) {
                plugin.logDebug("Packs:");
                List<String> packName = (List<String>) packs;
                for (String secondaryPack : packName) {
                    ResourcePack pack = getByName(secondaryPack);
                    if (pack != null) {
                        assignment.addPack(pack);
                        plugin.logDebug("- " + pack.getName());
                    } else {
                        plugin.log(Level.WARNING, "No pack with the name " + secondaryPack + " defined?");
                    }
                }
            }
        }
        Object optionalPacks = config.getOrDefault("optional-packs", config.get("secondary"));
        if(optionalPacks != null) {
            if (!(optionalPacks instanceof List)
                    || !((List) optionalPacks).isEmpty()
                    && !(((List) optionalPacks).get(0) instanceof String)) {
                plugin.log(Level.WARNING, "'optional-packs' option has to be a String List!");
            } else {
                plugin.logDebug("Optional packs:");
                List<String> optionalName = (List<String>) optionalPacks;
                for(String secondaryPack : optionalName) {
                    ResourcePack pack = getByName(secondaryPack);
                    if (pack != null) {
                        assignment.addOptionalPack(pack);
                        plugin.logDebug("- " + pack.getName());
                    } else {
                        plugin.log(Level.WARNING, "No pack with the name " + secondaryPack + " defined?");
                    }
                }
            }
        }
        if (config.get("send-delay") != null) {
            if (!(config.get("send-delay") instanceof Number)) {
                plugin.log(Level.WARNING, "'send-delay' option has to be a number!");
            } else {
                assignment.setSendDelay(((Number) config.get("send-delay")).longValue());
                plugin.logDebug("Send delay: " + assignment.getSendDelay());
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
        if (literalAssignments.remove(key.toLowerCase(Locale.ROOT)) != null) {
            regexAssignments.remove(key.toLowerCase(Locale.ROOT));
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
            removed = regexAssignments.remove(assignment.getName().toLowerCase(Locale.ROOT)) != null;
        } else {
            removed = literalAssignments.remove(assignment.getName().toLowerCase(Locale.ROOT)) != null;
        }
        checkDirty();
        return removed;
    }

    /**
     * Remove a specific pack from the player
     * @param playerId The UUID of the player to remove the pack from
     * @param pack The pack to remove
     */
    private void removePack(UUID playerId, ResourcePack pack) {
        if (!plugin.supportsMultiplePacks(playerId))
            throw new UnsupportedOperationException("The client version of " + playerId + " does not support pack removal!");

        plugin.getUserManager().removeUserPack(playerId, pack);
        plugin.removePack(playerId, pack);
        for (ResourcePack variant : pack.getVariants()) {
            plugin.removePack(playerId, variant);
        }
    }

    /**
     * Set the pack of a player and send it to him, calls a ResourcePackSendEvent
     * @param playerId  The UUID of the player to set the pack for
     * @param pack      The ResourcePack to set, if it is null it will reset to empty if the player has a pack applied
     * @return The result of setting a pack
     */
    public @NonNull PackSetResult setPack(UUID playerId, ResourcePack pack) {
        return setPack(playerId, pack, true);
    }

    /**
     * Set the pack of a player and send it to him, calls a ResourcePackSendEvent
     * @param playerId  The UUID of the player to set the pack for
     * @param pack      The ResourcePack to set, if it is null/empty it will reset to empty if the player has a pack applied
     * @param temporary Should the pack be removed on log out or stored?
     * @return The result of setting a pack
     */
    public @NonNull PackSetResult setPack(UUID playerId, ResourcePack pack, boolean temporary) {
        return setPack(playerId, pack, temporary, pack == null || pack.equals(getEmptyPack()));
    }

    /**
     * Set the pack of a player and send it to him, calls a ResourcePackSendEvent
     * @param playerId  The UUID of the player to set the pack for
     * @param pack      The ResourcePack to set
     * @param temporary Should the pack be removed on log out or stored?
     * @param removeExisting Should existing packs be removed? (Only works on 1.20.3+, versions before that will always remove)
     * @return The result of setting a pack
     */
    public @NonNull PackSetResult setPack(UUID playerId, ResourcePack pack, boolean temporary, boolean removeExisting) {
        List<ResourcePack> prev = plugin.getUserManager().getUserPacks(playerId);
        if (!temporary) {
            if (pack == null) {
                plugin.setStoredPack(playerId, null);
            } else {
                plugin.setStoredPack(playerId, pack.getName());
                plugin.getUserManager().updatePackTime(playerId);
            }
        }
        if (pack == null) {
            ResourcePack stored = getByName(plugin.getStoredPack(playerId));
            if (stored != null && checkPack(playerId, stored, Status.SUCCESS) == Status.SUCCESS) {
                pack = stored;
                plugin.logDebug(playerId + " has the pack " + stored.getName() + " stored!");
            }
        }
        if (plugin.supportsMultiplePacks(playerId) && removeExisting) {
            for (ResourcePack existing : prev) {
                if (existing != pack) {
                    removePack(playerId, existing);
                }
            }
        }
        if (pack != null && prev.contains(pack)) {
            ResourcePack variant = getMatchingVariant(playerId, pack).getPack();
            return new PackSetResult(variant != null ? variant : pack, Status.ALREADY_APPLIED);
        }
        if (prev.isEmpty() && (pack == null || pack.equals(getEmptyPack()))) {
            return new PackSetResult(null, Status.IS_EMPTY);
        }
        if (pack != null && pack.getType() == ClientType.BEDROCK) {
            // TODO: Find way to change client pack for Bedrock players
            return new PackSetResult(null, Status.UNKNOWN);
        }
        IResourcePackSendEvent sendEvent = plugin.callPackSendEvent(playerId, pack);
        if (sendEvent.isCancelled()) {
            plugin.logDebug("Pack send event for " + playerId + " was cancelled!");
            return new PackSetResult(null, Status.UNKNOWN);
        }
        pack = processSendEvent(sendEvent, prev);
        if (pack != null) {
            PackSetResult variant = getMatchingVariant(playerId, pack);
            if (variant.getPack() != null) {
                sendPack(playerId, variant.getPack());
            }
            return variant;
        }
        return new PackSetResult(null, Status.UNKNOWN);
    }

    /**
     * Get the variant of a pack that should be applied to a player
     * @param playerId The UUID of the player
     * @param pack     The pack to get the variant for
     * @return The resulting variant or error status
     */
    private PackSetResult getMatchingVariant(UUID playerId, ResourcePack pack) {
        if (pack.getVariants().isEmpty()) {
            return new PackSetResult(pack, Status.SUCCESS);
        } else {
            Status status = Status.SUCCESS;
            for (ResourcePack variant : pack.getVariants()) {
                Status varStatus = checkPack(playerId, variant, Status.UNKNOWN);
                if (varStatus == Status.SUCCESS) {
                    return new PackSetResult(variant, Status.SUCCESS);
                }
                if (varStatus.ordinal() > status.ordinal()) {
                    status = varStatus;
                }
            }
            return new PackSetResult(null, status);
        }
    }

    /**
     * Send the pack
     * @param playerId The UUID of the player to send the pack to
     * @param pack The pack to send
     */
    private void sendPack(UUID playerId, ResourcePack pack) {
        // If there is no watch service running and a local path is set for the pack then calculate the hash
        if (watchService == null && pack.getLocalPath() != null && !pack.getLocalPath().isEmpty()) {
            Path path = Paths.get(pack.getLocalPath());
            if (Files.exists(path) && Files.isRegularFile(path)) {
                plugin.runAsyncTask(() -> {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        HashCode hash = Hashing.sha1().hashBytes(bytes);
                        setPackHash(pack, hash.toString());
                    } catch (IOException e) {
                        plugin.log(Level.WARNING, "Error while trying to read resource pack " + pack.getName() + " file from local path " + path, e);
                    }
                    plugin.runTask(() -> {
                        plugin.sendPack(playerId, pack);
                    });
                });
                return;
            }
        }

        plugin.sendPack(playerId, pack);
    }

    /**
     * Process the pack send event using the previous pack, this calculates if a pack should
     * be sent (if it's null then the empty one will be returned and when it isn't different
     * from the previous one then it will return null).
     * Will also set the pack of the player in the UserManager
     * @param event The event
     * @param prev The previous packs
     * @return The pack that should be sent to the player or null if no pack should be sent
     */
    public ResourcePack processSendEvent(IResourcePackSendEvent event, List<ResourcePack> prev) {
        ResourcePack pack = event.getPack();
        if (pack == null && !prev.isEmpty()) {
            pack = getEmptyPack();
        }
        if (pack != null && !prev.contains(pack)) {
            if (!plugin.supportsMultiplePacks(event.getPlayerId())) {
                plugin.getUserManager().clearUserPacks(event.getPlayerId());
            }
            plugin.getUserManager().addUserPack(event.getPlayerId(), pack);
            return pack;
        }
        return null;
    }

    /**
     * Apply the pack that a player should have on that server/world
     * @param playerId      The UUID of the player
     * @param serverName    The name of the server/world
     * @deprecated Use {@link #applyPack(ResourcepacksPlayer, String)} instead
     */
    @Deprecated
    public void applyPack(UUID playerId, String serverName) {
        applyPack(plugin.getPlayer(playerId), serverName);
    }

    /**
     * Apply the pack that a player should have on that server/world
     * @param player        The player
     * @param serverName    The name of the server/world
     * @return The packs that were loaded by the client
     */
    public Set<ResourcePack> applyPack(ResourcepacksPlayer player, String serverName) {
        UUID playerId = player.getUniqueId();
        LinkedHashSet<ResourcePack> sentPacks = new LinkedHashSet<>();
        LinkedHashSet<ResourcePack> packs = getApplicablePacks(player, serverName);
        if (plugin.supportsMultiplePacks(playerId)) {
            PackAssignment assignment = getAssignment(serverName);
            boolean packWasRemoved = false;
            List<ResourcePack> userPacks = plugin.getUserManager().getUserPacks(playerId);
            for (ResourcePack pack : userPacks) {
                if (!packs.contains(pack) && !getGlobalAssignment().isOptionalPack(pack) && !assignment.isOptionalPack(pack)) {
                    removePack(playerId, pack);
                    packWasRemoved = true;
                }
            }

            boolean alreadyHadPack = false;
            List<PackSetResult> appliedResults = new ArrayList<>();
            for (ResourcePack pack : packs) {
                PackSetResult packSetResult = setPack(playerId, pack);
                if (packSetResult.getPack() != null) {
                    alreadyHadPack |= packSetResult.getStatus() == Status.ALREADY_APPLIED;
                    appliedResults.add(packSetResult);
                }
            }
            if (!alreadyHadPack || packWasRemoved) {
                for (PackSetResult appliedResult : appliedResults) {
                    sentPacks.add(appliedResult.getPack());
                }
            }
        } else if (!packs.isEmpty()) {
            ResourcePack sentPack = setPack(playerId, packs.iterator().next()).getPack();
            if (sentPack != null) {
                sentPacks.add(sentPack);
            }
        }

        plugin.sendPackInfo(playerId);
        return sentPacks;
    }

    /**
     * Get the pack the player should have on that server
     * @param playerId The UUID of the player
     * @param serverName The name of the server
     * @return The packs for that server; an empty list if they should have none
     * @deprecated Use {@link #getApplicablePacks(ResourcepacksPlayer, String)} instead
     */
    @Deprecated
    public LinkedHashSet<ResourcePack> getApplicablePacks(UUID playerId, String serverName) {
        return getApplicablePacks(plugin.getPlayer(playerId), serverName);
    }

    /**
     * Get the pack the player should have on that server
     * @param player     The player
     * @param serverName The name of the server
     * @return The packs for that server; an empty list if they should have none
     */
    public LinkedHashSet<ResourcePack> getApplicablePacks(ResourcepacksPlayer player, String serverName) {
        UUID playerId = player.getUniqueId();
        List<ResourcePack> previousPacks = plugin.getUserManager().getUserPacks(playerId);
        LinkedHashSet<ResourcePack> packs = new LinkedHashSet<>();
        ResourcePack stored = getByName(plugin.getStoredPack(playerId));

        if (getStoredPacksOverride() && stored != null) {
            if (checkPack(playerId, stored, Status.SUCCESS) == Status.SUCCESS) {
                if (previousPacks.contains(stored)) {
                    plugin.logDebug(player.getName() + " already uses the stored pack " + stored.getName());
                } else {
                    plugin.logDebug(player.getName() + " had the pack " + stored.getName() + " stored, using that");
                }
                packs.add(stored);

                return packs;
            }
        }

        for (ResourcePack prev : previousPacks) {
            if (getGlobalAssignment().isOptionalPack(prev) && checkPack(playerId, prev, Status.SUCCESS) == Status.SUCCESS) {
                plugin.logDebug(player.getName() + " matched global as they already have the pack " + prev.getName());
                packs.add(prev);

                if (!plugin.supportsMultiplePacks(playerId)) {
                    return packs;
                }
            }
        }

        if (stored != null && getGlobalAssignment().isOptionalPack(stored) && checkPack(playerId, stored, Status.SUCCESS) == Status.SUCCESS) {
            plugin.logDebug(player.getName() + " has stored pack which matches the optional packs in the global assignment");
            packs.add(stored);

            if (!plugin.supportsMultiplePacks(playerId))
                return packs;
        }

        String matchReason = " due to ";
        Status status = Status.UNKNOWN;
        if (serverName != null && !serverName.isEmpty()) {
            PackAssignment assignment = getAssignment(serverName);
            for (ResourcePack prev : previousPacks) {
                if (assignment.isOptionalPack(prev) && checkPack(playerId, prev, Status.SUCCESS) == Status.SUCCESS) {
                    plugin.logDebug(player.getName() + " matched assignment " + assignment.getName() + " as they already have the pack " + prev.getName());
                    packs.add(prev);

                    if (!plugin.supportsMultiplePacks(playerId)) {
                        return packs;
                    }
                }
            }

            if (stored != null && assignment.isOptionalPack(stored) && checkPack(playerId, stored, Status.SUCCESS) == Status.SUCCESS) {
                plugin.logDebug(player.getName() + " matched assignment " + assignment.getName() + " as their stored pack is an optional packs");
                packs.add(stored);

                if (!plugin.supportsMultiplePacks(playerId)) {
                    return packs;
                }
            }


            List<ResourcePack> serverPacks = assignment.getPacks().stream()
                    .map(this::getByName)
                    .collect(Collectors.toCollection(ArrayList::new));
            status = checkPacks(playerId, serverPacks, Status.SUCCESS);
            matchReason = assignment.getName() + matchReason;
            if (status == Status.SUCCESS) {
                packs.addAll(serverPacks);
                matchReason += "main packs";
            } else if (!plugin.supportsMultiplePacks(playerId) && (!previousPacks.isEmpty() || !serverPacks.isEmpty())) {
                for (String secondaryName : assignment.getOptionalPacks()) {
                    ResourcePack secondaryPack = getByName(secondaryName);
                    status = checkPack(playerId, secondaryPack, status);
                    if (status == Status.SUCCESS) {
                        packs.add(secondaryPack);
                        matchReason += "secondary pack";
                        break;
                    }
                }
            }
        }

        if (packs.isEmpty()) {
            List<ResourcePack> globalPacks = getGlobalAssignment().getPacks().stream()
                    .map(this::getByName)
                    .collect(Collectors.toCollection(ArrayList::new));
            status = checkPacks(playerId, globalPacks, Status.SUCCESS);
            matchReason = "global due to ";
            if (status == Status.SUCCESS) {
                packs.addAll(globalPacks);
                matchReason += "main packs";
            } else if (!plugin.supportsMultiplePacks(playerId) && (!previousPacks.isEmpty() || !globalPacks.isEmpty())) {
                for (String secondaryName : getGlobalAssignment().getOptionalPacks()) {
                    ResourcePack secondaryPack = getByName(secondaryName);
                    status = checkPack(playerId, secondaryPack, status);
                    if (status == Status.SUCCESS) {
                        packs.add(secondaryPack);
                        matchReason += "secondary pack";
                        break;
                    }
                }
            }
        }

        if (status == Status.SUCCESS) {
            if (!packs.isEmpty()) {
                for (ResourcePack pack : packs) {
                    if (!pack.getVariants().isEmpty()) {
                        status = Status.UNKNOWN;
                        for (ResourcePack variant : pack.getVariants()) {
                            status = checkPack(playerId, variant, status);
                            if (status == Status.SUCCESS) {
                                matchReason += " variant";
                                break;
                            }
                        }
                    }
                }
            }
            if (status == Status.SUCCESS) {
                plugin.logDebug(player.getName() + " matched assignment " + matchReason);
            }
        }

        if (!packs.isEmpty()) {
            Set<Status> packStatuses = EnumSet.of(status);
            for (ResourcePack pack : packs) {
                if (!pack.getUrl().isEmpty() && pack.getRawHash().length > 0) {
                    packStatuses.add(Status.SUCCESS);
                }
            }
            status = packStatuses.stream()
                    .reduce((a, b) -> a.ordinal() > b.ordinal() ? a : b)
                    .orElse(Status.UNKNOWN);
        }

        IResourcePackSelectEvent selectEvent = plugin.callPackSelectEvent(playerId, new ArrayList<>(packs), status);
        if (selectEvent.getStatus() != Status.SUCCESS) {
            plugin.logDebug(player.getName() + " ResourcePackSelectEvent Status: " + selectEvent.getStatus());
        }
        return new LinkedHashSet<>(selectEvent.getPacks());
    }

    protected Status checkPacks(UUID playerId, List<ResourcePack> packs, Status status) {
        Set<Status> packStatuses = EnumSet.noneOf(Status.class);
        for (Iterator<ResourcePack> it = packs.iterator(); it.hasNext(); ) {
            ResourcePack pack = it.next();
            Status packStatus = checkPack(playerId, pack, status);
            packStatuses.add(packStatus);
            if (packStatus != Status.SUCCESS) {
                it.remove();
            }
        }
        if (packStatuses.contains(Status.SUCCESS)) {
            if (packStatuses.size() == 1) {
                return Status.SUCCESS;
            }
            return status;
        }
        return packStatuses.stream()
                .reduce((a, b) -> a.ordinal() > b.ordinal() ? a : b)
                .orElse(Status.UNKNOWN);
    }

    protected Status checkPack(UUID playerId, ResourcePack pack, Status status) {
        if (pack == null) {
            return status;
        }
        boolean rightFormat = pack.getType() == plugin.getPlayerClientType(playerId)
                && (plugin.getPlayerProtocol(playerId) < 0 /* unknown version */ || (
                        pack.getFormat() <= plugin.getPlayerPackFormat(playerId)
                                && pack.getVersion() <= plugin.getPlayerProtocol(playerId)));
        boolean hasPermission = !pack.isRestricted() || plugin.checkPermission(playerId, pack.getPermission());
        if(rightFormat && hasPermission) {
            return Status.SUCCESS;
        }
        if(status != Status.NO_PERM_AND_WRONG_VERSION) {
            if(!rightFormat) {
                if(!hasPermission || status == Status.NO_PERMISSION) {
                    status = Status.NO_PERM_AND_WRONG_VERSION;
                } else {
                    status = Status.WRONG_VERSION;
                }
            }
            if(!hasPermission) {
                if(!rightFormat || status == Status.WRONG_VERSION) {
                    status = Status.NO_PERM_AND_WRONG_VERSION;
                } else {
                    status = Status.NO_PERMISSION;
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
        List<ResourcePack> packs = getPacks();
        plugin.runAsyncTask(() -> {
            plugin.sendMessage(sender, "generate-hashes.generating");
            int changed = 0;

            for (ResourcePack pack : packs) {
                if (pack.getName().startsWith("backend-")) {
                    continue;
                }
                if (pack.getVariants().isEmpty()) {
                    if (generateHash(sender, pack, pack)) {
                        changed++;
                    }
                } else {
                    for (ResourcePack packVariant : pack.getVariants()) {
                        if (generateHash(sender, packVariant, pack)) {
                            changed++;
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

    private boolean generateHash(ResourcepacksPlayer sender, ResourcePack pack, ResourcePack packToCache) {
        boolean changed = false;
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
                packHashes.put(pack.getHash(), packToCache);
                changed = true;
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
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage()
            );
            plugin.getPluginLogger().log(Level.WARNING, "Invalid URL while trying to generate hash of pack " + pack.getName() + " from url " + pack.getUrl(), e);
        } catch (IOException e) {
            plugin.sendMessage(sender, Level.SEVERE, "generate-hashes.failed-to-load-pack",
                    "pack", pack.getName(),
                    "url", pack.getUrl(),
                    "hash", pack.getHash(),
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage()
            );
            plugin.getPluginLogger().log(Level.WARNING, "IO error while trying to generate hash of pack " + pack.getName() + " from url " + pack.getUrl(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    plugin.sendMessage(sender, Level.SEVERE, "generate-hashes.failed-to-load-pack",
                            "pack", pack.getName(),
                            "url", pack.getUrl(),
                            "hash", pack.getHash(),
                            "error", e.getClass().getSimpleName() + ": " + e.getMessage()
                    );
                    plugin.getPluginLogger().log(Level.WARNING, "Error while trying to close the input stream for pack " + pack.getName() + " hash generation", e);
                }
            }
        }
        return changed;
    }

    /**
     * Get a pack's URL. Potentially with the hash appended to work around MC-164316
     * @param pack The pack to get the URL for
     * @return The url
     */
    public String getPackUrl(ResourcePack pack) {
        if (!shouldAppendHashToUrl() || pack.getRawHash().length == 0) {
            return pack.getUrl();
        }
        return pack.getUrl() + PackManager.HASH_KEY + pack.getHash();
    }

    /**
     * Whether to append the hash to the URL of a pack or not
     * @return Whether to append the hash to the URL of a pack or not
     */
    public boolean shouldAppendHashToUrl() {
        return appendHashToUrl;
    }

    /**
     * Set whether to append the hash to the URL of a pack or not
     * @param appendHashToUrl  Whether to append the hash to the URL of a pack or not
     */
    public void setAppendHashToUrl(boolean appendHashToUrl) {
        this.appendHashToUrl = appendHashToUrl;
    }

    /**
     * Get the format of the pack a player can maximally use
     * @param version The Protocol version to get the format for
     * @return The pack format; <code>-1</code> if the player has an unknown version
     */
    public int getPackFormat(int version) {
        for (int i = 0; i + 1 < PACK_FORMATS.length; i += 2) {
            if (version >= PACK_FORMATS[i]) {
                return PACK_FORMATS[i + 1];
            }
        }
        return -1;
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

    /**
     * The result of setting a pack
     */
    public static class PackSetResult {
        private final ResourcePack pack;
        private final Status status;

        private PackSetResult(ResourcePack pack, Status status) {
            this.pack = pack;
            this.status = status;
        }

        public ResourcePack getPack() {
            return pack;
        }

        public Status getStatus() {
            return status;
        }
    }
}
