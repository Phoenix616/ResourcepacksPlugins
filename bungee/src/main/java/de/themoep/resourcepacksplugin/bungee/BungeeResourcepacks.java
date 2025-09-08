package de.themoep.resourcepacksplugin.bungee;

/*
 * ResourcepacksPlugins - bungee
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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import de.themoep.bungeeplugin.FileConfiguration;
import de.themoep.minedown.MineDown;
import de.themoep.resourcepacksplugin.bungee.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.bungee.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.bungee.listeners.ConnectListener;
import de.themoep.resourcepacksplugin.bungee.listeners.JPremiumListener;
import de.themoep.resourcepacksplugin.bungee.listeners.LibreLoginListener;
import de.themoep.resourcepacksplugin.bungee.listeners.LibrePremiumListener;
import de.themoep.resourcepacksplugin.bungee.listeners.NLoginListener;
import de.themoep.resourcepacksplugin.bungee.listeners.PluginMessageListener;
import de.themoep.resourcepacksplugin.bungee.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.bungee.listeners.ServerSwitchListener;
import de.themoep.resourcepacksplugin.bungee.packets.IdMapping;
import de.themoep.resourcepacksplugin.bungee.packets.ResourcePackRemovePacket;
import de.themoep.resourcepacksplugin.bungee.packets.ResourcePackSendPacket;
import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.core.PackAssignment;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.PlatformType;
import de.themoep.resourcepacksplugin.core.PluginLogger;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import de.themoep.resourcepacksplugin.core.UserManager;
import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.ResetPackCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.ResourcepacksPluginCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.UsePackCommandExecutor;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import de.themoep.utils.lang.LanguageConfig;
import de.themoep.utils.lang.bungee.LanguageManager;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.BadPacketException;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.bstats.MetricsLite;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 18.03.2015.
 */
public class BungeeResourcepacks extends Plugin implements ResourcepacksPlugin {

    private static BungeeResourcepacks instance;
    
    private FileConfiguration config;

    private FileConfiguration storedPacks;

    private FileConfiguration packetMap;
    
    private PackManager pm = new PackManager(this);

    private UserManager um;

    private LanguageManager lm;
    
    private Level loglevel = Level.INFO;

    private PluginLogger pluginLogger = new PluginLogger() {
        @Override
        public void log(Level level, String message) {
            getLogger().log(level, message);
        }

        @Override
        public void log(Level level, String message, Throwable e) {
            getLogger().log(level, message, e);
        }
    };

    protected ResourcepacksPluginCommandExecutor pluginCommand;

    /**
     * Set of uuids of players which got send a pack by the backend server. 
     * This is needed so that the server does not send the bungee pack if the user has a backend one.
     */
    private Map<UUID, Boolean> backendPackedPlayers = new ConcurrentHashMap<>();

    /**
     * Set of uuids of players which were authenticated by a backend server's plugin
     */
    private Set<UUID> authenticatedPlayers = new HashSet<>();

    /**
     * Wether the plugin is enabled or not
     */
    private boolean enabled = false;

    private int bungeeVersion = -1;

    private ViaAPI viaApi;
    private GeyserApi geyser;
    private FloodgateApi floodgate;
    private boolean appendHashToUrl;
    private PluginMessageListener messageChannelHandler;

    @Override
    public void onEnable() {
        instance = this;

        boolean firstStart = !getDataFolder().exists();

        messageChannelHandler = new PluginMessageListener(this);

        if (!loadConfig()) {
            return;
        }

        if (!registerPacket(Protocol.GAME, "TO_CLIENT", ResourcePackSendPacket.class, ResourcePackSendPacket::new)) {
            getLogger().log(Level.SEVERE, "Disabling the plugin as it can't work without the ResourcePackSendPacket!");
            return;
        }

        if (bungeeVersion >= MinecraftVersion.MINECRAFT_1_20_2.getProtocolNumber()) {
            try {
                registerPacket(Protocol.valueOf("CONFIGURATION"), "TO_CLIENT", ResourcePackSendPacket.class, ResourcePackSendPacket::new);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.WARNING, "Unable to register ResourcePackSendPacket in config phase?");
            }
        }

        if (bungeeVersion >= MinecraftVersion.MINECRAFT_1_20_3.getProtocolNumber()) {
            registerPacket(Protocol.GAME, "TO_CLIENT", ResourcePackRemovePacket.class, ResourcePackRemovePacket::new);
            registerPacket(Protocol.CONFIGURATION, "TO_CLIENT", ResourcePackRemovePacket.class, ResourcePackRemovePacket::new);
        }

        setEnabled(true);

        registerCommand(pluginCommand = new ResourcepacksPluginCommandExecutor(this));
        registerCommand(new UsePackCommandExecutor(this));
        registerCommand(new ResetPackCommandExecutor(this));

        if (getProxy().getPluginManager().getPlugin("ViaVersion") != null) {
            try {
                viaApi = Via.getAPI();
                getLogger().log(Level.INFO, "Detected ViaVersion " + viaApi.getVersion());
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create ViaVersion hook!", e);
            }
        }

        Plugin geyserPlugin = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord");
        if (geyserPlugin != null) {
            try {
                geyser = GeyserApi.api();
                getLogger().log(Level.INFO, "Detected " + geyserPlugin.getDescription().getName() + " " + geyserPlugin.getDescription().getVersion());
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create Geyser hook!", e);
            }
        }

        Plugin floodgatePlugin = getProxy().getPluginManager().getPlugin("floodgate");
        if (floodgatePlugin != null) {
            try {
                floodgate = FloodgateApi.getInstance();
                getLogger().log(Level.INFO, "Detected " + floodgatePlugin.getDescription().getName() + " " + floodgatePlugin.getDescription().getVersion());
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create floodgate hook!", e);
            }
        }

        Plugin nLoginPlugin = getProxy().getPluginManager().getPlugin("nLogin");
        if (nLoginPlugin != null) {
            getLogger().log(Level.INFO, "Detected nLogin " + nLoginPlugin.getDescription().getVersion());
            try {
                getProxy().getPluginManager().registerListener(this, new NLoginListener(this));
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create nLogin hook!", e);
            }
        }

        Plugin jPremiumPlugin = getProxy().getPluginManager().getPlugin("JPremium");
        if (jPremiumPlugin != null) {
            getLogger().log(Level.INFO, "Detected JPremium " + jPremiumPlugin.getDescription().getVersion());
            try {
                getProxy().getPluginManager().registerListener(this, new JPremiumListener(this));
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create JPremium hook!", e);
            }
        }

        Plugin librePremiumPlugin = getProxy().getPluginManager().getPlugin("LibrePremium");
        if (librePremiumPlugin != null) {
            getLogger().log(Level.INFO, "Detected LibrePremium " + librePremiumPlugin.getDescription().getVersion());
            try {
                new LibrePremiumListener(this, librePremiumPlugin);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create LibrePremium hook!", e);
            }
        }

        Plugin libreLoginPlugin = getProxy().getPluginManager().getPlugin("LibreLogin");
        if (libreLoginPlugin != null) {
            getLogger().log(Level.INFO, "Detected LibreLogin " + libreLoginPlugin.getDescription().getVersion());
            try {
                new LibreLoginListener(this, libreLoginPlugin);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Could not create LibreLogin hook!", e);
            }
        }

        if (isEnabled() && getConfig().getBoolean("autogeneratehashes", true)) {
            getPackManager().generateHashes(null);
        }

        um = new UserManager(this);

        getProxy().getPluginManager().registerListener(this, new ConnectListener(this));
        getProxy().getPluginManager().registerListener(this, new DisconnectListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerSwitchListener(this));
        getProxy().getPluginManager().registerListener(this, messageChannelHandler);
        getProxy().registerChannel(SubChannelHandler.MESSAGING_CHANNEL);

        if (!getConfig().getBoolean("disable-metrics", false)) {
            new MetricsLite(this);
        }

        if (firstStart || new Random().nextDouble() < 0.01) {
            startupMessage();
        }
    }

    protected boolean registerPacket(Protocol protocol, String directionName, Class<? extends DefinedPacket> packetClass, Supplier<? extends DefinedPacket> constructor) {
        try {
            Field directionField;
            try {
                directionField = Protocol.class.getField(directionName);
            } catch (NoSuchFieldException e) {
                directionField = Protocol.class.getDeclaredField(directionName);
                directionField.setAccessible(true);
            }
            Object direction = directionField.get(protocol);
            List<Integer> supportedVersions = new ArrayList<>();
            try {
                Field svField = Protocol.class.getField("supportedVersions");
                supportedVersions = (List<Integer>) svField.get(null);
            } catch(Exception e1) {
                // Old bungee protocol version, try new one
            }
            if (supportedVersions.size() == 0) {
                Field svIdField = ProtocolConstants.class.getField("SUPPORTED_VERSION_IDS");
                supportedVersions = (List<Integer>) svIdField.get(null);
            }

            List<IdMapping> idMappings = getIdMappings(protocol, packetClass);
            if (idMappings.isEmpty()) {
                getLogger().log(Level.SEVERE, "No mappings set in packetmap.yml for " + protocol + " " + packetClass.getSimpleName() + "!");
                return false;
            }

            logDebug("Registering " + packetClass.getSimpleName() + " in " + protocol + " phase...");
            if (bungeeVersion == -1) {
                for (int i = supportedVersions.size() - 1; i > -1; i--) {
                    bungeeVersion = supportedVersions.get(i);
                    if (bungeeVersion < 0x4000000) {
                        break;
                    }
                }
            }
            if (bungeeVersion == ProtocolConstants.MINECRAFT_1_8) {
                logDebug("BungeeCord 1.8 (" + bungeeVersion + ") detected!");
                Method reg = direction.getClass().getDeclaredMethod("registerPacket", int.class, Class.class);
                reg.setAccessible(true);
                int id = -1;
                for (IdMapping mapping : idMappings) {
                    if (mapping.getProtocolVersion() == ProtocolConstants.MINECRAFT_1_8) {
                        id = mapping.getPacketId();
                        break;
                    }
                }
                if (id == -1) {
                    getLogger().log(Level.SEVERE, packetClass.getSimpleName() + " does not contain an ID for 1.8!");
                    return false;
                }
                reg.invoke(direction, id, packetClass);
            } else if (bungeeVersion >= ProtocolConstants.MINECRAFT_1_9 && bungeeVersion < ProtocolConstants.MINECRAFT_1_9_4) {
                logDebug("BungeeCord 1.9-1.9.3 (" + bungeeVersion + ") detected!");
                Method reg = direction.getClass().getDeclaredMethod("registerPacket", int.class, int.class, Class.class);
                reg.setAccessible(true);
                int id18 = -1;
                int id19 = -1;
                for (IdMapping mapping : idMappings) {
                    if (mapping.getProtocolVersion() == ProtocolConstants.MINECRAFT_1_8) {
                        id18 = mapping.getPacketId();
                    } else if (mapping.getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_9 && mapping.getProtocolVersion() < ProtocolConstants.MINECRAFT_1_9_4) {
                        id19 = mapping.getPacketId();
                    }
                }
                if (id18 == -1 || id19 == -1) {
                    getLogger().log(Level.SEVERE, packetClass.getSimpleName() + " does not contain an ID for 1.8 or 1.9!");
                    return false;
                }
                reg.invoke(direction, id18, id19, packetClass);
            } else if (bungeeVersion >= ProtocolConstants.MINECRAFT_1_9_4) {
                logDebug("BungeeCord 1.9.4+ (" + bungeeVersion + ") detected!");
                Method map = Protocol.class.getDeclaredMethod("map", int.class, int.class);
                map.setAccessible(true);
                Map<String, Object> mappings = new LinkedHashMap<>();

                ArrayDeque<IdMapping> additionalMappings = new ArrayDeque<>();
                Set<Integer> registeredVersions = new HashSet<>();
                for (IdMapping mapping : idMappings) {
                    if (ProtocolConstants.SUPPORTED_VERSION_IDS.contains(mapping.getProtocolVersion())) {
                        mappings.put(mapping.getName(), map.invoke(null, mapping.getProtocolVersion(), mapping.getPacketId()));
                        registeredVersions.add(mapping.getProtocolVersion());
                    } else {
                        additionalMappings.addFirst(mapping);
                    }
                }

                // Check if we have a supported version after the additional mapping's id
                // This allows specifying the snapshot version an ID was first used
                for (IdMapping mapping : additionalMappings) {
                    for (int id : ProtocolConstants.SUPPORTED_VERSION_IDS) {
                        if (id < 0x4000000 && !registeredVersions.contains(id) && id > mapping.getProtocolVersion()) {
                            logDebug("Using unregistered mapping " + mapping.getName() + "/" + mapping.getProtocolVersion() + " for unregistered version " + id);
                            mappings.put(mapping.getName(), map.invoke(null, id, mapping.getPacketId()));
                            registeredVersions.add(id);
                            break;
                        }
                    }
                }

                Object mappingsObject = Array.newInstance(mappings.values().iterator().next().getClass(), mappings.size());
                int i = 0;
                for (Iterator<Map.Entry<String, Object>> it = mappings.entrySet().iterator(); it.hasNext() ; i++) {
                    Map.Entry<String, Object> entry = it.next();
                    Array.set(mappingsObject, i, entry.getValue());
                    logDebug("Found mapping for " + entry.getKey() + "+ " + protocol + " " + entry.getValue());
                }
                Object[] mappingsArray = (Object[]) mappingsObject;
                try {
                    Method reg = direction.getClass().getDeclaredMethod("registerPacket", Class.class, Supplier.class, mappingsArray.getClass());
                    reg.setAccessible(true);
                    try {
                        reg.invoke(direction, packetClass, constructor, mappingsArray);
                    } catch (Throwable t) {
                        getLogger().log(Level.SEVERE, "Protocol version " + bungeeVersion + " is not supported! Please look for an update!", t);
                        return false;
                    }
                } catch (NoSuchMethodException e) {
                    // Old pre build 1580
                    Method reg = direction.getClass().getDeclaredMethod("registerPacket", Class.class, mappingsArray.getClass());
                    reg.setAccessible(true);
                    try {
                        reg.invoke(direction, packetClass, mappingsArray);
                    } catch (Throwable t) {
                        getLogger().log(Level.SEVERE, "Protocol version " + bungeeVersion + " is not supported! Please look for an update!", t);
                        return false;
                    }
                }
            } else {
                getLogger().log(Level.SEVERE, "Unsupported BungeeCord version (" + bungeeVersion + ") found! You need at least 1.8 for this plugin to work!");
                return false;
            }
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            getLogger().log(Level.SEVERE, "Couldn't find a required method! Please update this plugin or downgrade BungeeCord!", e);
        } catch (NoSuchFieldException e) {
            getLogger().log(Level.SEVERE, "Couldn't find a required field! Please update this plugin or downgrade BungeeCord!", e);
        }
        return false;
    }

    private List<IdMapping> getIdMappings(Protocol protocol, Class<? extends DefinedPacket> packetClass) {
        Configuration directionConfig = packetMap.getConfiguration();
        if (protocol != null) {
            directionConfig = packetMap.getSection(protocol.name());
        }
        Configuration packetConfig = directionConfig.getSection(packetClass.getSimpleName());
        if (packetConfig.getKeys().isEmpty()) {
            packetConfig = directionConfig.getSection(packetClass.getSimpleName().toLowerCase(Locale.ROOT));
        }
        Map<Integer, IdMapping> protocolVersionMap = new TreeMap<>();

        for (MinecraftVersion value : MinecraftVersion.values()) {
            Object defaultId = packetConfig.getDefault(value.toConfigString().replace('.', '_'));
            int packetId;
            if (defaultId instanceof Integer) {
                packetId = (int) defaultId;
            } else {
                packetId = packetConfig.getInt(value.toConfigString().replace('.', '_'));
            }
            if (packetId > 0) {
                protocolVersionMap.put(value.getProtocolNumber(), new IdMapping(value.toConfigString(), value.getProtocolNumber(), packetId));
            }
        }

        for (String key : packetConfig.getKeys()) {
            int protocolId = -1;
            try {
                protocolId = MinecraftVersion.parseVersion(key).getProtocolNumber();
            } catch (IllegalArgumentException e1) {
                try {
                    protocolId = Integer.parseInt(key);
                } catch (NumberFormatException e2) {
                    getLogger().log(Level.WARNING, key + " in packetmap.yml for " + packetClass.getSimpleName() + " is not a known version!");
                }
            }
            if (protocolId > 0) {
                Object defaultId = packetConfig.getDefault(key);
                int packetId;
                if (defaultId instanceof Integer) {
                    packetId = (int) defaultId;
                } else {
                    packetId = packetConfig.getInt(key);
                }
                if (packetId > 0) {
                    protocolVersionMap.put(protocolId, new IdMapping(key.replace('_', '.'), protocolId, packetId));
                }
            }
        }

        // legacy config support
        if (protocol != null && protocolVersionMap.isEmpty()) {
            return getIdMappings(null, packetClass);
        }

        return new ArrayList<>(protocolVersionMap.values());
    }

    protected void registerCommand(PluginCommandExecutor executor) {
        getProxy().getPluginManager().registerCommand(this, new ForwardingCommand(executor));
    }

    public boolean loadConfig() {
        try {
            config = new FileConfiguration(this, new File(getDataFolder(), "config.yml"), "bungee-config.yml");
            getLogger().log(Level.INFO, "Loading config!");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration! " + getDescription().getName() + " will not be enabled!", e);
            return false;
        }

        try {
            storedPacks = new FileConfiguration(this, new File(getDataFolder(), "players.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load players.yml! Stored player packs will not work!", e);
        }

        try {
            packetMap = new FileConfiguration(this, new File(getDataFolder(), "packetmap.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load packetmap.yml! The plugin will not work!", e);
            return false;
        }

        String debugString = getConfig().getString("debug");
        if (debugString.equalsIgnoreCase("true")) {
            loglevel = Level.INFO;
        } else if (debugString.equalsIgnoreCase("false")) {
            loglevel = Level.FINE;
        } else {
            try {
                loglevel = Level.parse(debugString.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Wrong config value for debug! To disable debugging just set it to \"false\"! (" + e.getMessage() + ")");
            }
        }
        getLogger().log(Level.INFO, "Debug level: " + getLogLevel().getName());

        messageChannelHandler.reload();

        if (getConfig().getBoolean("use-auth-plugin", getConfig().getBoolean("useauth", false))) {
            getLogger().log(Level.INFO, "Compatibility with backend authentication plugin ('use-auth-plugin') is enabled.");
        }

        lm = new LanguageManager(this, getConfig().getString("default-language"));

        getPackManager().init();
        if (getConfig().isSet("packs", true) && getConfig().isSection("packs")) {
            getLogger().log(Level.INFO, "Loading packs:");
            Configuration packs = getConfig().getSection("packs");
            for (String s : packs.getKeys()) {
                if (packs.get(s) instanceof Configuration) {
                    Configuration packSection = packs.getSection(s);
                    try {
                        ResourcePack pack = getPackManager().loadPack(s, getConfigMap(packSection));
                        getLogger().log(Level.INFO, pack.getName() + " - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                        ResourcePack previous = getPackManager().addPack(pack);
                        if (previous != null) {
                            getLogger().log(Level.WARNING, "Multiple resource packs with name '" + previous.getName().toLowerCase() + "' found!");
                        }
                        logDebug(pack.serialize().toString());
                    } catch (IllegalArgumentException e) {
                        getLogger().log(Level.SEVERE, "Error while loading pack " + s, e);
                    }
                }
            }
        } else {
            logDebug("No packs defined!");
        }

        if (getConfig().isSection("empty")) {
            Configuration packSection = getConfig().getSection("empty");
            try {
                ResourcePack pack = getPackManager().loadPack(PackManager.EMPTY_IDENTIFIER, getConfigMap(packSection));
                getLogger().log(Level.INFO, "Empty pack - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                getPackManager().addPack(pack);
                getPackManager().setEmptyPack(pack);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Error while loading empty pack", e);
            }
        } else {
            String emptypackname = getConfig().getString("empty");
            if (emptypackname != null && !emptypackname.isEmpty()) {
                ResourcePack ep = getPackManager().getByName(emptypackname);
                if (ep != null) {
                    getLogger().log(Level.INFO, "Empty pack: " + ep.getName());
                    getPackManager().setEmptyPack(ep);
                } else {
                    getLogger().log(Level.WARNING, "Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
                }
            } else {
                getLogger().log(Level.WARNING, "No empty pack defined!");
            }
        }

        if (getConfig().isSet("global", true) && getConfig().isSection("global")) {
            getLogger().log(Level.INFO, "Loading global assignment...");
            Configuration globalSection = getConfig().getSection("global");
            PackAssignment globalAssignment = getPackManager().loadAssignment("global", getValues(globalSection));
            getPackManager().setGlobalAssignment(globalAssignment);
            logDebug("Loaded " + globalAssignment.toString());
        } else {
            logDebug("No global assignment defined!");
        }

        if (getConfig().isSet("servers", true) && getConfig().isSection("servers")) {
            getLogger().log(Level.INFO, "Loading server assignments...");
            Configuration servers = getConfig().getSection("servers");
            for (String server : servers.getKeys()) {
                if (servers.get(server) instanceof Configuration) {
                    Configuration serverSection = servers.getSection(server);
                    if (!serverSection.getKeys().isEmpty()) {
                        getLogger().log(Level.INFO, "Loading assignment for server " + server + "...");
                        PackAssignment serverAssignment = getPackManager().loadAssignment(server, getValues(serverSection));
                        getPackManager().addAssignment(serverAssignment);
                        logDebug("Loaded server assignment " + serverAssignment.toString());
                    } else {
                        getLogger().log(Level.WARNING, "Config has entry for server " + server + " but it is not a configuration section?");
                    }
                }
            }
        } else {
            logDebug("No server assignments defined!");
        }

        getPackManager().setStoredPacksOverride(getConfig().getBoolean("stored-packs-override-assignments"));
        logDebug("Stored packs override assignments: " + getPackManager().getStoredPacksOverride());

        getPackManager().setAppendHashToUrl(getConfig().getBoolean("append-hash-to-url"));
        logDebug("Append hash to pack URL: " + getPackManager().shouldAppendHashToUrl());
        return true;
    }

    @Override
    public Map<String, Object> getConfigMap(Object configuration) {
        if (configuration instanceof Map) {
            return (Map<String, Object>) configuration;
        } else if (configuration instanceof Configuration) {
            return getValues((Configuration) configuration);
        }
        return null;
    }

    private Map<String, Object> getValues(Configuration config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys()) {
            Object value = config.get(key);
            if (value instanceof Configuration) {
                map.put(key, getValues((Configuration) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Reloads the configuration from the file and 
     * resends the resource pack to all online players 
     */
    public void reloadConfig(boolean resend) {
        loadConfig();
        getLogger().log(Level.INFO, "Reloaded config.");
        if(isEnabled() && resend) {
            getLogger().log(Level.INFO, "Resending packs for all online players!");
            getUserManager().clearUserPacks();
            for (ProxiedPlayer p : getProxy().getPlayers()) {
                resendPack(p);
            }
        }
    }

    public void saveConfigChanges() {
        getConfig().set("packs", null);
        for (ResourcePack pack : getPackManager().getPacks()) {
            if (pack.getName().startsWith("backend-")) {
                continue;
            }
            String path = "packs." + pack.getName();
            if (pack.equals(getPackManager().getEmptyPack()) && getConfig().isSection("empty")) {
                path = "empty";
            }
            setConfigFlat(path, pack.serialize());
        }
        setConfigFlat(getPackManager().getGlobalAssignment().getName(), getPackManager().getGlobalAssignment().serialize());
        for (PackAssignment assignment : getPackManager().getAssignments()) {
            setConfigFlat("servers." + assignment.getName(), assignment.serialize());
        }
        getConfig().saveConfig();
    }

    private boolean setConfigFlat(String rootKey, Map<String, Object> map) {
        boolean isEmpty = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                isEmpty &= setConfigFlat(rootKey + "." + entry.getKey(), (Map<String, Object>) entry.getValue());
            } else {
                // Remove empty map lists
                if (entry.getValue() instanceof List) {
                    ((List<?>) entry.getValue()).removeIf(e -> {
                        if (e instanceof Map) {
                            ((Map<?, ?>) e).entrySet().removeIf(le -> le.getValue() == null);
                            return ((Map<?, ?>) e).isEmpty();
                        }
                        return false;
                    });
                }
                getConfig().set(rootKey + "." + entry.getKey(), entry.getValue());
                if (entry.getValue() != null && (!(entry.getValue() instanceof Collection) || !((Collection) entry.getValue()).isEmpty())) {
                    isEmpty = false;
                }
            }
        }
        if (isEmpty) {
            getConfig().set(rootKey, null);
        }
        return isEmpty;
    }

    @Override
    public void setStoredPack(UUID playerId, String packName) {
        if (storedPacks != null) {
            storedPacks.set("players." + playerId, packName);
            storedPacks.saveConfig();
        }
    }

    @Override
    public String getStoredPack(UUID playerId) {
        return storedPacks != null ? storedPacks.getString("players." + playerId.toString(), null) : null;
    }

    public Configuration getStoredPacks() {
        return storedPacks.getSection("players");
    }

    @Override
    public boolean isUsepackTemporary() {
        return getConfig().getBoolean("usepack-is-temporary");
    }
    
    @Override
    public int getPermanentPackRemoveTime() {
        return getConfig().getInt("permanent-pack-remove-time");
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.PROXY;
    }

    public static BungeeResourcepacks getInstance() {
        return instance;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Get whether the plugin successful enabled or not
     * @return Whether or not the plugin was enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if the plugin is enabled or not
     * @param enabled Set whether or not the plugin is enabled
     */
    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Resends the pack that corresponds to the player's server
     * @param player The player to set the pack for
     */
    public void resendPack(ProxiedPlayer player) {
        String serverName = "";
        if(player.getServer() != null) {
            serverName = player.getServer().getInfo().getName();
        }
        getPackManager().applyPack(getPlayer(player), serverName);
    }

    public void resendPack(UUID playerId) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            resendPack(player);
        }
    }
    
    /**
     * Send a resourcepack to a connected player
     * @param player The ProxiedPlayer to send the pack to
     * @param pack The resourcepack to send the pack to
     */
    protected void sendPack(ProxiedPlayer player, ResourcePack pack) {
        int clientVersion = player.getPendingConnection().getVersion();
        if (clientVersion >= MinecraftVersion.MINECRAFT_1_20_3.getProtocolNumber()
                && (pack == null || pack == getPackManager().getEmptyPack())) {
            ResourcePackRemovePacket packet = new ResourcePackRemovePacket();
            try {
                ((UserConnection) player).sendPacketQueued(packet);
            } catch (Throwable t) {
                player.unsafe().sendPacket(packet);
            }
            logDebug("Cleared all packs of " + player.getName());
        } else if (clientVersion >= MinecraftVersion.MINECRAFT_1_8.getProtocolNumber()) {
            try {
                ResourcePackSendPacket packet = new ResourcePackSendPacket(pack.getUuid(), getPackManager().getPackUrl(pack), pack.getHash());
                try {
                    ((UserConnection) player).sendPacketQueued(packet);
                } catch (Throwable t) {
                    player.unsafe().sendPacket(packet);
                }
                logDebug("Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
            } catch(BadPacketException e) {
                getLogger().log(Level.SEVERE, e.getMessage() + " Please check for updates!");
            } catch(ClassCastException e) {
                getLogger().log(Level.SEVERE, "Packet defined was not ResourcePackSendPacket? Please check for updates!");
            }
        } else {
            getLogger().log(Level.WARNING, "Cannot send the pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName() + " as he uses the unsupported protocol version " + clientVersion + "!");
            getLogger().log(Level.WARNING, "Consider blocking access to your server for clients with version under 1.8 if you want this plugin to work for everyone!");
        }
    }

    @Override
    public void sendPackInfo(UUID playerId) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if (player != null) {
            sendPackInfo(player, getUserManager().getUserPacks(playerId));
        }
    }

    /**
      * <p>Send a plugin message to the server the player is connected to!</p>
      * @param player The player to update the pack on the player's bukkit server
      * @param packs The ResourcePacks to send the info of the Bukkit server, can be empty to clear it!
      */
    private void sendPackInfo(ProxiedPlayer player, List<ResourcePack> packs) {
        if (player.getServer() == null) {
            logDebug("Tried to send pack info of " + packs.size() + " packs for player " + player.getName() + " but server was null!");
            return;
        }
        if (!packs.isEmpty()) {
            getMessageChannelHandler().sendMessage(player.getServer(), "packsChange", out -> {
                out.writeUTF(player.getName());
                out.writeLong(player.getUniqueId().getMostSignificantBits());
                out.writeLong(player.getUniqueId().getLeastSignificantBits());
                out.writeInt(packs.size());
                for (ResourcePack pack : packs) {
                    out.writeUTF(pack.getName());
                    out.writeUTF(pack.getUrl());
                    out.writeUTF(pack.getHash());
                    out.writeLong(pack.getUuid().getMostSignificantBits());
                    out.writeLong(pack.getUuid().getLeastSignificantBits());
                }
            });
        } else {
            getMessageChannelHandler().sendMessage(player.getServer(), "clearPack", out -> {
                out.writeUTF(player.getName());
                out.writeLong(player.getUniqueId().getMostSignificantBits());
                out.writeLong(player.getUniqueId().getLeastSignificantBits());
            });
        }
    }

    public void sendPack(UUID playerId, ResourcePack pack) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            sendPack(player, pack);
        }
    }

    @Override
    public void removePack(UUID playerId, ResourcePack pack) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if (player != null) {
            removePack(player, pack);
        }
    }

    private void removePack(ProxiedPlayer player, ResourcePack pack) {
        if (pack.getUuid() != null) {
            ResourcePackRemovePacket packet = new ResourcePackRemovePacket(pack.getUuid());
            try {
                ((UserConnection) player).sendPacketQueued(packet);
            } catch (Throwable t) {
                player.unsafe().sendPacket(packet);
            }
            logDebug("Removed pack " + pack.getName() + " (" + pack.getUuid() + ") from " + player.getName());
        }
        sendPackRemoveInfo(player, pack);
    }

    private void sendPackRemoveInfo(ProxiedPlayer player, ResourcePack pack) {
        if (player.getServer() == null) {
            logDebug("Tried to send pack removal info of pack " + pack.getName() + " for player " + player.getName() + " but server was null!");
            return;
        }
        getMessageChannelHandler().sendMessage(player.getServer(), "removePack", out -> {
            out.writeUTF(player.getName());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            getMessageChannelHandler().writePack(out, pack);
        });
    }

    public void clearPack(ProxiedPlayer player) {
        getUserManager().clearUserPacks(player.getUniqueId());
    }

    public void clearPack(UUID playerId) {
        getUserManager().clearUserPacks(playerId);
    }

    public PackManager getPackManager() {
        return pm;
    }

    public UserManager getUserManager() {
        return um;
    }

    /**
     * Add a player's UUID to the list of players with a backend pack
     * @param playerId The uuid of the player
     */
    public void setBackend(UUID playerId) {
        backendPackedPlayers.put(playerId, false);
    }

    /**
     * Remove a player's UUID from the list of players with a backend pack
     * @param playerId The uuid of the player
     */
    public void unsetBackend(UUID playerId) {
        backendPackedPlayers.remove(playerId);
    }

    /**
     * Check if a player has a pack set by a backend server
     * @param playerId The UUID of the player
     * @return If the player has a backend pack
     */
    public boolean hasBackend(UUID playerId) {
        return backendPackedPlayers.containsKey(playerId);
    }

    @Override
    public String getMessage(ResourcepacksPlayer sender, String key, String... replacements) {
        return TextComponent.toLegacyText(getComponents(sender, key, replacements));
    }

    /**
     * Get message components from the language config
     * @param sender The sender to get the message from, will use the client language if available
     * @param key The message key
     * @param replacements Optional placeholder replacement array
     * @return The components or an error message if not available, never null
     */
    public BaseComponent[] getComponents(ResourcepacksPlayer sender, String key, String... replacements) {
        if (lm != null) {
            ProxiedPlayer player = null;
            if (sender != null) {
                player = getProxy().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player);
            if (config != null) {
                return MineDown.parse(config.get(key), replacements);
            } else {
                return TextComponent.fromLegacyText("Missing language config! (default language: " + lm.getDefaultLocale() + ", key: " + key + ")");
            }
        }
        return TextComponent.fromLegacyText(key);
    }

    @Override
    public boolean hasMessage(ResourcepacksPlayer sender, String key) {
        if (lm != null) {
            ProxiedPlayer player = null;
            if (sender != null) {
                player = getProxy().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player);
            if (config != null) {
                return config.contains(key, true);
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return getDescription().getName();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    @Override
    public void logDebug(String message) {
        logDebug(message, null);
    }

    @Override
    public void logDebug(String message, Throwable throwable) {
        if (getLogLevel() != Level.OFF) {
            getLogger().log(getLogLevel(), "[DEBUG] " + message, throwable);
        }
    }

    @Override
    public Level getLogLevel() {
        return loglevel;
    }

    @Override
    public ResourcepacksPlayer getPlayer(UUID playerId) {
        return getPlayer(getProxy().getPlayer(playerId));
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        return getPlayer(getProxy().getPlayer(playerName));
    }

    public ResourcepacksPlayer getPlayer(ProxiedPlayer player) {
        return player != null ? new ResourcepacksPlayer(player.getName(), player.getUniqueId()) : null;
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements) {
        return sendMessage(player, Level.INFO, key, replacements);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, Level level, String key, String... replacements) {
        BaseComponent[] message = getComponents(player, key, replacements);
        if (message.length == 0) {
            return false;
        }
        if(player != null) {
            ProxiedPlayer proxyPlayer = getProxy().getPlayer(player.getUniqueId());
            if(proxyPlayer != null) {
                proxyPlayer.sendMessage(message);
                return true;
            }
        } else {
            log(level, TextComponent.toLegacyText(message));
        }
        return false;
    }

    @Override
    public void log(Level level, String message) {
        getLogger().log(level, ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        getLogger().log(level, ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)), throwable);
    }

    @Override
     public boolean checkPermission(ResourcepacksPlayer player, String perm) {
        // Console
        if(player == null)
            return true;
        return checkPermission(player.getUniqueId(), perm);

    }

    @Override
    public boolean checkPermission(UUID playerId, String perm) {
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(playerId);
        if(proxiedPlayer != null) {
            return proxiedPlayer.hasPermission(perm);
        }
        return perm == null;

    }

    @Override
    public int getPlayerProtocol(UUID playerId) {
        if (viaApi != null) {
            return viaApi.getPlayerVersion(playerId);
        }

        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(playerId);
        if (proxiedPlayer != null) {
            return proxiedPlayer.getPendingConnection().getVersion();
        }
        return -1;
    }

    @Override
    public ClientType getPlayerClientType(UUID playerId) {
        if (geyser != null && geyser.isBedrockPlayer(playerId)) {
            return ClientType.BEDROCK;
        }

        if (floodgate != null && floodgate.isFloodgatePlayer(playerId)) {
            return ClientType.BEDROCK;
        }

        if (geyser != null || floodgate != null) {
            return ClientType.ORIGINAL;
        }

        return ResourcepacksPlugin.super.getPlayerClientType(playerId);
    }

    @Override
    public IResourcePackSelectEvent callPackSelectEvent(UUID playerId, List<ResourcePack> packs, IResourcePackSelectEvent.Status status) {
        ResourcePackSelectEvent selectEvent = new ResourcePackSelectEvent(playerId, packs, status);
        getProxy().getPluginManager().callEvent(selectEvent);
        return selectEvent;
    }

    @Override
    public IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack) {
        ResourcePackSendEvent sendEvent = new ResourcePackSendEvent(playerId, pack);
        getProxy().getPluginManager().callEvent(sendEvent);
        return sendEvent;
    }

    @Override
    public boolean isAuthenticated(UUID playerId) {
        return !getConfig().getBoolean("use-auth-plugin", getConfig().getBoolean("useauth", false)) || authenticatedPlayers.contains(playerId);
    }

    @Override
    public int runTask(Runnable runnable) {
        return getProxy().getScheduler().schedule(this, runnable, 0, TimeUnit.MICROSECONDS).getId();
    }

    @Override
    public int runAsyncTask(Runnable runnable) {
        return getProxy().getScheduler().runAsync(this, runnable).getId();
    }

    public void setAuthenticated(UUID playerId, boolean b) {
        if(b) {
            authenticatedPlayers.add(playerId);
        } else {
            authenticatedPlayers.remove(playerId);
        }
    }

    public int getBungeeVersion() {
        return bungeeVersion;
    }

    /**
     * Get the handler for sub channels that listens on the "rp:plugin" channel to register new sub channels
     * @return  The message channel handler
     */
    public SubChannelHandler<Server> getMessageChannelHandler() {
        return messageChannelHandler;
    }
}
