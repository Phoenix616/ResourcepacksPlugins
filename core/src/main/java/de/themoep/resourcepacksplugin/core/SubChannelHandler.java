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
import java.util.UUID;
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
        registerSubChannel("packsChange", (p, in) -> {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            int packCount = in.readInt();

            ResourcepacksPlayer player = plugin.getPlayer(playerUuid);
            if (player == null) {
                plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent pack " + packCount + " packs to player " + playerName + " but they aren't online?");
            }

            plugin.getUserManager().clearUserPacks(playerUuid);

            for (int i = 0; i < packCount; i++) {
                ResourcePack pack = readPack(in);
                if (pack != null) {
                    plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + playerName);
                    plugin.getUserManager().addUserPack(playerUuid, pack);
                } else {
                    plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to add an unknown pack to " + playerName + "?");
                }
            }
        });
        registerSubChannel("clearPack", (p, in) -> {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());
            ResourcepacksPlayer player = plugin.getPlayer(playerUuid);
            if (player == null) {
                plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to clear the pack of player " + playerName + " but they aren't online?");
            }

            plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to clear the pack of player " + playerName);
            plugin.clearPack(playerUuid);
        });
        registerSubChannel("removePack", (p, in) -> {
            String playerName = in.readUTF();
            UUID playerUuid = new UUID(in.readLong(), in.readLong());

            ResourcePack pack = readPack(in);

            if (pack != null) {
                ResourcepacksPlayer player = plugin.getPlayer(playerUuid);
                if (player == null) {
                    plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to remove the pack " + pack.getName() + " of player " + playerName + " but they aren't online?");
                }
                plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to remove the pack " + pack.getName() + " from player " + playerName);
                plugin.getUserManager().removeUserPack(playerUuid, pack);
            } else {
                plugin.logDebug(plugin.getPlatformType().getOpposite() + " sent command to remove an unknown pack from " + playerName + "?");
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
        plugin.log(Level.WARNING, "Unknown subchannel " + subChannel + "! Please make sure you are running a compatible plugin version on your " + plugin.getPlatformType().getOpposite());
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
            plugin.log(Level.WARNING, "Can't send data to " + getTargetType() + " on channel " + subChannel + " as no authentication key is set!\n" +
                    "If you are using the plugin on both the proxy and the Minecraft servers as well then this is an error!" +
                    " Make sure the same key is set in the plugin's key.yml on the proxy and all your servers!\n" +
                    "If you are not using the plugin on the proxy then you can just ignore this warning!" +
                    " The key should be set automatically to empty on the next join." +
                    " (Otherwise just set an empty key yourself)");
            return;
        }
        if (key.isEmpty()) {
            plugin.logDebug("Not sending message to " + target + " on " + subChannel + " as we are not in an environment where we should (key is empty in key.yml)");
            return;
        }
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF(subChannel);
        dataOutput.writeLong(version);
        dataOutput.writeUTF(trustsSender() ? key : "");
        out.accept(dataOutput);
        sendPluginMessage(target, dataOutput.toByteArray());
    }

    /**
     * Write a pack to the output. If the pack is null, an empty string will be written
     *
     * @param out  The output to write to
     * @param pack The pack to write
     */
    public void writePack(ByteArrayDataOutput out, ResourcePack pack) {
        if (pack == null) {
            out.writeUTF("");
            return;
        }
        out.writeUTF(pack.getName());
        out.writeUTF(pack.getUrl());
        out.writeUTF(pack.getHash());
        out.writeLong(pack.getUuid() != null ? pack.getUuid().getMostSignificantBits() : 0);
        out.writeLong(pack.getUuid() != null ? pack.getUuid().getLeastSignificantBits() : 0);
    }

    /**
     * Read a pack from the input
     *
     * @param in The input to read from
     * @return The pack that was read, if the name is empty then the empty pack will be returned,
     *         if the pack is unknown then <code>null</code> is returned
     */
    protected ResourcePack readPack(ByteArrayDataInput in) {
        String packName = in.readUTF();
        if (packName.isEmpty()) {
            return plugin.getPackManager().getEmptyPack();
        }
        String packUrl = in.readUTF();
        String packHash = in.readUTF();
        UUID packUuid = new UUID(in.readLong(), in.readLong());
        if (packUuid.getLeastSignificantBits() == 0 && packUuid.getMostSignificantBits() == 0) {
            packUuid = null;
        }

        ResourcePack pack = plugin.getPackManager().getByName(packName);
        if (pack == null) {
            try {
                pack = new ResourcePack(packName, packUuid, packUrl, packHash);
                plugin.getPackManager().addPack(pack);
            } catch (IllegalArgumentException e) {
                pack = plugin.getPackManager().getByHash(packHash);
                if (pack == null) {
                    pack = plugin.getPackManager().getByUrl(packUrl);
                }
            }
        }
        return pack;
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

    /**
     * Get the type that we are sending messages to
     * @return The type
     */
    protected abstract String getTargetType();
}
