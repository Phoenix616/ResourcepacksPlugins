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

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 02.02.2016.
 */
public class ProxyPackListener implements PluginMessageListener {

    private final WorldResourcepacks plugin;
    private Map<String, ProxyPackReaction> subChannels = new HashMap<>();

    public ProxyPackListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] message) {
        if(!channel.equals("rp:plugin")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();

        if(subchannel.equals("packChange")) {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            String packName = in.readUTF();
            String packUrl = in.readUTF();
            String packHash = in.readUTF();

            Player player = plugin.getServer().getPlayer(playerUuid);
            if(player == null || !player.isOnline()) {
                plugin.logDebug("Proxy send pack " + packName + " (" + packUrl + ") to player " + playerName + " but they aren't online?");
            }

            ResourcePack pack = plugin.getPackManager().getByName(packName);
            if(pack == null) {
                try {
                    pack = new ResourcePack(packName, packUrl, packHash);
                    plugin.getPackManager().addPack(pack);
                } catch (IllegalArgumentException e) {
                    pack = plugin.getPackManager().getByHash(packHash);
                    if(pack == null) {
                        pack = plugin.getPackManager().getByUrl(packUrl);
                    }
                }
            }

            plugin.logDebug("Proxy send pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + playerName);
            plugin.getUserManager().setUserPack(playerUuid, pack);
        } else if(subchannel.equals("clearPack")) {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                plugin.logDebug("Proxy send command to clear the pack of player " + playerName + " but they aren't online?");
            }

            plugin.logDebug("Proxy send command to clear the pack of player " + playerName);
            plugin.clearPack(playerUuid);
        } else if (subChannels.containsKey(subchannel)) {
            subChannels.get(subchannel).execute(p, in);
        } else {
            plugin.getLogger().log(Level.WARNING, "Unknown subchannel " + subchannel + "! Please make sure you are running a compatible plugin version on your Proxy!");
        }
    }

    /**
     * Register a new sub channel with this listener on the channel "rp:plugin"
     * @param name      The name of the sub channel, case sensitive
     * @param reaction  The reaction that should happen
     * @return          The previously registered Reaction or null
     */
    public ProxyPackReaction registerSubChannel(String name, ProxyPackReaction reaction) {
        return subChannels.put(name, reaction);
    }

}