package de.themoep.BungeeResourcepacks.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.BungeeResourcepacks.bungee.listeners.DisconnectListener;
import de.themoep.BungeeResourcepacks.bungee.listeners.ServerSwitchListener;
import de.themoep.BungeeResourcepacks.bungee.packets.ResourcePackSendPacket;
import de.themoep.BungeeResourcepacks.core.PackManager;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlayer;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
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
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, ResourcePackSendPacket.class);
                
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
        
        pm = new PackManager();
        Configuration packs = getConfig().getSection("packs");
        for(String s : packs.getKeys()) {
            getPackManager().addPack(new ResourcePack(s.toLowerCase(), packs.getString(s + ".url"), packs.getString(s + ".hash")));
        }
        
        String emptypackname = getConfig().getString("empty");
        if(emptypackname != null && !emptypackname.isEmpty()) {
            ResourcePack ep = getPackManager().getByName(emptypackname);
            if(ep != null) {
                getPackManager().setEmptyPack(ep);
            } else {
                getLogger().warning("Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
            }
        }

        String globalpackname = getConfig().getString("global.pack");
        if(globalpackname != null && !globalpackname.isEmpty()) {
            ResourcePack gp = getPackManager().getByName(globalpackname);
            if(gp != null) {
                getPackManager().setGlobalPack(gp);
            } else {
                getLogger().warning("Cannot set global resourcepack as there is no pack with the name " + globalpackname + " defined!");
            }
        }
        List<String> globalsecondary = getConfig().getStringList("global.secondary");
        if(globalsecondary != null) {
            for(String secondarypack : globalsecondary) {
                ResourcePack sp = getPackManager().getByName(secondarypack);
                if (sp != null) {
                    getPackManager().addGlobalSecondary(sp);
                } else {
                    getLogger().warning("Cannot add resourcepack as a global secondaray pack as there is no pack with the name " + secondarypack + " defined!");
                }
            }
        }
        
        Configuration servers = getConfig().getSection("servers");
        for(String s : servers.getKeys()) {
            String packname = servers.getString(s + ".pack");
            if(packname != null && !packname.isEmpty()) {
                ResourcePack sp = getPackManager().getByName(packname);
                if(sp != null) {
                    getPackManager().addServer(s, sp);
                } else {
                    getLogger().warning("Cannot set resourcepack for " + s + " as there is no pack with the name " + packname + " defined!");
                }
            }  else {
                getLogger().warning("Cannot find a pack setting for " + s + "! Please make sure you have a pack node on servers." + s + "!");
            }
            List<String> serversecondary = getConfig().getStringList(s + ".secondary");
            if(serversecondary != null) {
                for(String secondarypack : serversecondary) {
                    ResourcePack sp = getPackManager().getByName(s);
                    if (sp != null) {
                        getPackManager().addServerSecondary(s, sp);
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
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if the plugin is enabled or not
     * @param enabled
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
            player.unsafe().sendPacket(new ResourcePackSendPacket(pack.getUrl(), pack.getHash()));
            getPackManager().setUserPack(player.getUniqueId(), pack);
            sendPackInfo(player, pack);
            getLogger().log(loglevel, "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
        } else {
            getLogger().log(Level.WARNING, "Cannot send the pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName() + " as he uses the unsupported protocol version " + clientVersion + "!");
            getLogger().log(Level.WARNING, "Consider blocking access to your server for clients below 1.8 if you want this plugin to work for everyone!");
        }
    }

    /**
      * <p>Send a plugin message to the server the player is connected to!</p>
      * @param player The player to update the pack on the player's bukkit server
      * @param pack The ResourcePack to send the info of the the Bukkit server, null if you want to clear it!
      * Channel: ForceResourcepacks<br />
      * sub-channel: packChange<br />
      * arg1: playername<br />
      * arg2: pack.getName();<br />
      * arg3: pack.getUrl();<br />
      * arg4: pack.getHash();<br />
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

    public void setBackend(UUID playerId) {
        backendPackedPlayers.put(playerId, false);
    }

    public void unsetBackend(UUID playerId) {
        backendPackedPlayers.remove(playerId);
    }

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
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(player.getUniqueId());
        if(proxiedPlayer != null) {
            return proxiedPlayer.hasPermission(perm);
        }
        return false;

    }
}
