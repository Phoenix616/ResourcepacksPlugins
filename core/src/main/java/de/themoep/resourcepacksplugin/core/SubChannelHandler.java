package de.themoep.resourcepacksplugin.core;/*
 * ResourcepacksPlugins - core
 * Copyright (C) 2024 Max Lee aka Phoenix616 (mail@moep.tv)
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public abstract class SubChannelHandler<S> {
    private final Map<String, BiConsumer<S, ByteArrayDataInput>> subChannels = new HashMap<>();
    private final ResourcepacksPlugin plugin;

    public SubChannelHandler(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a new sub channel with this listener on the channel "rp:plugin"
     * @param name      The name of the sub channel, case sensitive
     * @param reaction  The reaction that should happen
     * @return          The previously registered Reaction or null
     */
    public BiConsumer<S, ByteArrayDataInput> registerSubChannel(String name, BiConsumer<S, ByteArrayDataInput> reaction) {
        return subChannels.put(name, reaction);
    }

    /**
     * Handle a message with sub channels
     * @param source   The source of the plugin message
     * @param message  The message that was received
     * @return         Whether the message was handled or not
     */
    protected boolean handleMessage(S source, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        BiConsumer<S, ByteArrayDataInput> reaction = subChannels.get(subChannel);
        if (reaction != null) {
            reaction.accept(source, in);
            return true;
        }
        plugin.log(Level.WARNING, "Unknown subchannel " + subChannel + "! Please make sure you are running a compatible plugin version on your Proxy!");
        return false;
    }
}
