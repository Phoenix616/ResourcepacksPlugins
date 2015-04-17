package de.themoep.BungeeResourcepacks.bungee;

import de.themoep.BungeeResourcepacks.bungee.listeners.JoinListener;
import de.themoep.BungeeResourcepacks.bungee.listeners.ServerConnectListener;
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
import java.util.ArrayList;
import java.util.Arrays;
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
     * Set of uuids of currently joining players. This is needed for backend packs to be send after bungee packs
     */
    private Map<UUID, Boolean> joiningplayers = new ConcurrentHashMap<UUID, Boolean>();

    /**
     * Wether the plugin is enabled or not
     */
    public boolean enabled = false;

    public void onEnable() {
        getProxy().getPluginManager().registerCommand(BungeeResourcepacks.getInstance(), new BungeeResouecepacksCommand(this, "bungeeresourcepacks", "bungeeresourcepacks.command", new String[] {"brp"}));
        
        try {
            Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", new Class[] { int.class, Class.class });
            reg.setAccessible(true);
            try {
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, ResourcePackSendPacket.class);
                
                enabled = loadConfig();

                //getProxy().getPluginManager().registerCommand(this, new BungeeTestCommand("bungeetest", "bungeetest.command", "btest"));
                getProxy().getPluginManager().registerListener(this, new JoinListener());
                getProxy().getPluginManager().registerListener(this, new ServerConnectListener());
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
            config = new YamlConfig(getDataFolder() + File.separator + "config.yml");
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
            pm.addPack(new ResourcePack(s.toLowerCase(), packs.getString(s + ".url"), packs.getString(s + ".hash")));
        }

        String globalpackname = getConfig().getString("global.pack");
        if(globalpackname != null) {
            ResourcePack gp = pm.getByName(globalpackname);
            if(gp != null) {
                pm.setGlobalPack(gp);
            }
        }
        
        Configuration servers = getConfig().getSection("servers");
        for(String s : servers.getKeys()) {
            String packname = servers.getString(s + ".pack");
            if(packname != null) {
                ResourcePack sp = pm.getByName(packname);
                if(sp != null) {
                    pm.addServer(s, sp);
                } else {
                    getLogger().warning("Cannot set resourcepack for " + s + " as there is no pack with the name " + packname + " defined!");
                }
            }  else {
                getLogger().warning("Cannot find a pack setting for " + s + "! Please make sure you have a pack node on servers." + s + "!");
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
        if(enabled) {
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
        player.unsafe().sendPacket(new ResourcePackSendPacket(pack));
        getPackManager().setUserPack(player.getUniqueId(), pack);
        BungeeResourcepacks.getInstance().getLogger().log(loglevel, "Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
    }

    public void clearPack(ProxiedPlayer player) {
        getPackManager().clearUserPack(player.getUniqueId());
    }

    public PackManager getPackManager() {
        return pm;
    }

    /**
     * Add a players uuid to the list of currently joining players
     * @param playerid The uuid of the player
     */
    public void setJoining(UUID playerid) {
        joiningplayers.put(playerid, false);
    }

    /**
     * Remove a players uuid from the list of currently joining players
     * @param playerid The uuid of the player
     */
    public void unsetJoining(UUID playerid) {
        joiningplayers.remove(playerid);
    }
    
    /**
     * Check if a player is on the list of currently joining players
     * @param playerid The uuid of the player
     * @return If the player is currently joining or not
     */
    public boolean isJoining(UUID playerid) {
        return joiningplayers.containsKey(playerid);
    }

}
