package de.themoep.resourcepacksplugin.bungee.packets;

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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import de.themoep.resourcepacksplugin.bungee.BungeeResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;

import java.beans.ConstructorProperties;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ResourcePackSendPacket extends DefinedPacket {

    private String url;
    private Optional<String> hash = Optional.empty();
    private Optional<Boolean> forced = Optional.empty();
    private Optional<String> forcedMessage = Optional.empty();

    public ResourcePackSendPacket() {};

    private static Field conField = null;

    static {
        try {
            conField = DownstreamBridge.class.getDeclaredField("con");
            conField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            BungeeResourcepacks.getInstance().getLogger().log(Level.SEVERE, "Error while trying to get the UserConnection field from the DownstreamBridge object. Is the plugin up to date?");
        }
    }

    @ConstructorProperties({"url", "hash"})
    public ResourcePackSendPacket(String url, String hash) {
        this.url = url;
        if  (hash != null) {
            this.hash = Optional.of(hash.toLowerCase(Locale.ROOT));
        } else {
            this.hash = Optional.of(Hashing.sha1().hashString(this.getUrl(), Charsets.UTF_8).toString().toLowerCase(Locale.ROOT));
        }
    }

    @ConstructorProperties({"url", "hash", "force", "forcedMessage"})
    public ResourcePackSendPacket(String url, String hash, boolean forced, String forcedMessage) {
        this(url, hash);
        this.forced = Optional.of(forced);
        this.forcedMessage = Optional.of(forcedMessage);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        PacketWrapper packetWrapper = new PacketWrapper(this, Unpooled.copiedBuffer(ByteBuffer.allocate(Integer.toString(this.getUrl().length()).length())));
        if (handler instanceof DownstreamBridge) {
            if (conField != null) {
                DownstreamBridge bridge = (DownstreamBridge) handler;
                try {
                    UserConnection userConnection = (UserConnection) conField.get(bridge);
                    updatePlayer(userConnection);
                    userConnection.getPendingConnection().handle(packetWrapper);
                } catch (IllegalAccessException e) {
                    BungeeResourcepacks.getInstance().getLogger().log(Level.WARNING, "Sorry but you are not allowed to do this.", e);
                }
            }
        } else {
            BungeeResourcepacks.getInstance().logDebug("Sending ResourcePackSend packets to " + handler.getClass().getName() + " is not properly supported by this plugin! (Only players) Trying to handle anyways...");
            if (handler instanceof PacketHandler) {
                ((PacketHandler) handler).handle(packetWrapper);
            } else if (BungeeResourcepacks.getInstance().getLogLevel().intValue() >= Level.INFO.intValue()) {
                new UnsupportedOperationException("Unsupported handler type " + handler.getClass().getName()).fillInStackTrace().printStackTrace();
            }
        }
    }

    private void updatePlayer(UserConnection usercon) {
        BungeeResourcepacks plugin = BungeeResourcepacks.getInstance();
        if(plugin.isEnabled()) {
            ResourcePack pack = plugin.getPackManager().getByHash(getHash());
            if (pack == null) {
                pack = plugin.getPackManager().getByUrl(getUrl());
            }
            if (pack == null) {
                pack = new ResourcePack("backend-" + getUrl().substring(getUrl().lastIndexOf('/') + 1).replace(".zip", "").toLowerCase(Locale.ROOT), getUrl(), getHash());
                try {
                    plugin.getPackManager().addPack(pack);
                } catch (IllegalArgumentException e) {
                    // Can only happen when pack was gotten by hash but another pack had the same url
                    pack = plugin.getPackManager().getByUrl(getUrl());
                }
            }
            plugin.setBackend(usercon.getUniqueId());
            plugin.logDebug("Backend mc server send pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + usercon.getName());
            plugin.getUserManager().setUserPack(usercon.getUniqueId(), pack);
        }
    }

    public void read(ByteBuf buf) {
        this.url = readString(buf);
        try {
            this.hash = Optional.of(readString(buf));
            this.forced = Optional.of(buf.readBoolean());
            this.forcedMessage = Optional.of(readString(buf));
        } catch (IndexOutOfBoundsException ignored) {} // No hash
    }

    public void write(ByteBuf buf) {
        writeString(getUrl(), buf);
        this.hash.ifPresent(h -> writeString(h, buf));
        this.forced.ifPresent(buf::writeBoolean);
        this.forcedMessage.ifPresent(m -> writeString(m, buf));
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return this.hash.orElse(null);
    }

    @Override
    public String toString() {
        return "ResourcePackSendPacket{" +
                "url='" + url + '\'' +
                ", hash=" + hash +
                ", forced=" + forced +
                ", forcedMessage=" + forcedMessage +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourcePackSendPacket that = (ResourcePackSendPacket) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(forced, that.forced) &&
                Objects.equals(forcedMessage, that.forcedMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, hash, forced, forcedMessage);
    }
}
