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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.core.PluginLogger;
import de.themoep.resourcepacksplugin.velocity.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.velocity.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.velocity.integrations.FloodgateIntegration;
import de.themoep.resourcepacksplugin.velocity.integrations.GeyserIntegration;
import de.themoep.resourcepacksplugin.velocity.integrations.ViaVersionIntegration;
import de.themoep.resourcepacksplugin.velocity.listeners.PluginMessageListener;
import de.themoep.resourcepacksplugin.velocity.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.velocity.listeners.ServerSwitchListener;
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
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class VelocityResourcepacks implements ResourcepacksPlugin, Languaged {

    private final ProxyServer proxy;
    private final VelocityPluginLogger logger;
    private final File dataFolder;
    private PluginContainer ownContainer = null;

    public static final ChannelIdentifier PLUGIN_MESSAGE_CHANNEL = MinecraftChannelIdentifier.create("rp", "plugin");

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

    @Inject
    public VelocityResourcepacks(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = new VelocityPluginLogger(logger);
        this.dataFolder = dataFolder.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        boolean firstStart = !getDataFolder().exists();

        if (!loadConfig()) {
            return;
        }

        setEnabled(true);

        registerCommand(pluginCommand = new ResourcepacksPluginCommandExecutor(this));
        registerCommand(new UsePackCommandExecutor(this));
        registerCommand(new ResetPackCommandExecutor(this));

        getProxy().getPluginManager().getPlugin("ViaVersion")
                .ifPresent(c -> viaApi = new ViaVersionIntegration(this, c));

        getProxy().getPluginManager().getPlugin("geyser")
                .ifPresent(c -> geyser = new GeyserIntegration(this, c));

        getProxy().getPluginManager().getPlugin("floodgate")
                .ifPresent(c -> floodgate = new FloodgateIntegration(this, c));

        if (isEnabled() && getConfig().getBoolean("autogeneratehashes", true)) {
            getPackManager().generateHashes(null);
        }

        um = new UserManager(this);

        getProxy().getEventManager().register(this, new DisconnectListener(this));
        getProxy().getEventManager().register(this, new ServerSwitchListener(this));
        getProxy().getEventManager().register(this, new PluginMessageListener(this));
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
            log(Level.SEVERE, "Unable to load players.yml! Stored player packs will not apply!");
        }

        String debugString = getConfig().getString("debug", "true");
        if (debugString.equalsIgnoreCase("true")) {
            loglevel = Level.INFO;
        } else if (debugString.equalsIgnoreCase("false") || debugString.equalsIgnoreCase("off")) {
            loglevel = Level.FINE;
        } else {
            try {
                loglevel = Level.parse(debugString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log(Level.SEVERE, "Wrong config value for debug! To disable debugging just set it to \"false\"! (" + e.getMessage() + ")");
            }
        }
        log(Level.INFO, "Debug level: " + getLogLevel().getName());

        if (getConfig().getBoolean("useauth")) {
            log(Level.INFO, "Compatibility with backend AuthMe install ('useauth') is enabled.");
        }

        lm = new LanguageManager(this, getConfig().getString("default-language"));

        getPackManager().init();
        if (getConfig().isSection("packs")) {
            log(Level.INFO, "Loading packs:");
            ConfigurationNode packs = getConfig().getRawConfig("packs");
            for (Map.Entry<Object, ? extends ConfigurationNode> s : packs.getChildrenMap().entrySet()) {
                ConfigurationNode packSection = s.getValue();
                try {
                    ResourcePack pack = getPackManager().loadPack((String) s.getKey(), getConfigMap(packSection));
                    log(Level.INFO, pack.getName() + " - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                    ResourcePack previous = getPackManager().addPack(pack);
                    if (previous != null) {
                        log(Level.WARNING, "Multiple resource packs with name '" + previous.getName().toLowerCase() + "' found!");
                    }
                    logDebug(pack.serialize().toString());
                } catch (IllegalArgumentException e) {
                    log(Level.SEVERE, e.getMessage());
                }
            }
        } else {
            logDebug("No packs defined!");
        }

        if (getConfig().isSection("empty")) {
            ConfigurationNode packSection = getConfig().getRawConfig("empty");
            try {
                ResourcePack pack = getPackManager().loadPack(PackManager.EMPTY_IDENTIFIER, getConfigMap(packSection));
                log(Level.INFO, "Empty pack - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                getPackManager().addPack(pack);
                getPackManager().setEmptyPack(pack);
            } catch (IllegalArgumentException e) {
                log(Level.SEVERE, e.getMessage());
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
            ConfigurationNode globalSection = getConfig().getRawConfig("global");
            PackAssignment globalAssignment = getPackManager().loadAssignment("global", getValues(globalSection));
            getPackManager().setGlobalAssignment(globalAssignment);
            logDebug("Loaded " + globalAssignment.toString());
        } else {
            logDebug("No global assignment defined!");
        }

        if (getConfig().isSection("servers")) {
            log(Level.INFO, "Loading server assignments...");
            ConfigurationNode servers = getConfig().getRawConfig("servers");
            for (Map.Entry<Object, ? extends ConfigurationNode> server : servers.getChildrenMap().entrySet()) {
                ConfigurationNode serverSection = server.getValue();
                if (serverSection.isMap()) {
                    log(Level.INFO, "Loading assignment for server " + server.getKey() + "...");
                    PackAssignment serverAssignment = getPackManager().loadAssignment((String) server.getKey(), getValues(serverSection));
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
        return true;
    }

    @Override
    public Map<String, Object> getConfigMap(Object configuration) {
        if (configuration instanceof Map) {
            return (Map<String, Object>) configuration;
        } else if (configuration instanceof ConfigurationNode) {
            return getValues((ConfigurationNode) configuration);
        }
        return null;
    }

    private Map<String, Object> getValues(ConfigurationNode config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : config.getChildrenMap().entrySet()) {
            if (entry.getKey() instanceof String) {
                map.put((String) entry.getKey(), entry.getValue().getValue());
            } else {
                map.put(String.valueOf(entry.getKey()), entry.getValue().getValue());
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
        log(Level.INFO, "Reloaded config.");
        if (isEnabled() && resend) {
            log(Level.INFO, "Resending packs for all online players!");
            um = new UserManager(this);
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
        return storedPacks != null ? storedPacks.getString("players." + playerId.toString()) : null;
    }

    public ConfigurationNode getStoredPacks() {
        return storedPacks.getRawConfig("players");
    }

    @Override
    public boolean isUsepackTemporary() {
        return getConfig().getBoolean("usepack-is-temporary");
    }
    
    @Override
    public int getPermanentPackRemoveTime() {
        return getConfig().getInt("permanent-pack-remove-time");
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
        getPackManager().applyPack(player.getUniqueId(), serverName);
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
        ProtocolVersion clientVersion = player.getProtocolVersion();
        if (clientVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_8.getProtocol()) {
            player.sendResourcePack(pack.getUrl() + PackManager.HASH_KEY + pack.getHash(), pack.getRawHash());
        } else {
            log(Level.WARNING, "Cannot send the pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getUsername() + " as he uses the unsupported protocol version " + clientVersion + "!");
            log(Level.WARNING, "Consider blocking access to your server for clients with version under 1.8 if you want this plugin to work for everyone!");
        }
    }

    /**
      * <p>Send a plugin message to the server the player is connected to!</p>
      * <p>Channel: Resourcepack</p>
      * <p>sub-channel: packChange</p>
      * <p>arg1: player.getName()</p>
      * <p>arg2: pack.getName();</p>
      * <p>arg3: pack.getUrl();</p>
      * <p>arg4: pack.getHash();</p>
      * @param player The player to update the pack on the player's bukkit server
      * @param pack The ResourcePack to send the info of the the Bukkit server, null if you want to clear it!
      */
    public void sendPackInfo(Player player, ResourcePack pack) {
        if (!player.getCurrentServer().isPresent()) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        if(pack != null) {
            out.writeUTF("packChange");
            out.writeUTF(player.getUsername());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            out.writeUTF(pack.getName());
            out.writeUTF(pack.getUrl());
            out.writeUTF(pack.getHash());
        } else {
            out.writeUTF("clearPack");
            out.writeUTF(player.getUsername());
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
        }
        player.getCurrentServer().get().sendPluginMessage(PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
    }

    public void setPack(UUID playerId, ResourcePack pack) {
        getPackManager().setPack(playerId, pack);
    }

    public void sendPack(UUID playerId, ResourcePack pack) {
        getProxy().getPlayer(playerId).ifPresent(p -> sendPack(p, pack));
    }

    public void clearPack(Player player) {
        getUserManager().clearUserPack(player.getUniqueId());
        sendPackInfo(player, null);
    }

    public void clearPack(UUID playerId) {
        getUserManager().clearUserPack(playerId);
        getProxy().getPlayer(playerId).ifPresent(p -> sendPackInfo(p, null));
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
        log(getLogLevel(), "[DEBUG] " + message, throwable);
    }

    @Override
    public Level getLogLevel() {
        return loglevel;
    }

    @Override
    public ResourcepacksPlayer getPlayer(UUID playerId) {
        return getProxy().getPlayer(playerId)
                .map(player1 -> new ResourcepacksPlayer(player1.getUsername(), player1.getUniqueId()))
                .orElse(null);
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        return getProxy().getPlayer(playerName)
                .map(player1 -> new ResourcepacksPlayer(player1.getUsername(), player1.getUniqueId()))
                .orElse(null);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements) {
        return sendMessage(player, Level.INFO, key, replacements);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, Level level, String key, String... replacements) {
        Component message = getComponents(player, key, replacements);
        if (PlainComponentSerializer.plain().serialize(message).length() == 0) {
            return false;
        }
        if (player != null) {
            Optional<Player> proxyPlayer = getProxy().getPlayer(player.getUniqueId());
            if (proxyPlayer.isPresent()) {
                proxyPlayer.get().sendMessage(message);
                return true;
            }
        } else {
            log(level, PlainComponentSerializer.plain().serialize(message));
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

        return ResourcepacksPlugin.super.getPlayerClientType(playerId);
    }

    @Override
    public IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status) {
        ResourcePackSelectEvent selectEvent = new ResourcePackSelectEvent(playerId, pack, status);
        getProxy().getEventManager().fire(selectEvent);
        return selectEvent;
    }

    @Override
    public IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack) {
        ResourcePackSendEvent sendEvent = new ResourcePackSendEvent(playerId, pack);
        getProxy().getEventManager().fire(sendEvent);
        return sendEvent;
    }

    @Override
    public boolean isAuthenticated(UUID playerId) {
        return !getConfig().getBoolean("useauth") || authenticatedPlayers.contains(playerId);
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
}
