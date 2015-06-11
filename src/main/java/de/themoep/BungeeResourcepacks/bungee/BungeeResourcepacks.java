package de.themoep.BungeeResourcepacks.bungee;

import de.themoep.BungeeResourcepacks.bungee.listeners.DisconnectListener;
import de.themoep.BungeeResourcepacks.bungee.listeners.ServerSwitchListener;
import de.themoep.BungeeResourcepacks.bungee.packets.ResourcePackSendPacket;
import de.themoep.BungeeResourcepacks.core.PackManager;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.Protocol;

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
public class BungeeResourcepacks extends Plugin {

    private YamlConfig config;
    
    private PackManager pm;
    
    public Level loglevel;

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
        getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new BungeeResouecepacksCommand(this, "bungeeresourcepacks", "bungeeresourcepacks.command", new String[] {"brp"}));
        
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
            getLogger().severe("Unable to load configuration! BungeeResourcepacks will not be enabled!");
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
        if(emptypackname != null) {
            ResourcePack ep = getPackManager().getByName(emptypackname);
            if(ep != null) {
                getPackManager().setEmptyPack(ep);
            } else {
                getLogger().warning("Cannot set empty resourcepack as there is no pack with the name " + emptypackname + " defined!");
            }
        }

        String globalpackname = getConfig().getString("global.pack");
        if(globalpackname != null) {
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
            if(packname != null) {
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
                        getPackManager().addGlobalSecondary(sp);
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
    public void reloadConfig() {
        loadConfig();
        if(isEnabled()) {
            getLogger().log(Level.INFO, "Reloaded config. Resending packs for all online players!");
            for (ProxiedPlayer p : getProxy().getPlayers()) {
                resendPack(p);
            }
        }
    }
    
    public static BungeeResourcepacks getInstance() {
        return (BungeeResourcepacks) ProxyServer.getInstance().getPluginManager().getPlugin("BungeeResourcepacks");
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
    
    /**
     * Set the resoucepack of a connected player
     * @param player The ProxiedPlayer to set the pack for
     * @param pack The resourcepack to set for the player
     */
    public void setPack(ProxiedPlayer player, ResourcePack pack) {
        player.unsafe().sendPacket(new ResourcePackSendPacket(pack.getUrl(), pack.getHash()));
        getPackManager().setUserPack(player.getUniqueId(), pack);
        getLogger().log(loglevel, "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
    }

    public void clearPack(ProxiedPlayer player) {
        getPackManager().clearUserPack(player.getUniqueId());
    }

    public PackManager getPackManager() {
        return pm;
    }

    /**
     * Add a player's uuid to the list of players with a backend pack
     * @param playerid The uuid of the player
     */
    public void setBackend(UUID playerid) {
        backendPackedPlayers.put(playerid, false);
    }

    /**
     * Remove a player's uuid from the list of players with a backend pack
     * @param playerid The uuid of the player
     */
    public void unsetBackend(UUID playerid) {
        backendPackedPlayers.remove(playerid);
    }
    
    /**
     * Check if a player has a pack set by a backend server
     * @param playerid The uuid of the player
     * @return If the player has a backend pack
     */
    public boolean hasBackend(UUID playerid) {
        return backendPackedPlayers.containsKey(playerid);
    }

}
