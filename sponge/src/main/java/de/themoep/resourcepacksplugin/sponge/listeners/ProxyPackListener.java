package de.themoep.resourcepacksplugin.sponge.listeners;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.sponge.SpongeResourcepacks;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.PlayerConnection;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 02.02.2016.
 */
public class ProxyPackListener implements RawDataListener {

    private final SpongeResourcepacks plugin;
    private Map<String, ProxyPackReaction> subChannels = new HashMap<>();

    public ProxyPackListener(SpongeResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handlePayload(ChannelBuf data, RemoteConnection connection, Platform.Type side) {
        if (!(connection instanceof PlayerConnection)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data.array());
        String subchannel = in.readUTF();

        if (subchannel.equals("packChange")) {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            String packName = in.readUTF();
            String packUrl = in.readUTF();
            String packHash = in.readUTF();

            Optional<Player> player = Sponge.getServer().getPlayer(playerName);
            if (!player.isPresent() || !player.get().isOnline()) {
                plugin.logDebug("Proxy send pack " + packName + " (" + packUrl + ") to player " + playerName + " but they aren't online?");
            }

            ResourcePack pack = plugin.getPackManager().getByName(packName);
            if (pack == null) {
                pack = new ResourcePack(packName, packUrl, packHash);
                try {
                    plugin.getPackManager().addPack(pack);
                } catch (IllegalArgumentException e) {
                    pack = plugin.getPackManager().getByHash(packHash);
                    if (pack == null) {
                        pack = plugin.getPackManager().getByUrl(packUrl);
                    }
                }
            }

            plugin.logDebug("Proxy send pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + player.get().getName());
            plugin.getUserManager().setUserPack(player.get().getUniqueId(), pack);
        } else if (subchannel.equals("clearPack")) {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            Optional<Player> player = Sponge.getServer().getPlayer(playerUuid);
            if (!player.isPresent() || !player.get().isOnline()) {
                plugin.logDebug("Proxy send command to clear the pack of player " + playerName + " but they aren't online?");
            }

            plugin.logDebug("Proxy send command to clear the pack of player " + playerName);
            plugin.clearPack(playerUuid);
        } else if (subChannels.containsKey(subchannel)) {
            subChannels.get(subchannel).execute(((PlayerConnection) connection).getPlayer(), in);
        } else {
            plugin.log(Level.WARNING, "Unknown subchannel " + subchannel + "! Please make sure you are running a compatible plugin version on your Proxy!");
        }
    }

    /**
     * Register a new sub channel with this listener on the channel "rp:plugin"
     * @param name     The name of the sub channel, case sensitive
     * @param reaction The reaction that should happen
     * @return The previously registered Reaction or null
     */
    public ProxyPackReaction registerSubChannel(String name, ProxyPackReaction reaction) {
        return subChannels.put(name, reaction);
    }
}