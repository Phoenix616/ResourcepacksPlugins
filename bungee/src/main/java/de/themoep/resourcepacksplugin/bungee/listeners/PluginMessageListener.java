package de.themoep.resourcepacksplugin.bungee.listeners;

/*
 * ResourcepacksPlugins - bungee
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

import de.themoep.bungeeplugin.FileConfiguration;
import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class PluginMessageListener extends SubChannelHandler<Server> implements Listener {
    private final BungeeResourcepacks plugin;
    private FileConfiguration keyConfig;
    private final AuthHandler authHandler;

    public PluginMessageListener(BungeeResourcepacks plugin) {
        super(plugin);
        this.plugin = plugin;
        File keyFile = new File(plugin.getDataFolder(), "key.yml");
        try {
            keyConfig = new FileConfiguration(plugin, keyFile);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Unable to create key.yml! " + e.getMessage());
        }
        authHandler = new AuthHandler(plugin);
        registerSubChannel("authLogin", (s, in) -> {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());
            ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
            if (player != null && !plugin.isAuthenticated(playerId)) {
                authHandler.onAuth(player);
            }
        });
    }

    @EventHandler
    public void pluginMessageReceived(PluginMessageEvent event) {
        if (!plugin.isEnabled() || !event.getTag().equals(MESSAGING_CHANNEL))
            return;

        event.setCancelled(true);
        if (event.getSender() instanceof Server) {
            handleMessage((Server) event.getSender(), event.getData());
        } else {
            plugin.logDebug("Received plugin message from " + event.getSender() + " which is not a ServerConnection!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerSwitch(ServerConnectedEvent event) {
        if (plugin.isEnabled()) {
            sendKey(event.getServer());
        }
    }

    @Override
    protected void sendPluginMessage(Server target, byte[] data) {
        target.sendData(MESSAGING_CHANNEL, data);
    }

    @Override
    protected void saveKey(String key) {
        if (keyConfig != null) {
            keyConfig.set("key", key);
            keyConfig.saveConfig();
        }
    }

    @Override
    protected String loadKey() {
        String key = null;
        if (keyConfig != null) {
            try {
                if (keyConfig.loadConfig()) {
                    key = keyConfig.getString("key", null);
                }
            } catch (IOException e) {
                plugin.log(Level.SEVERE, "Error while loading key.yml! " + e.getMessage());
            }
            if (key == null) {
                key = generateKey();
                saveKey(key);
            }
        }
        return key;
    }
}
