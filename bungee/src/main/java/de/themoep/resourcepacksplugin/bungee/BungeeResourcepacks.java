package de.themoep.resourcepacksplugin.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bungee.events.ResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.bungee.events.ResourcePackSendEvent;
import de.themoep.resourcepacksplugin.bungee.listeners.PluginMessageListener;
import de.themoep.resourcepacksplugin.bungee.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.bungee.listeners.ServerSwitchListener;
import de.themoep.resourcepacksplugin.bungee.packets.ResourcePackSendPacket;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.BadPacketException;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.minecrell.mcstats.BungeeStatsLite;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 18.03.2015.
 */
public class BungeeResourcepacks extends Plugin implements ResourcepacksPlugin {

    private static BungeeResourcepacks instance;
    
    private YamlConfig config;
    
    private PackManager pm;
    
    private Level loglevel = Level.INFO;

    /**
     * Set of uuids of players which got send a pack by the backend server. 
     * This is needed so that the server does not send the bungee pack if the user has a backend one.
     */
    private Map<UUID, Boolean> backendPackedPlayers = new ConcurrentHashMap<UUID, Boolean>();

    /**
     * Set of uuids of players which were authenticated by a backend server's plugin
     */
    private Set<UUID> authenticatedPlayers = new HashSet<UUID>();

    /**
     * Wether the plugin is enabled or not
     */
    private boolean enabled = false;

    private int bungeeVersion;

    public void onEnable() {
        instance = this;
        try {
            List<Integer> supportedVersions = new ArrayList<Integer>();
            try {
                Field svField = Protocol.class.getField("supportedVersions");
                supportedVersions = (List<Integer>) svField.get(null);
            } catch(Exception e1) {
                // Old bungee protocol version, try new one
            }
            if(supportedVersions.size() == 0) {
                Field svIdField = ProtocolConstants.class.getField("SUPPORTED_VERSION_IDS");
                supportedVersions = (List<Integer>) svIdField.get(null);
            }

            bungeeVersion = supportedVersions.get(supportedVersions.size() - 1);
            if(bungeeVersion == ProtocolConstants.MINECRAFT_1_8) {
                getLogger().log(Level.INFO, "BungeeCord 1.8 detected!");
                Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", int.class, Class.class);
                reg.setAccessible(true);
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, ResourcePackSendPacket.class);
            } else if(bungeeVersion >= ProtocolConstants.MINECRAFT_1_9 && bungeeVersion < ProtocolConstants.MINECRAFT_1_9_4){
                getLogger().log(Level.INFO, "BungeeCord 1.9-1.9.3 detected!");
                Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", int.class, int.class, Class.class);
                reg.setAccessible(true);
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, 0x32, ResourcePackSendPacket.class);
            } else if(bungeeVersion >= ProtocolConstants.MINECRAFT_1_9_4){
                getLogger().log(Level.INFO, "BungeeCord 1.9.4+ detected!");
                Method map = Protocol.class.getDeclaredMethod("map", int.class, int.class);
                map.setAccessible(true);
                Object mapping18 = map.invoke(null, ProtocolConstants.MINECRAFT_1_8, 0x48);
                Object mapping19 = map.invoke(null, ProtocolConstants.MINECRAFT_1_9, 0x32);
                Object mappingsObject = Array.newInstance(mapping18.getClass(), 2);
                Array.set(mappingsObject, 0, mapping18);
                Array.set(mappingsObject, 1, mapping19);
                Object[] mappings = (Object[]) mappingsObject;
                Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", Class.class, mappings.getClass());
                reg.setAccessible(true);
                reg.invoke(Protocol.GAME.TO_CLIENT, ResourcePackSendPacket.class, mappings);
            } else {
                getLogger().log(Level.SEVERE, "Unsupported BungeeCord version found! You need at least 1.8 for this plugin to work!");
                setEnabled(false);
                return;
            }

            getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new BungeeResourcepacksCommand(this, getDescription().getName().toLowerCase().charAt(0) + "rp", getDescription().getName().toLowerCase() + ".command", new String[]{getDescription().getName().toLowerCase()}));
            getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new UsePackCommand(this, "usepack", getDescription().getName().toLowerCase() + ".command.usepack", new String[] {}));

            setEnabled(loadConfig());

            getProxy().getPluginManager().registerListener(this, new DisconnectListener(this));
            getProxy().getPluginManager().registerListener(this, new ServerSwitchListener(this));
            getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));
            getProxy().registerChannel("Resourcepack");

            BungeeStatsLite stats = new BungeeStatsLite(this);
            stats.start();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            getLogger().log(Level.SEVERE, "Couldn't find the registerPacket method in the Protocol.DirectionData class! Please update this plugin or downgrade BungeeCord!");
            e.printStackTrace();
        } catch(NoSuchFieldException e) {
            getLogger().log(Level.SEVERE, "Couldn't find the field with the supported versions! Please update this plugin or downgrade BungeeCord!");
            e.printStackTrace();
        }
    }

    public boolean loadConfig() {
        try {
            config = new YamlConfig(this, getDataFolder() + File.separator + "config.yml");
            getLogger().log(Level.INFO, "Loading config!");
        } catch (IOException e) {
            getLogger().severe("Unable to load configuration! " + getDescription().getName() + " will not be enabled!");
            e.printStackTrace();
            return false;
        }

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

        if(getConfig().isSet("useauth")) {
            getLogger().log(getLogLevel(), "Use backend auth: " + getConfig().getBoolean("useauth"));
        }

        pm = new PackManager(this);
        Configuration packs = getConfig().getSection("packs");
        getLogger().log(getLogLevel(), "Loading packs:");
        for(String s : packs.getKeys()) {
            Configuration packSection = packs.getSection(s);

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

            getPackManager().addPack(pack);
            getLogger().log(getLogLevel(), pack.getName() + " - " + pack.getUrl() + " - " + pack.getHash());
        }
        
        String emptypackname = getConfig().getString("empty");
        if(emptypackname != null && !emptypackname.isEmpty()) {
            ResourcePack ep = getPackManager().getByName(emptypackname);
            if(ep != null) {
                getLogger().log(getLogLevel(), "Empty pack: " + ep.getName());
                getPackManager().setEmptyPack(ep);
            } else {
                getLogger().warning("Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
            }
        }

        String globalpackname = getConfig().getString("global.pack");
        if(globalpackname != null && !globalpackname.isEmpty()) {
            ResourcePack gp = getPackManager().getByName(globalpackname);
            if(gp != null) {
                getLogger().log(getLogLevel(), "Global pack: " + gp.getName() + "!");
                getPackManager().setGlobalPack(gp);
            } else {
                getLogger().warning("Cannot set global resourcepack as there is no pack with the name " + globalpackname + " defined!");
            }
        }
        List<String> globalsecondary = getConfig().getStringList("global.secondary");
        if(globalsecondary != null && globalsecondary.size() > 0) {
            getLogger().log(getLogLevel(), "Global secondary packs:");
            for(String secondarypack : globalsecondary) {
                ResourcePack sp = getPackManager().getByName(secondarypack);
                if (sp != null) {
                    getPackManager().addGlobalSecondary(sp);
                    getLogger().log(getLogLevel(), sp.getName());
                } else {
                    getLogger().warning("Cannot add resourcepack as a global secondaray pack as there is no pack with the name " + secondarypack + " defined!");
                }
            }
        }
        
        Configuration servers = getConfig().getSection("servers");
        for(String s : servers.getKeys()) {
            getLogger().log(getLogLevel(), "Loading settings for server " + s + "!");
            String packname = servers.getString(s + ".pack");
            if(packname != null && !packname.isEmpty()) {
                ResourcePack sp = getPackManager().getByName(packname);
                if(sp != null) {
                    getPackManager().addServer(s, sp);
                    getLogger().log(getLogLevel(), "Pack: " + sp.getName() + "!");
                } else {
                    getLogger().warning("Cannot set resourcepack for " + s + " as there is no pack with the name " + packname + " defined!");
                }
            } else {
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
                        getLogger().warning("Cannot add resourcepack as a secondary pack for server " + s + " as there is no pack with the name " + secondarypack + " defined!");
                    }
                }
            }
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
            for (ProxiedPlayer p : getProxy().getPlayers()) {
                resendPack(p);
            }
        }
    }
    
    public static BungeeResourcepacks getInstance() {
        return instance;
    }
    
    public YamlConfig getConfig() {
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
        getPackManager().applyPack(player.getUniqueId(), serverName);
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
        if(clientVersion >= ProtocolConstants.MINECRAFT_1_8) {
            try {
                ResourcePackSendPacket packet = new ResourcePackSendPacket(pack.getUrl(), pack.getHash());
                player.unsafe().sendPacket(packet);
                sendPackInfo(player, pack);
                getLogger().log(getLogLevel(), "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
            } catch(BadPacketException e) {
                getLogger().log(Level.SEVERE, e.getMessage() + " Please check for updates!");
            } catch(ClassCastException e) {
                getLogger().log(Level.SEVERE, "Packet defined was not ResourcePackSendPacket? Please check for updates!");
            }
        } else {
            getLogger().log(Level.WARNING, "Cannot send the pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName() + " as he uses the unsupported protocol version " + clientVersion + "!");
            getLogger().log(Level.WARNING, "Consider blocking access to your server for clients that are not 1.8 or 1.9 if you want this plugin to work for everyone!");
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
    public void sendPackInfo(ProxiedPlayer player, ResourcePack pack) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        if(pack != null) {
            out.writeUTF("packChange");
            out.writeUTF(player.getName());
            out.writeUTF(pack.getName());
            out.writeUTF(pack.getUrl());
            out.writeUTF(pack.getHash());
        } else {
            out.writeUTF("clearPack");
            out.writeUTF(player.getName());
        }
        player.getServer().sendData("Resourcepack", out.toByteArray());
    }

    public void setPack(UUID playerId, ResourcePack pack) {
        getPackManager().setPack(playerId, pack);
    }

    public void sendPack(UUID playerId, ResourcePack pack) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            sendPack(player, pack);
        }
    }

    public void clearPack(ProxiedPlayer player) {
        clearPack(player.getUniqueId());
    }

    public void clearPack(UUID playerId) {
        getPackManager().clearUserPack(playerId);
    }

    public PackManager getPackManager() {
        return pm;
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

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key, getConfig().getDefaults().getString("messages." + key));
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

    public String getName() {
        return getDescription().getName();
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

    public Level getLogLevel() {
        return loglevel;
    }

    @Override
    public ResourcepacksPlayer getPlayer(UUID playerId) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            return new ResourcepacksPlayer(player.getName(), player.getUniqueId());
        }
        return null;
    }

    @Override
    public ResourcepacksPlayer getPlayer(String playerName) {
        ProxiedPlayer player = getProxy().getPlayer(playerName);
        if(player != null) {
            return new ResourcepacksPlayer(player.getName(), player.getUniqueId());
        }
        return null;
    }

    @Override
    public boolean sendMessage(ResourcepacksPlayer player, String message) {
        if(player != null) {
            ProxiedPlayer proxyPlayer = getProxy().getPlayer(player.getUniqueId());
            if(proxyPlayer != null) {
                proxyPlayer.sendMessage(message);
                return true;
            }
        } else {
            getProxy().getConsole().sendMessage(message);
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
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(playerId);
        if(proxiedPlayer != null) {
            return proxiedPlayer.hasPermission(perm);
        }
        return false;

    }

    @Override
    public int getPlayerPackFormat(UUID playerId) {
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(playerId);
        if(proxiedPlayer != null) {
            int version = proxiedPlayer.getPendingConnection().getVersion();
            if(version < 47) { // pre 1.8
                return 0;
            } else if(version < 107) { // pre 1.9
                return 1;
            } else { // current
                return 2;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public IResourcePackSelectEvent callPackSelectEvent(UUID playerId, ResourcePack pack, IResourcePackSelectEvent.Status status) {
        ResourcePackSelectEvent selectEvent = new ResourcePackSelectEvent(playerId, pack, status);
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
        return !getConfig().getBoolean("useauth", false) || authenticatedPlayers.contains(playerId);
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
}
