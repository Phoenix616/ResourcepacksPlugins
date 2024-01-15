package de.themoep.resourcepacksplugin.core;

/*
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
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class SubChannelHandler<S> {
    public static final String MESSAGING_CHANNEL = "rp:plugin";
    private final long version = 1;
    private final Map<String, BiConsumer<S, ByteArrayDataInput>> subChannels = new HashMap<>();
    private final ResourcepacksPlugin plugin;
    private String key;

    public SubChannelHandler(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
        registerSubChannel("key", (p, in) -> {
            if (!trustsSender() && acceptsNewKey()) {
                String key = in.readUTF();
                if (!key.isEmpty()) {
                    plugin.logDebug("New key was sent via connection of " + p + " (If you are not using a proxy, this is a bug or player trying to exploit your server!)");
                    setKey(key);
                }
            }
        });
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
            long version = in.readLong();
            if (version != this.version) {
                plugin.log(Level.SEVERE, "Received a message with an incompatible version by " + source + " on subchannel " + subChannel + "! Please make sure you are running the same plugin version on both your Minecraft server and the proxy!");
                return false;
            }
            String key = in.readUTF();
            if (!trustsSender() && !"key".equals(subChannel) && (key.isEmpty() || !key.equals(this.key))) {
                plugin.log(Level.WARNING, "Received a message with an invalid key by " + source + " on subchannel " + subChannel + "! Please make sure the key on your proxy(s) and Minecraft servers match!");
                return false;
            }
            reaction.accept(source, in);
            return true;
        }
        plugin.log(Level.WARNING, "Unknown subchannel " + subChannel + "! Please make sure you are running a compatible plugin version on your Proxy!");
        return false;
    }

    protected void sendKey(S target) {
        if (key != null && !key.isEmpty()) {
            sendMessage(target, "key", out -> out.writeUTF(key));
        }
    }

    /**
     * Send a message to the target on the given sub channel
     * @param target        The target to send the message to
     * @param subChannel    The sub channel to send the message on
     * @param out           The output to write to
     */
    public void sendMessage(S target, String subChannel, Consumer<ByteArrayDataOutput> out) {
        if (key == null) {
            plugin.log(Level.WARNING, "Can't send message to " + target + " on " + subChannel + " as no key is set!");
            return;
        }
        if (key.isEmpty()) {
            plugin.logDebug("Not sending message to " + target + " on " + subChannel + " as we are not in an environment where we should (key is empty in keys.yml)");
            return;
        }
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF(subChannel);
        dataOutput.writeLong(version);
        dataOutput.writeUTF(trustsSender() ? key : "");
        out.accept(dataOutput);
        sendPluginMessage(target, dataOutput.toByteArray());
    }

    protected abstract void sendPluginMessage(S target, byte[] data);

    /**
     * Reload the handler
     */
    public void reload() {
        key = loadKey();
    }

    /**
     * Generate a new key
     */
    protected String generateKey() {
        byte[] key;
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey secretKey = generator.generateKey();
            key = secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            plugin.log(Level.WARNING, "Unable to generate a new key with AES! Using a less-secure random one! " + e.getMessage());
            key = String.valueOf(new Random().nextLong()).getBytes();
        }
        plugin.logDebug("Generated new key! You can find it in the key.yml file.");
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * Set the key used for authenticating communication and save it
     * @param key The key to set
     */
    protected void setKey(String key) {
        this.key = key;
        saveKey(key);
    }

    /**
     * Check if this listener accepts a new key
     * @return Whether it accepts a new key or not
     */
    protected boolean acceptsNewKey() {
        return key == null;
    }

    /**
     * Check whether this implementation can trust the sender
     *
     * @return Whether the sender can be trusted or not
     */
    protected boolean trustsSender() {
        return true;
    }

    /**
     * Save the key used for authenticating communication
     * @param key The key to set
     */
    protected abstract void saveKey(String key);

    /**
     * Load the key from the storage
     * @return The key (or null if none is stored)
     */
    protected abstract String loadKey();

}
