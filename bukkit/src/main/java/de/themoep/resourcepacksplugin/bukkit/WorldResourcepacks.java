package de.themoep.resourcepacksplugin.bukkit;

/*
 * ResourcepacksPlugins - bukkit
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

import com.nickuc.login.api.nLoginAPI;
import com.nickuc.openlogin.bukkit.OpenLoginBukkit;
import de.themoep.minedown.MineDown;
import de.themoep.resourcepacksplugin.bukkit.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.bukkit.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.bukkit.internal.InternalHelper;
import de.themoep.resourcepacksplugin.bukkit.internal.InternalHelper_fallback;
import de.themoep.resourcepacksplugin.bukkit.listeners.AuthmeLoginListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.NLoginListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.OpeNLoginListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.ProxyPackListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.WorldSwitchListener;
import de.themoep.resourcepacksplugin.core.ClientType;
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
import de.themoep.utils.lang.LanguageConfig;
import de.themoep.utils.lang.bukkit.LanguageManager;
import fr.xephi.authme.api.v3.AuthMeApi;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.floodgate.api.FloodgateApi;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolType;
import protocolsupport.api.ProtocolVersion;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 18.03.2015.
 */
public class WorldResourcepacks extends JavaPlugin implements ResourcepacksPlugin {

    private ConfigAccessor storedPacks;

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
            getLogger().log(level, message);
        }
    };

    private int serverProtocolVersion = 0;

    private InternalHelper internalHelper;

    protected ResourcepacksPluginCommandExecutor pluginCommand;

    private ViaAPI viaApi;
    private boolean protocolSupportApi = false;
    private GeyserConnector geyser;
    private FloodgateApi floodgate;
    private AuthMeApi authmeApi = null;
    private OpenLoginBukkit openLogin = null;
    private nLoginAPI nLogin = null;
    private ProxyPackListener proxyPackListener;

    public void onEnable() {
        boolean firstStart = !getDataFolder().exists();
        storedPacks = new ConfigAccessor(this, "players.yml");

        File serverPropertiesFile = new File("server.properties");
        if (serverPropertiesFile.exists() && serverPropertiesFile.isFile()) {
            try (FileInputStream in = new FileInputStream(serverPropertiesFile)) {
                Properties properties = new Properties();
                properties.load(in);
                String resourcePack = properties.getProperty("resource-pack");
                if (resourcePack != null && !resourcePack.isEmpty()) {
                    getLogger().log(Level.WARNING, "You seem to have defined a resource-pack in your server.properties file, " +
                            "if you experience issues then please remove it and configure the pack via this plugin's config directly " +
                            "as it works better when it can completely handle the whole sending itself.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (loadConfig()) {
            getServer().getPluginManager().registerEvents(new DisconnectListener(this), this);
            getServer().getPluginManager().registerEvents(new WorldSwitchListener(this), this);

            getServer().getMessenger().registerOutgoingPluginChannel(this, "rp:plugin");
            proxyPackListener = new ProxyPackListener(this);
            getServer().getMessenger().registerIncomingPluginChannel(this, "rp:plugin", proxyPackListener);

            registerCommand(pluginCommand = new ResourcepacksPluginCommandExecutor(this));
            registerCommand(new UsePackCommandExecutor(this));
            registerCommand(new ResetPackCommandExecutor(this));

            String versionString = getServer().getBukkitVersion();
            int minus = versionString.indexOf("-");
            String versionNumberString = versionString.substring(0, minus > -1 ? minus : versionString.length());
            try {
                serverProtocolVersion = MinecraftVersion.parseVersion(versionNumberString).getProtocolNumber();
                logDebug("Detected server server protocol version " + serverProtocolVersion + "!");
            } catch(IllegalArgumentException e) {
                getLogger().log(Level.WARNING, "Could not get version of the server! (" + versionString + "/" + versionNumberString + ")");
            }

            String packageName = getServer().getClass().getPackage().getName();
            String serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            Class<?> internalClass;
            try {
                internalClass = Class.forName(getClass().getPackage().getName() + ".internal.InternalHelper_" + serverVersion);
            } catch (Exception e) {
                internalClass = InternalHelper_fallback.class;
            }
            try {
                if(InternalHelper.class.isAssignableFrom(internalClass)) {
                    internalHelper = (InternalHelper) internalClass.getConstructor().newInstance();
                }
            } catch (Exception e) {
                internalHelper = new InternalHelper_fallback();
            }

            Plugin viaPlugin = getServer().getPluginManager().getPlugin("ViaVersion");
            if (viaPlugin != null && viaPlugin.isEnabled()) {
                viaApi = Via.getAPI();
                getLogger().log(Level.INFO, "Detected ViaVersion " + viaApi.getVersion());
            }

            Plugin protocolSupport = getServer().getPluginManager().getPlugin("ProtocolSupport");
            if (protocolSupport != null && protocolSupport.isEnabled()) {
                protocolSupportApi = true;
                getLogger().log(Level.INFO, "Detected ProtocolSupport " + protocolSupport.getDescription().getVersion());
            }

            Plugin geyserPlugin = getServer().getPluginManager().getPlugin("Geyser-Spigot");
            if (geyserPlugin != null) {
                geyser = GeyserConnector.getInstance();
                getLogger().log(Level.INFO, "Detected " + geyserPlugin.getName() + " " + geyserPlugin.getDescription().getVersion());
            }

            Plugin floodgatePlugin = getServer().getPluginManager().getPlugin("floodgate");
            if (floodgatePlugin != null) {
                floodgate = FloodgateApi.getInstance();
                getLogger().log(Level.INFO, "Detected " + floodgatePlugin.getName() + " " + floodgatePlugin.getDescription().getVersion());
            }

            if (getConfig().getBoolean("autogeneratehashes", true)) {
                getPackManager().generateHashes(null);
            }

            um = new UserManager(this);

            if (!getConfig().getBoolean("disable-metrics", false)) {
                new org.bstats.MetricsLite(this);
            }

            if (firstStart || new Random().nextDouble() < 0.01) {
                startupMessage();
            }
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    protected void registerCommand(PluginCommandExecutor executor) {
        getCommand(executor.getName()).setExecutor(new ForwardingCommand(executor));
    }

    public boolean loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        storedPacks.reloadConfig();
        getLogger().log(Level.INFO, "Loading config!");
        String debugString = getConfig().getString("debug");
        if (debugString.equalsIgnoreCase("true")) {
            loglevel = Level.INFO;
        } else if (debugString.equalsIgnoreCase("false") || debugString.equalsIgnoreCase("off")) {
            loglevel = Level.FINE;
        } else {
            try {
                loglevel = Level.parse(debugString.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Wrong config value for debug! To disable debugging just set it to \"false\"! (" + e.getMessage() + ")");
            }
        }
        getLogger().log(Level.INFO, "Debug level: " + getLogLevel().getName());

        lm = new LanguageManager(this, getConfig().getString("default-language"));

        getPackManager().init();
        if (getConfig().isSet("packs") && getConfig().isConfigurationSection("packs")) {
            logDebug("Loading packs:");
            ConfigurationSection packs = getConfig().getConfigurationSection("packs");
            for (String s : packs.getKeys(false)) {
                ConfigurationSection packSection = packs.getConfigurationSection(s);
                try {
                    ResourcePack pack = getPackManager().loadPack(s, getConfigMap(packSection));
                    getLogger().log(Level.INFO, pack.getName() + " - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                    ResourcePack previous = getPackManager().addPack(pack);
                    if (previous != null) {
                        getLogger().log(Level.WARNING, "Multiple resource packs with name '" + previous.getName().toLowerCase() + "' found!");
                    }
                    logDebug(pack.serialize().toString());

                    registerPackPermission(pack);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, e.getMessage());
                }
            }
        } else {
            logDebug("No packs defined!");
        }

        if (getConfig().isConfigurationSection("empty")) {
            ConfigurationSection packSection = getConfig().getConfigurationSection("empty");
            try {
                ResourcePack pack = getPackManager().loadPack(PackManager.EMPTY_IDENTIFIER, getConfigMap(packSection));
                getLogger().log(Level.INFO, "Empty pack - " + (pack.getVariants().isEmpty() ? (pack.getUrl() + " - " + pack.getHash()) : pack.getVariants().size() + " variants"));

                getPackManager().addPack(pack);
                getPackManager().setEmptyPack(pack);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
            }
        } else {
            String emptypackname = getConfig().getString("empty", null);
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
        String name = null;
        if (getConfig().isSet("server") && getConfig().isConfigurationSection("server")) {
            name = "server";
        } else if (getConfig().isSet("global") && getConfig().isConfigurationSection("global")) {
            name = "global";
        }
        if (name != null) {
            getLogger().log(Level.INFO, "Loading " + name + " assignment...");
            ConfigurationSection globalSection = getConfig().getConfigurationSection(name);
            PackAssignment globalAssignment = getPackManager().loadAssignment(name, getValues(globalSection));
            getPackManager().setGlobalAssignment(globalAssignment);
            logDebug("Loaded " + globalAssignment.toString());
        } else {
            logDebug("No global server assignment defined!");
        }

        if (getConfig().isSet("worlds") && getConfig().isConfigurationSection("worlds")) {
            getLogger().log(Level.INFO, "Loading world assignments...");
            ConfigurationSection worlds = getConfig().getConfigurationSection("worlds");
            for (String world : worlds.getKeys(false)) {
                ConfigurationSection worldSection = worlds.getConfigurationSection(world);
                if (worldSection != null) {
                    getLogger().log(Level.INFO, "Loading assignment for world " + world + "...");
                    PackAssignment worldAssignment = getPackManager().loadAssignment(world, getValues(worldSection));
                    getPackManager().addAssignment(worldAssignment);
                    logDebug("Loaded " + worldAssignment.toString() );
                } else {
                    getLogger().log(Level.WARNING, "Config has entry for world " + world + " but it is not a configuration section?");
                }
            }
        } else {
            logDebug("No world assignments defined!");
        }

        getPackManager().setStoredPacksOverride(getConfig().getBoolean("stored-packs-override-assignments"));
        logDebug("Stored packs override assignments: " + getPackManager().getStoredPacksOverride());

        if (getConfig().getBoolean("use-auth-plugin", getConfig().getBoolean("useauthme", true))) {
            if (getServer().getPluginManager().getPlugin("AuthMe") != null) {
                authmeApi = AuthMeApi.getInstance();
                getLogger().log(Level.INFO, "Detected AuthMe " + getServer().getPluginManager().getPlugin("AuthMe").getDescription().getVersion());
                getServer().getPluginManager().registerEvents(new AuthmeLoginListener(this), this);
            }
            if (getServer().getPluginManager().getPlugin("OpeNLogin") != null) {
                openLogin = (OpenLoginBukkit) getServer().getPluginManager().getPlugin("OpeNLogin");
                getLogger().log(Level.INFO, "Detected OpeNLogin " + openLogin.getDescription().getVersion());
                getServer().getPluginManager().registerEvents(new OpeNLoginListener(this), this);
            }
            if (getServer().getPluginManager().getPlugin("nLogin") != null) {
                nLogin = nLoginAPI.getApi();
                getLogger().log(Level.INFO, "Detected nLogin " + nLogin.getVersion());
                getServer().getPluginManager().registerEvents(new NLoginListener(this), this);
            }
        }
        return true;
    }

    private void registerPackPermission(ResourcePack pack) {
        if (getServer().getPluginManager().getPermission(pack.getPermission()) == null) {
            Permission perm = new Permission(pack.getPermission());
            perm.setDefault(PermissionDefault.OP);
            perm.setDescription("Permission for access to the resourcepack " + pack.getName() + " via the usepack command and automatic sending.");
            try {
                getServer().getPluginManager().addPermission(perm);
            } catch (IllegalArgumentException ignored) {} // Permission already registered
        }
        for (ResourcePack variant : pack.getVariants()) {
            registerPackPermission(variant);
        }
    }

    @Override
    public Map<String, Object> getConfigMap(Object configuration) {
        if (configuration instanceof Map) {
            return (Map<String, Object>) configuration;
        } else if (configuration instanceof ConfigurationSection) {
            return getValues((ConfigurationSection) configuration);
        }
        return null;
    }

    private Map<String, Object> getValues(ConfigurationSection config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys(false)) {
            if (config.get(key, null) != null) {
                if (config.isConfigurationSection(key)) {
                    map.put(key, getValues(config.getConfigurationSection(key)));
                } else {
                    map.put(key, config.get(key));
                }
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
            um = new UserManager(this);
            for(Player p : getServer().getOnlinePlayers()) {
                resendPack(p);
            }
        }
    }

    public void saveConfigChanges() {
        getConfig().set("packs", null);
        for (ResourcePack pack : getPackManager().getPacks()) {
            String path = "packs." + pack.getName();
            if (pack.equals(getPackManager().getEmptyPack()) && getConfig().isConfigurationSection("empty")) {
                path = "empty";
            }
            setConfigFlat(path, pack.serialize());
        }
        setConfigFlat(getPackManager().getGlobalAssignment().getName(), getPackManager().getGlobalAssignment().serialize());
        for (PackAssignment assignment : getPackManager().getAssignments()) {
            setConfigFlat("worlds." + assignment.getName(), assignment.serialize());
        }
        saveConfig();
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
            getConfig().set(rootKey, null);
        }
        return isEmpty;
    }

    @Override
    public void setStoredPack(UUID playerId, String packName) {
        storedPacks.getConfig().set("players." + playerId, packName);
        storedPacks.saveConfig();
    }

    @Override
    public String getStoredPack(UUID playerId) {
        return storedPacks.getConfig().getString("players." + playerId);
    }

    public ConfigurationSection getStoredPacks() {
        return storedPacks.getConfig().getConfigurationSection("players");
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
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            resendPack(player);
        }
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
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            sendPack(player, pack);
        }
    }

    /**
     * Set the resourcepack of a connected player
     * @param player The ProxiedPlayer to set the pack for
     * @param pack The resourcepack to set for the player
     */
    public void sendPack(Player player, ResourcePack pack) {
        if (pack.getRawHash().length != 0) {
            internalHelper.setResourcePack(player, pack);
        } else {
            player.setResourcePack(pack.getUrl() + PackManager.HASH_KEY + pack.getHash());
        }
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
            Player player = null;
            if (sender != null) {
                player = getServer().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player);
            if (config != null) {
                return MineDown.parse(config.get(key, replacements));
            } else {
                return TextComponent.fromLegacyText("Missing language config! (default language: " + lm.getDefaultLocale() + ", key: " + key + ")");
            }
        }
        return TextComponent.fromLegacyText(key);
    }

    @Override
    public boolean hasMessage(ResourcepacksPlayer sender, String key) {
        if (lm != null) {
            Player player = null;
            if (sender != null) {
                player = getServer().getPlayer(sender.getUniqueId());
            }
            LanguageConfig config = lm.getConfig(player);
            if (config != null) {
                return config.contains(key, true);
            }
        }
        return false;
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
        getLogger().log(getLogLevel(), "[DEBUG] " + message, throwable);
    }
    
    @Override
    public Level getLogLevel() {
        return loglevel;
    }

    @Override
    public ResourcepacksPlayer getPlayer(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            return new ResourcepacksPlayer(player.getName(), player.getUniqueId());
        }
        return null;
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        Player player = getServer().getPlayer(playerName);
        if(player != null) {
            return new ResourcepacksPlayer(player.getName(), player.getUniqueId());
        }
        return null;
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String key, String... replacements) {
        return sendMessage(player, Level.INFO, key, replacements);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer packPlayer, Level level, String key, String... replacements) {
        BaseComponent[] message = getComponents(packPlayer, key, replacements);
        if (message.length == 0) {
            return false;
        }
        if(packPlayer != null) {
            Player player = getServer().getPlayer(packPlayer.getUniqueId());
            if(player != null) {
                player.spigot().sendMessage(message);
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
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            return player.hasPermission(perm);
        }
        return perm == null;

    }

    @Override
    public int getPlayerProtocol(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if (player != null) {
            int protocol = serverProtocolVersion;
            if (viaApi != null) {
                protocol = viaApi.getPlayerVersion(playerId);
            }
            if (protocolSupportApi && protocol == serverProtocolVersion) { // if still same format test if player is using previous version
                ProtocolVersion version = ProtocolSupportAPI.getProtocolVersion(player);
                if (version.getProtocolType() == ProtocolType.PC) {
                    protocol = version.getId();
                }
            }
            return protocol;
        }
        return -1;
    }

    @Override
    public ClientType getPlayerClientType(UUID playerId) {
        if (geyser != null && geyser.getPlayerByUuid(playerId) != null) {
            return ClientType.BEDROCK;
        }

        if (floodgate != null && floodgate.getPlayer(playerId) != null) {
            return ClientType.BEDROCK;
        }

        return ResourcepacksPlugin.super.getPlayerClientType(playerId);
    }

    @Override
    public IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status) {
        ResourcePackSelectEvent selectEvent = new ResourcePackSelectEvent(playerId, pack, status);
        getServer().getPluginManager().callEvent(selectEvent);
        return selectEvent;
    }

    @Override
    public IResourcePackSendEvent callPackSendEvent(UUID playerId, ResourcePack pack) {
        ResourcePackSendEvent sendEvent = new ResourcePackSendEvent(playerId, pack);
        getServer().getPluginManager().callEvent(sendEvent);
        return sendEvent;
    }

    @Override
    public boolean isAuthenticated(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if (authmeApi != null) {
            return player != null && authmeApi.isAuthenticated(player);
        } else if (openLogin != null) {
            return player != null && openLogin.getLoginManagement().isAuthenticated(player.getName());
        } else if (nLogin != null) {
            return player != null && nLogin.isAuthenticated(player);
        }
        return true;
    }

    @Override
    public int runTask(Runnable runnable) {
        return getServer().getScheduler().runTask(this, runnable).getTaskId();
    }

    @Override
    public int runAsyncTask(Runnable runnable) {
        return getServer().getScheduler().runTaskAsynchronously(this, runnable).getTaskId();
    }

    /**
     * Get the listener that listens on the "rp:plugin" channel to register new sub channels
     * @return  The ProxyPackListener
     */
    public ProxyPackListener getProxyPackListener() {
        return proxyPackListener;
    }

    /**
     * Get the internal helper
     * @return The internal helper
     */
    protected InternalHelper getInternalHelper() {
        return internalHelper;
    }
}