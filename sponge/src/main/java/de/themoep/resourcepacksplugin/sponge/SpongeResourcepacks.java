package de.themoep.resourcepacksplugin.sponge;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.sponge.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.sponge.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.sponge.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.sponge.listeners.ProxyPackListener;
import de.themoep.resourcepacksplugin.sponge.listeners.WorldSwitchListener;
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.core.PackAssignment;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.PluginLogger;
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
import de.themoep.utils.lang.sponge.LanguageManager;
import de.themoep.utils.lang.sponge.Languaged;
import net.kyori.adventure.platform.spongeapi.SpongeAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import org.geysermc.geyser.api.GeyserApi;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

@Plugin(id = "worldresourcepacks")
public class SpongeResourcepacks implements ResourcepacksPlugin, Languaged {

    private boolean enabled = false;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path dataFolder;

    private final SpongeAudiences adventure;

    private PluginConfig config;

    private PluginConfig storedPacks;

    private PackManager pm = new PackManager(this);

    private UserManager um;

    private LanguageManager lm;

    private Level loglevel = Level.INFO;

    private SpongePluginLogger pluginLogger = new SpongePluginLogger(logger);

    private int serverProtocolVersion = 0;

    protected ResourcepacksPluginCommandExecutor pluginCommand;

    private ViaAPI viaApi;
    private GeyserApi geyser;

    @Inject
    SpongeResourcepacks(final SpongeAudiences adventure) {
        this.adventure = adventure;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        boolean firstStart = !getDataFolder().exists();
        storedPacks = new PluginConfig(this, new File(getDataFolder(), "players.yml"));

        File serverPropertiesFile = new File("server.properties");
        if (serverPropertiesFile.exists() && serverPropertiesFile.isFile()) {
            try (FileInputStream in = new FileInputStream(serverPropertiesFile)) {
                Properties properties = new Properties();
                properties.load(in);
                String resourcePack = properties.getProperty("resource-pack");
                if (resourcePack != null && !resourcePack.isEmpty()) {
                    log(Level.WARNING, "You seem to have defined a resource-pack in your server.properties file, " +
                            "if you experience issues then please remove it and configure the pack via this plugin's config directly " +
                            "as it works better when it can completely handle the whole sending itself.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (loadConfig()) {
            Sponge.getEventManager().registerListeners(this, new DisconnectListener(this));
            Sponge.getEventManager().registerListeners(this, new WorldSwitchListener(this));

            Sponge.getChannelRegistrar().createRawChannel(this, "rp:plugin")
                    .addListener(Platform.Type.SERVER, new ProxyPackListener(this));

            registerCommand(pluginCommand = new ResourcepacksPluginCommandExecutor(this));
            registerCommand(new UsePackCommandExecutor(this));
            registerCommand(new ResetPackCommandExecutor(this));

            String versionNumberString = Sponge.getPlatform().getMinecraftVersion().getName();
            try {
                serverProtocolVersion = MinecraftVersion.parseVersion(versionNumberString).getProtocolNumber();
                logDebug("Detected server server protocol version " + serverProtocolVersion + "!");
            } catch(IllegalArgumentException e) {
                log(Level.WARNING, "Could not get version of the server! (" + versionNumberString + "/" + versionNumberString + ")");
            }

            Optional<PluginContainer> viaContainer = Sponge.getPluginManager().getPlugin("viaversion");
            if (viaContainer.isPresent()) {
                log(Level.INFO, "Detected ViaVersion " + viaApi.getVersion());
                try {
                    viaApi = Via.getAPI();
                } catch (Exception e) {
                    log(Level.SEVERE, "Could not create ViaVersion hook!", e);
                }
            }

            Optional<PluginContainer> geyserPlugin = Sponge.getPluginManager().getPlugin("geyser");
            if (geyserPlugin.isPresent()) {
                log(Level.INFO, "Detected Geyser " + geyserPlugin.get().getVersion());
                try {
                    geyser = GeyserApi.api();
                } catch (Exception e) {
                    log(Level.SEVERE, "Could not create Geyser hook!", e);
                }
            }

            if (getConfig().getBoolean("autogeneratehashes", true)) {
                getPackManager().generateHashes(null);
            }

            um = new UserManager(this);

            //if (!getConfig().getBoolean("disable-metrics", false)) {
            //    new org.bstats.MetricsLite(this);
            //}

            if (firstStart || new Random().nextDouble() < 0.01) {
                startupMessage();
            }
            enabled = true;
        } else {
            Sponge.getEventManager().unregisterPluginListeners(this);
        }
    }

    protected void registerCommand(PluginCommandExecutor executor) {
        List<String> aliases = new ArrayList<>();
        aliases.add(executor.getName());
        Collections.addAll(aliases, executor.getAliases());
        Sponge.getCommandManager().register(this, new ForwardingCommand(executor), aliases.toArray(new String[0]));
    }

    public boolean loadConfig() {
        log(Level.INFO, "Loading config!");
        config = new PluginConfig(this, new File(getDataFolder(), "config.yml"), "sponge-config.yml");

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

        String debugString = getConfig().getString("debug");
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
                    log(Level.SEVERE, "Error while loading pack " + s, e);
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
                log(Level.SEVERE, "Error while loading empty pack", e);
            }
        } else {
            String emptypackname = getConfig().getString("empty", null);
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
            logDebug("No global server assignment defined!");
        }

        if (getConfig().isSection("worlds")) {
            log(Level.INFO, "Loading world assignments...");
            ConfigurationNode worlds = getConfig().getRawConfig("worlds");
            for (Map.Entry<Object, ? extends ConfigurationNode> world : worlds.getChildrenMap().entrySet()) {
                ConfigurationNode worldSection = world.getValue();
                if (worldSection != null) {
                    log(Level.INFO, "Loading assignment for world " + world.getKey() + "...");
                    PackAssignment worldAssignment = getPackManager().loadAssignment((String) world.getKey(), getValues(worldSection));
                    getPackManager().addAssignment(worldAssignment);
                    logDebug("Loaded " + worldAssignment.toString() );
                } else {
                    log(Level.WARNING, "Config has entry for world " + world + " but it is not a configuration section?");
                }
            }
        } else {
            logDebug("No world assignments defined!");
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
            getUserManager().clearUserPacks();
            for (Player p : Sponge.getServer().getOnlinePlayers()) {
                resendPack(p);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && Sponge.getPluginManager().isLoaded("worldresourcepacks");
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
            setConfigFlat("worlds." + assignment.getName(), assignment.serialize());
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
            getConfig().set(rootKey, null);
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


    public void resendPack(UUID playerId) {
        Sponge.getServer().getPlayer(playerId).ifPresent(this::resendPack);
    }

    /**
     * Resends the pack that corresponds to the player's world
     * @param player The player to set the pack for
     */
    public void resendPack(Player player) {
        String worldName = "";
        if(player.getWorld() != null) {
            worldName = player.getWorld().getName();
        }
        getPackManager().applyPack(player.getUniqueId(), worldName);
    }

    public void setPack(UUID playerId, ResourcePack pack) {
        getPackManager().setPack(playerId, pack);
    }

    public void sendPack(UUID playerId, ResourcePack pack) {
        Sponge.getServer().getPlayer(playerId).ifPresent(p -> sendPack(p, pack));
    }

    /**
     * Set the resourcepack of a connected player
     * @param player The ProxiedPlayer to set the pack for
     * @param pack The resourcepack to set for the player
     */
    public void sendPack(Player player, ResourcePack pack) {
        player.sendResourcePack(new org.spongepowered.api.resourcepack.ResourcePack() {
            @Override
            public URI getUri() {
                return URI.create(getPackManager().getPackUrl(pack));
            }

            @Override
            public String getName() {
                return pack.getUrl().substring(pack.getUrl().lastIndexOf('/') + 1);
            }

            @Override
            public String getId() {
                return pack.getName();
            }

            @Override
            public Optional<String> getHash() {
                return Optional.of(pack.getHash());
            }
        });
        logDebug("Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
    }

    public void clearPack(UUID playerId) {
        getUserManager().clearUserPack(playerId);
    }

    public void clearPack(Player player) {
        getUserManager().clearUserPack(player.getUniqueId());
    }

    public PackManager getPackManager() {
        return pm;
    }

    public UserManager getUserManager() {
        return um;
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
            Optional<Player> player = Optional.empty();
            if (sender != null) {
                player = Sponge.getServer().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player.orElse(null));
            if (config != null) {
                return MineDown.parse(config.get(key, replacements));
            } else {
                return Component.text("Missing language config! (default language: " + lm.getDefaultLocale() + ", key: " + key + ")");
            }
        }
        return LegacyComponentSerializer.legacySection().deserialize(key);
    }

    @Override
    public boolean hasMessage(ResourcepacksPlayer sender, String key) {
        if (lm != null) {
            Optional<Player> player = Optional.empty();
            if (sender != null) {
                player = Sponge.getServer().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player.orElse(null));
            if (config != null) {
                return config.contains(key, true);
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return pluginContainer.getName();
    }

    @Override
    public String getVersion() {
        return pluginContainer.getVersion().orElse("unknown");
    }

    @Override
    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder.toFile();
    }

    @Override
    public LangLogger getLangLogger() {
        return pluginLogger;
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
        Optional<Player> player = Sponge.getServer().getPlayer(playerId);
        return player
                .map(p -> new ResourcepacksPlayer(p.getName(), p.getUniqueId()))
                .orElse(null);
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        Optional<Player> player = Sponge.getServer().getPlayer(playerName);
        return player
                .map(p -> new ResourcepacksPlayer(p.getName(), p.getUniqueId()))
                .orElse(null);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements) {
        return sendMessage(player, Level.INFO, key, replacements);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer packPlayer, Level level, String key, String... replacements) {
        Component message = getComponents(packPlayer, key, replacements);
        if (message == null) {
            return false;
        }
        if (packPlayer != null) {
            Optional<Player> player = Sponge.getServer().getPlayer(packPlayer.getUniqueId());
            if (player.isPresent()) {
                adventure.player(player.get()).sendMessage(message);
                return true;
            }
        } else {
            log(level, LegacyComponentSerializer.legacySection().serialize(message));
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
        Optional<Player> player = Sponge.getServer().getPlayer(playerId);
        return player
                .map(player1 -> player1.hasPermission(perm))
                .orElseGet(() -> perm == null);

    }

    @Override
    public int getPlayerProtocol(UUID playerId) {
        Optional<Player> player = Sponge.getServer().getPlayer(playerId);
        if (player.isPresent()) {
            int protocol = serverProtocolVersion;
            if (viaApi != null) {
                protocol = viaApi.getPlayerVersion(playerId);
            }
            return protocol;
        }
        return -1;
    }

    @Override
    public ClientType getPlayerClientType(UUID playerId) {
        if (geyser != null && geyser.isBedrockPlayer(playerId)) {
            return ClientType.BEDROCK;
        }

        return ResourcepacksPlugin.super.getPlayerClientType(playerId);
    }

    @Override
    public IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status) {
        EventContext eventContext = EventContext.builder().add(EventContextKeys.PLUGIN, pluginContainer).build();
        Optional<Player> player = Sponge.getServer().getPlayer(playerId);
        ResourcePackSelectEvent selectEvent = new ResourcePackSelectEvent(player.orElse(null), pack, status, Cause.of(eventContext, pluginContainer));
        Sponge.getEventManager().post(selectEvent);
        return selectEvent;
    }

    @Override
    public IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack) {
        EventContext eventContext = EventContext.builder().add(EventContextKeys.PLUGIN, pluginContainer).build();
        Optional<Player> player = Sponge.getServer().getPlayer(playerId);
        ResourcePackSendEvent sendEvent = new ResourcePackSendEvent(player.orElse(null), pack, Cause.of(eventContext, pluginContainer));
        Sponge.getEventManager().post(sendEvent);
        return sendEvent;
    }

    @Override
    public boolean isAuthenticated(UUID playerId) {
        return true;
    }

    @Override
    public int runTask(Runnable runnable) {
        return Sponge.getScheduler().createTaskBuilder().execute(runnable).submit(this).getName().hashCode();
    }

    @Override
    public int runAsyncTask(Runnable runnable) {
        return Sponge.getScheduler().createTaskBuilder().async().execute(runnable).submit(this).getName().hashCode();
    }

    public SpongeAudiences getAdventure() {
        return adventure;
    }

    public PluginConfig getConfig() {
        return config;
    }
}