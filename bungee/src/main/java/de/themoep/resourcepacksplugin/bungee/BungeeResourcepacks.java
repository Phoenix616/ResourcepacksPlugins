package de.themoep.resourcepacksplugin.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bungee.listeners.DisconnectListener;
import de.themoep.resourcepacksplugin.bungee.listeners.ServerSwitchListener;
import de.themoep.resourcepacksplugin.bungee.packets.ResourcePackSendPacket;
import de.themoep.resourcepacksplugin.core.PackManager;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.BadPacketException;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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
    
    private Level loglevel;

    /**
     * Set of uuids of players which got send a pack by the backend server. 
     * This is needed so that the server does not send the bungee pack if the user has a backend one.
     */
    private Map<UUID, Boolean> backendPackedPlayers = new ConcurrentHashMap<UUID, Boolean>();

    /**
     * Wether the plugin is enabled or not
     */
    private boolean enabled = false;

    public void onEnable() {
        instance = this;
        
        getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new BungeeResourcepacksCommand(this, getDescription().getName().toLowerCase().charAt(0) + "rp", getDescription().getName().toLowerCase() + ".command", new String[]{getDescription().getName().toLowerCase()}));
        getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new UsePackCommand(this, "usepack", getDescription().getName().toLowerCase() + ".command.usepack", new String[] {}));

        try {
            Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", new Class[] { int.class, Class.class });
            reg.setAccessible(true);
            try {
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, 0x32, ResourcePackSendPacket.class);
                
                boolean loadingSuccessful = loadConfig();
                
                setEnabled(loadingSuccessful);

                getProxy().getPluginManager().registerListener(this, new DisconnectListener(this));
                getProxy().getPluginManager().registerListener(this, new ServerSwitchListener(this));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }            
        } catch (NoSuchMethodException e) {
            getLogger().log(Level.SEVERE, "Couldn't find the registerPacket method in the Protocol.DirectionData class! Please update this plugin or downgrade BungeeCord!");
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

        if(getConfig().getString("debug","true").equalsIgnoreCase("true")) {
            loglevel = Level.INFO;
        } else {
            loglevel = Level.FINE;
        }
        getLogger().log(Level.INFO, "Debug level: " + getLogLevel().getName());
        
        pm = new PackManager();
        Configuration packs = getConfig().getSection("packs");
        getLogger().log(getLogLevel(), "Loading packs:");
        for(String s : packs.getKeys()) {
            ResourcePack pack = new ResourcePack(s.toLowerCase(), packs.getString(s + ".url"), packs.getString(s + ".hash"));
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
        ResourcePack pack = null;
        Server server = player.getServer();
        if(server != null) {
            pack = getPackManager().getServerPack(server.getInfo().getName());
        }
        if (pack == null) {
            pack = getPackManager().getGlobalPack();
        }
        if (pack != null) {
            setPack(player, pack);
        }
    }

    public void resendPack(UUID playerId) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            resendPack(player);
        }
    }
    
    /**
     * Set the resoucepack of a connected player
     * @param player The ProxiedPlayer to set the pack for
     * @param pack The resourcepack to set for the player
     */
    public void setPack(ProxiedPlayer player, ResourcePack pack) {
        int clientVersion = player.getPendingConnection().getVersion();
        if(clientVersion >= ProtocolConstants.MINECRAFT_1_8) {
            try {
                ResourcePackSendPacket packet = (ResourcePackSendPacket) Protocol.GAME.TO_CLIENT.createPacket(0x48, clientVersion);
                packet.setHash(pack.getHash());
                packet.setUrl(pack.getUrl());
                player.unsafe().sendPacket(packet);
                getPackManager().setUserPack(player.getUniqueId(), pack);
                sendPackInfo(player, pack);
                getLogger().log(getLogLevel(), "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
            } catch(BadPacketException e) {
                getLogger().log(Level.SEVERE, "No Packet found with that id? Please check for updates!");
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
      * <p>Channel: ForceResourcepacks</p>
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
        player.getServer().sendData(getDescription().getName(), out.toByteArray());
    }

    public void setPack(UUID playerId, ResourcePack pack) {
        ProxiedPlayer player = getProxy().getPlayer(playerId);
        if(player != null) {
            setPack(player, pack);
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
        if(msg.isEmpty()) {
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
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(player.getUniqueId());
        if(proxiedPlayer != null) {
            return proxiedPlayer.hasPermission(perm);
        }
        return false;

    }
}
