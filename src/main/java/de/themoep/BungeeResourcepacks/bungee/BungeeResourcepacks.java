package de.themoep.BungeeResourcepacks.bungee;

import de.themoep.BungeeResourcepacks.bungee.listeners.JoinListener;
import de.themoep.BungeeResourcepacks.bungee.listeners.ServerSwitchListener;
import de.themoep.BungeeResourcepacks.bungee.packets.ResourcePackSendPacket;
import de.themoep.BungeeResourcepacks.core.PackManager;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.Protocol;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Phoenix616 on 18.03.2015.
 */
public class BungeeResourcepacks extends Plugin {

    private static YamlConfig config;
    
    private static PackManager pm;
        
    public void onEnable() {
        try {
            Method reg = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", new Class[] { int.class, Class.class });
            reg.setAccessible(true);
            try {
                reg.invoke(Protocol.GAME.TO_CLIENT, 0x48, ResourcePackSendPacket.class);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            loadConfig();

            //getProxy().getPluginManager().registerCommand(this, new BungeeTestCommand("bungeetest", "bungeetest.command", "btest"));
            getProxy().getPluginManager().registerListener(this, new JoinListener());
            getProxy().getPluginManager().registerListener(this, new ServerSwitchListener());
            
        } catch (NoSuchMethodException e) {
            getLogger().severe("Couldn't find the registerPacket method in the Protocol.DirectionData class! Please update this plugin or downgrade BungeeCord!");
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        try {
            config = new YamlConfig(getDataFolder() + File.separator + "config.yml");
        } catch (IOException e) {
            getLogger().severe("Unable to load configuration! NeoBans will not be enabled.");
            e.printStackTrace();
            return;
        }

        pm = new PackManager();
        Configuration packs = getConfig().getSection("packs");
        for(String s : packs.getKeys()) {
            pm.addPack(new ResourcePack(s.toLowerCase(), packs.getString(s + ".url"), packs.getString(s + ".hash")));
        }

        Configuration servers = getConfig().getSection("servers");
        for(String s : servers.getKeys()) {
            String packname = servers.getString(s + ".pack");
            if(packname != null) {
                ResourcePack bp = pm.getByName(packname);
                if(bp != null) {
                    pm.addServer(s, bp);
                } else {
                    getLogger().warning("Cannot set resourcepack for " + s + " as there is no pack with the name " + packname + " defined!");
                }
            }  else {
                getLogger().warning("Cannot find a pack setting for " + s + "! Please make sure you have a pack node on servers." + s + "!");
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
     * Set the resoucepack of a connected player
     * @param player The ProxiedPlayer to set the pack for
     * @param pack The resourcepack to set for the player
     */
    public void setPack(ProxiedPlayer player, ResourcePack pack) {
        getPackManager().setUserPack(player.getUniqueId(), pack);
        ((UserConnection) player).unsafe().sendPacket(new ResourcePackSendPacket(pack));
        BungeeResourcepacks.getInstance().getLogger().info("Send pack " + pack.getName() + " (" + pack.getUrl() + ") to " + player.getName());
    }

    public PackManager getPackManager() {
        return pm;
    }
}
