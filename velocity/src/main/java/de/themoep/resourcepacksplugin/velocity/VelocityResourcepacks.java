package de.themoep.resourcepacksplugin.velocity;

/*
 * ResourcepacksPlugins - velocity
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
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

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ProxyVersion;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.core.PlatformType;
import de.themoep.resourcepacksplugin.core.PluginLogger;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import de.themoep.resourcepacksplugin.velocity.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.velocity.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.velocity.integrations.FloodgateIntegration;
import de.themoep.resourcepacksplugin.velocity.integrations.GeyserIntegration;
import de.themoep.resourcepacksplugin.velocity.integrations.ViaVersionIntegration;
import de.themoep.resourcepacksplugin.velocity.listeners.*;
import de.themoep.resourcepacksplugin.core.PackAssignment;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.UserManager;
import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.ResetPackCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.ResourcepacksPluginCommandExecutor;
import de.themoep.resourcepacksplugin.core.commands.UsePackCommandExecutor;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import de.themoep.utils.lang.LangLogger;
import de.themoep.utils.lang.LanguageConfig;
import de.themoep.utils.lang.velocity.LanguageManager;
import de.themoep.utils.lang.velocity.Languaged;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class VelocityResourcepacks implements ResourcepacksPlugin, Languaged {

    private final ProxyServer proxy;
    private final VelocityPluginLogger logger;
    private final File dataFolder;
    private PluginContainer ownContainer = null;

    private PluginConfig config;

    private PluginConfig storedPacks;
    
    private PackManager pm = new PackManager(this);

    private UserManager um;

    private LanguageManager lm;
    
    private Level loglevel = Level.INFO;

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

    private ViaVersionIntegration viaApi;
    private GeyserIntegration geyser;
    private FloodgateIntegration floodgate;
    private PluginMessageListener messageChannelHandler;
    private CurrentServerTracker serverTracker;

    @Inject
    public VelocityResourcepacks(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = new VelocityPluginLogger(logger);
        this.dataFolder = dataFolder.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("org.spongepowered.configurate.ConfigurationNode");
        } catch (ClassNotFoundException e) {
            ProxyVersion version = getProxy().getVersion();
            log(Level.SEVERE, "\nYou are running an outdated version of Velocity! Update to at least Velocity 3.3.0!\n");
            log(Level.SEVERE, getName() + " " + getVersion() + " is not compatible with " + version.getName() + " " + version.getVersion() + "!\n");
            log(Level.SEVERE, "Disabling plugin!");
            return;
        }
        boolean firstStart = !getDataFolder().exists();

        messageChannelHandler = new PluginMessageListener(this);

        if (!loadConfig()) {
            return;
        }

        setEnabled(true);

        registerCommand(pluginCommand = new ResourcepacksPluginCommandExecutor(this));
        registerCommand(new UsePackCommandExecutor(this));
        registerCommand(new ResetPackCommandExecutor(this));

        getProxy().getPluginManager().getPlugin("viaversion")
                .ifPresent(c -> {
                    try {
                        viaApi = new ViaVersionIntegration(this, c);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " hook", e);
                    }
                });

        getProxy().getPluginManager().getPlugin("geyser")
                .ifPresent(c -> {
                    try {
                        geyser = new GeyserIntegration(this, c);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " hook", e);
                    }
                });

        getProxy().getPluginManager().getPlugin("floodgate")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        floodgate = new FloodgateIntegration(this, c);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " hook", e);
                    }
                });

        getProxy().getPluginManager().getPlugin("authmevelocity")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        getProxy().getEventManager().register(this, new AuthMeVelocityListener(this));
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""), e);
                    }
                });

        getProxy().getPluginManager().getPlugin("nlogin")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        getProxy().getEventManager().register(this, new NLoginListener(this));
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""), e);
                    }
                });

        getProxy().getPluginManager().getPlugin("jpremium")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        getProxy().getEventManager().register(this, new JPremiumListener(this));
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""), e);
                    }
                });

        getProxy().getPluginManager().getPlugin("librepremium")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        new LibrePremiumListener(this, c);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " hook", e);
                    }
                });

        getProxy().getPluginManager().getPlugin("librelogin")
                .ifPresent(c -> {
                    log(Level.INFO, "Detected " + c.getDescription().getName().orElse(c.getDescription().getId()) + " " + c.getDescription().getVersion().orElse(""));
                    try {
                        new LibreLoginListener(this, c);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Could not create " + c.getDescription().getName().orElse(c.getDescription().getId()) + " hook", e);
                    }
                });

        if (isEnabled() && getConfig().getBoolean("autogeneratehashes", true)) {
            getPackManager().generateHashes(null);
        }

        um = new UserManager(this);

        getProxy().getEventManager().register(this, serverTracker = new CurrentServerTracker(this));
        getProxy().getEventManager().register(this, new ConnectListener(this));
        getProxy().getEventManager().register(this, new DisconnectListener(this));
        getProxy().getEventManager().register(this, new ServerSwitchListener(this));
        getProxy().getEventManager().register(this, messageChannelHandler);
        getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("rp", "plugin"));

        if (!getConfig().getBoolean("disable-metrics", false)) {
            // TODO: Metrics?
        }

        if (firstStart || new Random().nextDouble() < 0.01) {
            startupMessage();
        }
    }

    protected void registerCommand(PluginCommandExecutor executor) {
        getProxy().getCommandManager().register(
                getProxy().getCommandManager().metaBuilder(executor.getName()).aliases(executor.getAliases()).build(),
                new ForwardingCommand(executor)
        );
    }

    public boolean loadConfig() {
        log(Level.INFO, "Loading config!");
        config = new PluginConfig(this, new File(getDataFolder(), "config.yml"), "velocity-config.yml");

        try {
            config.createDefaultConfig();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!config.load()) {
            return false;
        }

        storedPacks = new PluginConfig(this, new File(getDataFolder(), "players.conf"), null);
        if (!storedPacks.load()) {
            log(Level.SEVERE, "Unable to load players.conf! Stored player packs will not apply!");
        }

        String debugString = getConfig().getString("debug", "true");
        if (debugString.equalsIgnoreCase("true")) {
            loglevel = Level.INFO;
        } else if (debugString.equalsIgnoreCase("false")) {
            loglevel = Level.FINE;
        } else {
            try {
                loglevel = Level.parse(debugString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log(Level.SEVERE, "Wrong config value for debug! To disable debugging just set it to \"false\"! (" + e.getMessage() + ")");
            }
        }
        log(Level.INFO, "Debug level: " + getLogLevel().getName());

        messageChannelHandler.reload();

        if (getConfig().getBoolean("use-auth-plugin", getConfig().getBoolean("useauth", false))) {
            log(Level.INFO, "Compatibility with backend authentication plugin ('use-auth-plugin') is enabled.");
        }

        lm = new LanguageManager(this, getConfig().getString("default-language"));

        getPackManager().init();
        if (getConfig().isSection("packs")) {
            log(Level.INFO, "Loading packs:");
            Map<String, Object> packs = getConfig().getSection("packs");
            for (Map.Entry<String, Object> s : packs.entrySet()) {
                Object packSection = s.getValue();
                try {
                    ResourcePack pack = getPackManager().loadPack(s.getKey(), getConfigMap(packSection));
                    log(Level.INFO, pack.getName() + " - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                    ResourcePack previous = getPackManager().addPack(pack);
                    if (previous != null) {
                        log(Level.WARNING, "Multiple resource packs with name '" + previous.getName().toLowerCase() + "' found!");
                    }
                    logDebug(pack.serialize().toString());
                } catch (IllegalArgumentException e) {
                    log(Level.SEVERE, "Error while loading pack " + s, e);
                }
            }
        } else {
            logDebug("No packs defined!");
        }

        if (getConfig().isSection("empty")) {
            Map<String, Object> packSection = getConfig().getSection("empty");
            try {
                ResourcePack pack = getPackManager().loadPack(PackManager.EMPTY_IDENTIFIER, getConfigMap(packSection));
                log(Level.INFO, "Empty pack - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                getPackManager().addPack(pack);
                getPackManager().setEmptyPack(pack);
            } catch (IllegalArgumentException e) {
                log(Level.SEVERE, "Error while loading empty pack", e);
            }
        } else {
            String emptypackname = getConfig().getString("empty");
            if (emptypackname != null && !emptypackname.isEmpty()) {
                ResourcePack ep = getPackManager().getByName(emptypackname);
                if (ep != null) {
                    log(Level.INFO, "Empty pack: " + ep.getName());
                    getPackManager().setEmptyPack(ep);
                } else {
                    log(Level.WARNING, "Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
                }
            } else {
                log(Level.WARNING, "No empty pack defined!");
            }
        }

        if (getConfig().isSection("global")) {
            log(Level.INFO, "Loading global assignment...");
            Map<String, Object> globalSection = getConfig().getSection("global");
            PackAssignment globalAssignment = getPackManager().loadAssignment("global", globalSection);
            getPackManager().setGlobalAssignment(globalAssignment);
            logDebug("Loaded " + globalAssignment.toString());
        } else {
            logDebug("No global assignment defined!");
        }

        if (getConfig().isSection("servers")) {
            log(Level.INFO, "Loading server assignments...");
            Map<String, Object> servers = getConfig().getSection("servers");
            for (Map.Entry<String, Object> server : servers.entrySet()) {
                Object serverSection = server.getValue();
                if (serverSection instanceof Map) {
                    log(Level.INFO, "Loading assignment for server " + server.getKey() + "...");
                    PackAssignment serverAssignment = getPackManager().loadAssignment(server.getKey(), (Map<String, Object>) serverSection);
                    getPackManager().addAssignment(serverAssignment);
                    logDebug("Loaded server assignment " + serverAssignment.toString());
                } else {
                    log(Level.WARNING, "Config has entry for server " + server.getKey() + " but it is not a configuration section?");
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
        return PluginConfig.getConfigMap(configuration);
    }

    /**
     * Reloads the configuration from the file and 
     * resends the resource pack to all online players 
     */
    public void reloadConfig(boolean resend) {
        loadConfig();
        log(Level.INFO, "Reloaded config.");
        if (isEnabled() && resend) {
            log(Level.INFO, "Resending packs for all online players!");
            getUserManager().clearUserPacks();
            for (Player p : getProxy().getAllPlayers()) {
                resendPack(p);
            }
        }
    }

    public void saveConfigChanges() {
        getConfig().set("packs", null);
        for (ResourcePack pack : getPackManager().getPacks()) {
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
        getConfig().save();
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
            getConfig().remove(rootKey);
        }
        return isEmpty;
    }

    @Override
    public void setStoredPack(UUID playerId, String packName) {
        if (storedPacks != null) {
            storedPacks.set("players." + playerId, packName);
            storedPacks.save();
        }
    }

    @Override
    public String getStoredPack(UUID playerId) {
        return storedPacks != null ? storedPacks.getString("players." + playerId.toString(), null) : null;
    }

    public Map<String, Object> getStoredPacks() {
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

    public PluginConfig getConfig() {
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
    public void resendPack(Player player) {
        String serverName = "";
        if(player.getCurrentServer().isPresent()) {
            serverName = player.getCurrentServer().get().getServerInfo().getName();
        }
        getPackManager().applyPack(getPlayer(player), serverName);
    }

    public void resendPack(UUID playerId) {
        getProxy().getPlayer(playerId).ifPresent(this::resendPack);
    }
    
    /**
     * Send a resourcepack to a connected player
     * @param player The Player to send the pack to
     * @param pack The resourcepack to send the pack to
     */
    protected void sendPack(Player player, ResourcePack pack) {
        int clientVersion = player.getProtocolVersion().getProtocol();
        if (clientVersion >= ProtocolVersion.MINECRAFT_1_8.getProtocol()) {
            if (clientVersion >= MinecraftVersion.MINECRAFT_1_20_3.getProtocolNumber()
                    && (pack == null || pack == getPackManager().getEmptyPack())) {
                sendPackClearRequest(player);
                return;
            }
            ResourcePackInfo.Builder packInfoBuilder = proxy.createResourcePackBuilder(getPackManager().getPackUrl(pack))
                    .setId(pack.getUuid());
            if (pack.getRawHash().length == 20) {
                packInfoBuilder.setHash(pack.getRawHash());
            } else if (pack.getRawHash().length > 0) {
                log(Level.WARNING, "Invalid sha1 hash sum for pack " + pack.getName() + " detected! (It was '" + pack.getHash() + "')");
            }
            try {
                player.sendResourcePackOffer(packInfoBuilder.build());
                logDebug("Sent pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getUsername() + ".");
            } catch (IllegalStateException e) {
                logDebug("Not sending pack " + pack.getName() + "to " + player.getUsername() + ": " + e.getMessage());
            }
        } else {
            log(Level.WARNING, "Cannot send the pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getUsername() + " as he uses the unsupported protocol version " + clientVersion + "!");
            log(Level.WARNING, "Consider blocking access to your server for clients with version under 1.8 if you want this plugin to work for everyone!");
        }
    }

    @Override
    public void sendPackInfo(UUID playerId) {
        getProxy().getPlayer(playerId).ifPresent(player -> sendPackInfo(player, getUserManager().getUserPacks(playerId)));
    }

    /**
     * <p>Send a plugin message to the server the player is connected to!</p>
     * @param player The player to update the pack on the player's bukkit server
     * @param packs The ResourcePacks to send the info of the the Bukkit server, can be empty to clear it!
     */
    private void sendPackInfo(Player player, List<ResourcePack> packs) {
        RegisteredServer server = getCurrentServer(player);
        if (server == null) {
            logDebug("Tried to send pack info of " + packs.size() + " packs for player " + player.getUsername() + " but server was null!");
            return;
        }

        if (!packs.isEmpty()) {
            getMessageChannelHandler().sendMessage(server, "packsChange", out -> {
                out.writeUTF(player.getUsername());
                out.writeLong(player.getUniqueId().getMostSignificantBits());
                out.writeLong(player.getUniqueId().getLeastSignificantBits());
                out.writeInt(packs.size());
                for (ResourcePack pack : packs) {
                    getMessageChannelHandler().writePack(out, pack);
                }
            });
        } else {
            getMessageChannelHandler().sendMessage(server, "clearPack", out -> {
                out.writeUTF(player.getUsername());
                out.writeLong(player.getUniqueId().getMostSignificantBits());
                out.writeLong(player.getUniqueId().getLeastSignificantBits());
            });
        }
    }

    /**
     * Get the server the player is currently on or connecting to
     * @param player The player
     * @return The name of the server
     */
    public RegisteredServer getCurrentServer(Player player) {
        String serverName = getCurrentServerTracker().getCurrentServer(player);
        if (serverName == null) {
            if (player.getProtocolState() != ProtocolState.CONFIGURATION) {
                logDebug("Tried to get current server for player " + player.getUsername() + " but server '" + serverName + "' doesn't exist?");
            }
            return null;
        }

        return proxy.getServer(serverName).orElse(null);
    }

    public void sendPack(UUID playerId, ResourcePack pack) {
        getProxy().getPlayer(playerId).ifPresent(p -> sendPack(p, pack));
    }

    @Override
    public void removePack(UUID playerId, ResourcePack pack) {
        getProxy().getPlayer(playerId).ifPresent(p -> removePack(p, pack));
    }

    private void removePack(Player player, ResourcePack pack) {
        if (pack.getUuid() != null) {
            sendPackRemovalRequest(player, pack);
        }
        sendPackRemoveInfo(player, pack);
    }

    private void sendPackClearRequest(Player player) {
        try {
            player.clearResourcePacks();
            logDebug("Removed all packs from " + player.getUsername());
            return;
        } catch (NoSuchMethodError ignored) {
            // Outdated Velocity, fall back to plugin message
        }
        RegisteredServer server = getCurrentServer(player);
        if (server == null) {
            logDebug("Tried to send pack clear request for player " + player.getUsername() + " but server was null!");
            return;
        }
        getMessageChannelHandler().sendMessage(server, "removePackRequest", out -> {
            out.writeUTF(player.getUsername());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            getMessageChannelHandler().writePack(out, null);
        });
        logDebug("Removed all packs from " + player.getUsername());
    }

    private void sendPackRemovalRequest(Player player, ResourcePack pack) {
        if (pack.getUuid() != null) {
            try {
                player.removeResourcePacks(pack.getUuid());
                logDebug("Removed pack " + pack.getName() + " (" + pack.getUuid() + ") from " + player.getUsername());
                return;
            } catch (NoSuchMethodError ignored) {
                // Outdated Velocity, fall back to plugin message
            }
        }
        RegisteredServer server = getCurrentServer(player);
        if (server == null) {
            logDebug("Tried to send pack removal request of pack " + pack.getName() + " for player " + player.getUsername() + " but server was null!");
            return;
        }
        getMessageChannelHandler().sendMessage(server, "removePackRequest", out -> {
            out.writeUTF(player.getUsername());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            getMessageChannelHandler().writePack(out, pack);
        });
        logDebug("Removed pack " + pack.getName() + " (" + pack.getUuid() + ") from " + player.getUsername());
    }


    private void sendPackRemoveInfo(Player player, ResourcePack pack) {
        RegisteredServer server = getCurrentServer(player);
        if (server == null) {
            logDebug("Tried to send pack removal info of pack " + pack.getName() + " for player " + player.getUsername() + " but server was null!");
            return;
        }
        getMessageChannelHandler().sendMessage(server, "removePack", out -> {
            out.writeUTF(player.getUsername());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            getMessageChannelHandler().writePack(out, pack);
        });
    }

    public void clearPack(Player player) {
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
        return LegacyComponentSerializer.legacySection().serialize(getComponents(sender, key, replacements));
    }

    /**
     * Get message components from the language config
     * @param sender The sender to get the message from, will use the client language if available
     * @param key The message key
     * @param replacements Optional placeholder replacement array
     * @return The components or an error message if not available, never null
     */
    public Component getComponents(ResourcepacksPlayer sender, String key, String... replacements) {
        if (lm != null) {
            Player player = null;
            if (sender != null) {
                player = getProxy().getPlayer(sender.getUniqueId()).orElse(null);
            }
            LanguageConfig config = lm.getConfig(player);
            if (config != null) {
                return MineDown.parse(config.get(key), replacements);
            } else {
                return Component.text("Missing language config! (default language: " + lm.getDefaultLocale() + ", key: " + key + ")");
            }
        }
        return Component.text(key);
    }

    @Override
    public boolean hasMessage(ResourcepacksPlayer sender, String key) {
        if (lm != null) {
            Player player = null;
            if (sender != null) {
                player = getProxy().getPlayer(sender.getUniqueId()).orElse(null);
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
        return getDescription().getName().orElse(getDescription().getId());
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion().orElse("Unknown");
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return logger;
    }

    @Override
    public LangLogger getLangLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    public PluginDescription getDescription() {
        if (ownContainer == null) {
            ownContainer = proxy.getPluginManager().fromInstance(this).orElse(null);
        }
        return ownContainer != null ? ownContainer.getDescription() : new PluginDescription() {
            @Override
            public String getId() {
                return getClass().getSimpleName();
            }
        };
    }

    @Override
    public void logDebug(String message) {
        logDebug(message, null);
    }

    @Override
    public void logDebug(String message, Throwable throwable) {
        if (getLogLevel() != Level.OFF) {
            log(getLogLevel(), "[DEBUG] " + message, throwable);
        }
    }

    @Override
    public Level getLogLevel() {
        return loglevel;
    }

    @Override
    public ResourcepacksPlayer getPlayer(UUID playerId) {
        return getPlayer(getProxy().getPlayer(playerId).orElse(null));
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        return getPlayer(getProxy().getPlayer(playerName).orElse(null));
    }

    public ResourcepacksPlayer getPlayer(Player player) {
        return player != null ? new ResourcepacksPlayer(player.getUsername(), player.getUniqueId()) : null;
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements) {
        return sendMessage(player, Level.INFO, key, replacements);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, Level level, String key, String... replacements) {
        Component message = getComponents(player, key, replacements);
        if (PlainTextComponentSerializer.plainText().serialize(message).isEmpty()) {
            return false;
        }
        if (player != null) {
            Optional<Player> proxyPlayer = getProxy().getPlayer(player.getUniqueId());
            if (proxyPlayer.isPresent()) {
                proxyPlayer.get().sendMessage(message);
                return true;
            }
        } else {
            log(level, PlainTextComponentSerializer.plainText().serialize(message));
        }
        return false;
    }

    @Override
    public void log(Level level, String message) {
        getPluginLogger().log(level, message);
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        getPluginLogger().log(level, message, throwable);
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
        return getProxy().getPlayer(playerId).map(p -> p.hasPermission(perm)).orElseGet(() -> perm == null);

    }

    @Override
    public int getPlayerProtocol(UUID playerId) {
        if (viaApi != null) {
            return viaApi.getPlayerVersion(playerId);
        }

        return getProxy().getPlayer(playerId).map(p -> p.getProtocolVersion().getProtocol()).orElse(-1);
    }

    @Override
    public ClientType getPlayerClientType(UUID playerId) {
        if (geyser != null && geyser.hasPlayer(playerId)) {
            return ClientType.BEDROCK;
        }

        if (floodgate != null && floodgate.hasPlayer(playerId)) {
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
        try {
            return getProxy().getEventManager().fire(selectEvent).get();
        } catch (InterruptedException | ExecutionException e) {
            getPluginLogger().log(Level.SEVERE, "Error while firing ResourcePackSelectEvent!", e);
        }
        return null;
    }

    @Override
    public IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack) {
        ResourcePackSendEvent sendEvent = new ResourcePackSendEvent(playerId, pack);
        try {
            return getProxy().getEventManager().fire(sendEvent).get();
        } catch (InterruptedException | ExecutionException e) {
            getPluginLogger().log(Level.SEVERE, "Error while firing ResourcePackSendEvent!", e);
        }
        return null;
    }

    @Override
    public boolean isAuthenticated(UUID playerId) {
        return !getConfig().getBoolean("use-auth-plugin", getConfig().getBoolean("useauth", false)) || authenticatedPlayers.contains(playerId);
    }

    @Override
    public int runTask(Runnable runnable) {
        getProxy().getScheduler().buildTask(this, runnable).schedule();
        return 0;
    }

    @Override
    public int runAsyncTask(Runnable runnable) {
        return runTask(runnable);
    }

    public void setAuthenticated(UUID playerId, boolean b) {
        if(b) {
            authenticatedPlayers.add(playerId);
        } else {
            authenticatedPlayers.remove(playerId);
        }
    }

    /**
     * Get the handler for sub channels that listens on the "rp:plugin" channel to register new sub channels
     * @return  The message channel handler
     */
    public SubChannelHandler<RegisteredServer> getMessageChannelHandler() {
        return messageChannelHandler;
    }

    /**
     * Get the tracker for getting the server a player is on or connecting to
     * @return The tracker
     */
    public CurrentServerTracker getCurrentServerTracker() {
        return serverTracker;
    }
}
