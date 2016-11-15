package de.themoep.resourcepacksplugin.bukkit;

import de.themoep.resourcepacksplugin.bukkit.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.bukkit.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.bukkit.internal.InternalHelper;
import de.themoep.resourcepacksplugin.bukkit.internal.InternalHelper_fallback;
import de.themoep.resourcepacksplugin.bukkit.listeners.AuthmeLoginListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.ProxyPackListener;
import de.themoep.resourcepacksplugin.bukkit.listeners.WorldSwitchListener;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.UserManager;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;
import us.myles.ViaVersion.api.ViaVersionAPI;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 18.03.2015.
 */
public class WorldResourcepacks extends JavaPlugin implements ResourcepacksPlugin {

    private PackManager pm;

    private UserManager um;

    private Level loglevel = Level.INFO;

    private int serverPackFormat = Integer.MAX_VALUE;

    private InternalHelper internalHelper;

    private ViaVersionAPI viaVersion;
    private NewAPI authmeApi;

    public void onEnable() {
        if (loadConfig()) {
            getServer().getPluginManager().registerEvents(new DisconnectListener(this), this);
            getServer().getPluginManager().registerEvents(new WorldSwitchListener(this), this);

            getServer().getMessenger().registerOutgoingPluginChannel(this, "Resourcepack");
            getServer().getMessenger().registerIncomingPluginChannel(this, "Resourcepack", new ProxyPackListener(this));

            getCommand(getName().toLowerCase().charAt(0) + "rp").setExecutor(new WorldResourcepacksCommand(this));
            getCommand("usepack").setExecutor(new UsePackCommand(this));

            String versionString = getServer().getBukkitVersion();
            int firstPoint = versionString.indexOf(".");
            int secondPoint = versionString.indexOf(".", firstPoint + 1);
            int minus = versionString.indexOf("-", firstPoint + 1);
            String versionNumberString = versionString.substring(firstPoint + 1, (secondPoint < minus && secondPoint != -1) ? secondPoint : minus);
            try {
                int serverVersion = Integer.valueOf(versionNumberString);
                if (serverVersion < 8) {
                    serverPackFormat = 0;
                } else if (serverVersion < 9) {
                    serverPackFormat = 1;
                } else if (serverVersion < 11) {
                    serverPackFormat = 2;
                } else {
                    serverPackFormat = 3;
                }
                getLogger().log(Level.INFO, "Detected server packformat " + serverPackFormat + "!");
            } catch(NumberFormatException e) {
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

            viaVersion = (ViaVersionAPI) getServer().getPluginManager().getPlugin("ViaVersion");
            if(viaVersion != null) {
                getLogger().log(Level.INFO, "Detected ViaVersion " + viaVersion.getVersion());
            }

            if (getConfig().getBoolean("autogeneratehashes", true)) {
                getPackManager().generateHashes(null);
            }

            um = new UserManager(this);

            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
            } catch(IOException e) {
                // metrics failed to load
            }
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public boolean loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        getLogger().log(Level.INFO, "Loading config!");
        try {
            String debugString = getConfig().getString("debug", "true");
            if (debugString.equalsIgnoreCase("true")) {
                loglevel = Level.INFO;
            } else if (debugString.equalsIgnoreCase("false")) {
                loglevel = Level.OFF;
            } else {
                try {
                    loglevel = Level.parse(debugString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Wrong config value for debug!", e);
                }
            }
        } catch(ClassCastException e) {
            loglevel = getConfig().getBoolean("debug", true) ? Level.INFO : Level.OFF;
        }
        getLogger().log(Level.INFO, "Debug level: " + getLogLevel().getName());

        pm = new PackManager(this);
        ConfigurationSection packs = getConfig().getConfigurationSection("packs");
        getLogger().log(getLogLevel(), "Loading packs:");
        for(String s : packs.getKeys(false)) {
            ConfigurationSection packSection = packs.getConfigurationSection(s);

            String packName = s.toLowerCase();
            String packUrl = packSection.getString("url", "");
            if(packUrl.isEmpty()) {
                getLogger().log(Level.SEVERE, "Pack " + packName + " does not have an url defined!");
                continue;
            }
            String packHash =  packSection.getString("hash", "");
            int packFormat = packSection.getInt("format", 0);
            boolean packRestricted = packSection.getBoolean("restricted", false);
            String packPerm = packSection.getString("permission", getName().toLowerCase() + ".pack." + packName);

            ResourcePack pack = new ResourcePack(packName, packUrl, packHash, packFormat, packRestricted, packPerm);

            getLogger().log(getLogLevel(), pack.getName() + " - " + pack.getUrl() + " - " + pack.getHash());

            try {
                getPackManager().addPack(pack);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
                continue;
            }

            if(getServer().getPluginManager().getPermission(packPerm) == null) {
                Permission perm = new Permission(packPerm);
                perm.setDefault(PermissionDefault.OP);
                perm.setDescription("Permission for access to the resourcepack " + pack.getName() + " via the usepack command.");
                getServer().getPluginManager().addPermission(perm);
            }
        }

        String emptypackname = getConfig().getString("empty", null);
        if(emptypackname != null && !emptypackname.isEmpty()) {
            ResourcePack ep = getPackManager().getByName(emptypackname);
            if(ep != null) {
                getLogger().log(getLogLevel(), "Empty pack: " + ep.getName());
                getPackManager().setEmptyPack(ep);
            } else {
                getLogger().warning("Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
            }
        }

        String globalpackname = getConfig().getString("server.pack", "");
        if(!globalpackname.isEmpty()) {
            ResourcePack gp = getPackManager().getByName(globalpackname);
            if(gp != null) {
                getLogger().log(getLogLevel(), "Server pack: " + gp.getName() + "!");
                getPackManager().setGlobalPack(gp);
            } else {
                getLogger().warning("Cannot set server resourcepack as there is no pack with the name " + globalpackname + " defined!");
            }
        }
        List<String> globalsecondary = getConfig().getStringList("server.secondary");
        if(globalsecondary != null && globalsecondary.size() > 0) {
            getLogger().log(getLogLevel(), "Server secondary packs:");
            for(String secondarypack : globalsecondary) {
                ResourcePack sp = getPackManager().getByName(secondarypack);
                if (sp != null) {
                    getPackManager().addGlobalSecondary(sp);
                    getLogger().log(getLogLevel(), sp.getName());
                } else {
                    getLogger().warning("Cannot add resourcepack as a server secondary pack as there is no pack with the name " + secondarypack + " defined!");
                }
            }
        }

        ConfigurationSection servers = getConfig().getConfigurationSection("worlds");
        for(String s : servers.getKeys(false)) {
            getLogger().log(getLogLevel(), "Loading settings for world " + s + "!");
            String packname = servers.getString(s + ".pack", "");
            if(!packname.isEmpty()) {
                ResourcePack sp = getPackManager().getByName(packname);
                if(sp != null) {
                    getPackManager().addServer(s, sp);
                    getLogger().log(getLogLevel(), "Pack: " + sp.getName() + "!");
                } else {
                    getLogger().warning("Cannot set resourcepack for " + s + " as there is no pack with the name " + packname + " defined!");
                }
            }  else {
                getLogger().log(getLogLevel(), "No pack setting for " + s + "!");
            }
            List<String> serversecondary = servers.getStringList(s + ".secondary");
            if(serversecondary != null && serversecondary.size() > 0) {
                getLogger().log(getLogLevel(), "Secondary packs:");
                for(String secondarypack : serversecondary) {
                    ResourcePack sp = getPackManager().getByName(secondarypack);
                    if (sp != null) {
                        getPackManager().addServerSecondary(s, sp);
                        getLogger().log(getLogLevel(), sp.getName());
                    } else {
                        getLogger().warning("Cannot add resourcepack as a secondary pack for world " + s + " as there is no pack with the name " + secondarypack + " defined!");
                    }
                }
            }
        }

        if(getConfig().getBoolean("useauthme", true) && getServer().getPluginManager().getPlugin("AuthMe") != null) {
            authmeApi = NewAPI.getInstance();
            getLogger().log(Level.INFO, "Detected AuthMe " + getServer().getPluginManager().getPlugin("AuthMe").getDescription().getVersion());
            LoginEvent.getHandlerList().unregister(this);
            getServer().getPluginManager().registerEvents(new AuthmeLoginListener(this), this);
        }
        return true;
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
        for (ResourcePack pack : getPackManager().getPacks()) {
            String path = "packs." + pack.getName();
            getConfig().set(path + ".url", pack.getUrl());
            getConfig().set(path + ".hash", pack.getHash());
            getConfig().set(path + ".format", pack.getFormat());
            getConfig().set(path + ".restricted", pack.isRestricted());
            getConfig().set(path + ".permission", pack.getPermission());
        }
        saveConfig();
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
        if (!pack.getHash().isEmpty()) {
            internalHelper.setResourcePack(player, pack.getUrl(), pack.getHash());
        } else {
            player.setResourcePack(pack.getUrl());
        }
        getLogger().log(getLogLevel(), "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
    }

    public void clearPack(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            clearPack(player);
        }
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

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key);
        if(msg == null || msg.isEmpty()) {
            msg = "&cUnknown message key: &6messages." + key;
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String key, Map<String, String> replacements) {
        String msg = getMessage(key);
        if (replacements != null) {
            for(Map.Entry<String, String> repl : replacements.entrySet()) {
                msg = msg.replace("%" + repl.getKey() + "%", repl.getValue());
            }
        }
        return msg;
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

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
    public boolean sendMessage(ResourcepacksPlayer player, String message) {
        return sendMessage(player, Level.INFO, message);
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer packPlayer, Level level, String message) {
        if(packPlayer != null) {
            Player player = getServer().getPlayer(packPlayer.getUniqueId());
            if(player != null) {
                player.sendMessage(message);
                return true;
            }
        } else {
            getLogger().log(level, message);
        }
        return false;
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
        return false;

    }

    @Override
    public int getPlayerPackFormat(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if(player != null) {
            if (viaVersion != null) {
                return getPackManager().getPackFormat(viaVersion.getPlayerVersion(playerId));
            }
            return serverPackFormat;
        }
        return -1;
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
        if(authmeApi == null)
            return true;
        Player player = getServer().getPlayer(playerId);
        return player != null && authmeApi.isAuthenticated(player);
    }

    @Override
    public int runAsync(Runnable runnable) {
        return getServer().getScheduler().runTaskAsynchronously(this, runnable).getTaskId();
    }
}