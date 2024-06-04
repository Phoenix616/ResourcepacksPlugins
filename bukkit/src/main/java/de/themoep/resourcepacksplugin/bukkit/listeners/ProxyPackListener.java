package de.themoep.resourcepacksplugin.bukkit.listeners;

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

import de.themoep.resourcepacksplugin.bukkit.ConfigAccessor;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 02.02.2016.
 */
public class ProxyPackListener extends SubChannelHandler<Player> implements PluginMessageListener, Listener {

    private final WorldResourcepacks plugin;

    private final ConfigAccessor keyConfig;

    private boolean playerJoined = false;

    public ProxyPackListener(WorldResourcepacks plugin) {
        super(plugin);
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        keyConfig = new ConfigAccessor(plugin, "key.yml");
        registerSubChannel("removePackRequest", (p, in) -> {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());

            ResourcePack pack = readPack(in);
            if (pack == null) {
                plugin.logDebug("Proxy sent command to send a pack removal request for an unknown pack for player " + playerName + "?");
                return;
            }

            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                plugin.logDebug("Proxy sent command to send a pack removal request for pack " + pack.getName() + "/" + pack.getUuid() + " of player " + playerName + " but they aren't online?");
                return;
            }
            if (pack.equals(plugin.getPackManager().getEmptyPack())) {
                plugin.logDebug("Proxy sent command to send a pack removal request for all packs for player " + playerName);
                plugin.removePacks(playerUuid);
                return;
            }
            plugin.logDebug("Proxy send command to send a pack removal request for pack " + pack.getName() + "/" + pack.getUuid() + " for player " + playerName);
            try {
                plugin.removePack(player, pack);
            } catch (UnsupportedOperationException unsupported) {
                plugin.logDebug("Proxy sent command to send a pack removal request for pack " + pack.getName() + "/" + pack.getUuid() + " for player " + playerName + " but the server doesn't support it?");
            }
        });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] message) {
        if (!plugin.isEnabled() || !channel.equals("rp:plugin")) {
            return;
        }

        handleMessage(p, message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (playerJoined) {
            if (acceptsNewKey()) {
                setKey("");
            }
        } else {
            playerJoined = true;
        }
    }

    @Override
    protected void sendPluginMessage(Player target, byte[] data) {
        try {
            target.sendPluginMessage(plugin, MESSAGING_CHANNEL, data);
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to send plugin message to " + getTargetType() + " (via " + target.getName() + " connection)!" +
                    " This is most likely because the player connection timed out. " + e.getMessage());
            plugin.logDebug("Plugin message sending error:", e);
        }
    }

    @Override
    protected void saveKey(String key) {
        keyConfig.getConfig().set("key", key);
        keyConfig.saveConfig();
    }

    @Override
    protected String loadKey() {
        keyConfig.reloadConfig();
        return keyConfig.getConfig().getString("key", null);
    }

    @Override
    protected String getTargetType() {
        return "proxy";
    }

    @Override
    protected boolean trustsSender() {
        return false;
    }
}