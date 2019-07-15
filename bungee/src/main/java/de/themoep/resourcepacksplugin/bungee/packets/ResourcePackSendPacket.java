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
import net.md_5.bungee.protocol.ProtocolConstants;

import java.beans.ConstructorProperties;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ResourcePackSendPacket extends DefinedPacket {

    private String url;
    private String hash;

    public final static List<IdMapping> ID_MAPPINGS = Arrays.asList(
            new IdMapping("1.8", ProtocolConstants.MINECRAFT_1_8, 0x48),
            new IdMapping("1.9", ProtocolConstants.MINECRAFT_1_9, 0x32),
            new IdMapping("1.12", ProtocolConstants.MINECRAFT_1_12, 0x33),
            new IdMapping("1.12.1", ProtocolConstants.MINECRAFT_1_12_1, 0x34),
            new IdMapping("1.13", ProtocolConstants.MINECRAFT_1_13, 0x37),
            new IdMapping("1.14", ProtocolConstants.MINECRAFT_1_14, 0x39)
    );

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
        if(hash != null) {
            this.hash = hash.toLowerCase();
        } else {
            this.hash = Hashing.sha1().hashString(url, Charsets.UTF_8).toString().toLowerCase();
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        PacketWrapper packetWrapper = new PacketWrapper(this, Unpooled.copiedBuffer(ByteBuffer.allocate(Integer.toString(this.getUrl().length()).length())));
        if (handler instanceof DownstreamBridge) {
            if (conField != null) {
                DownstreamBridge bridge = (DownstreamBridge) handler;
                try {
                    updatePlayer((UserConnection) conField.get(bridge));
                } catch (IllegalAccessException e) {
                    BungeeResourcepacks.getInstance().getLogger().log(Level.WARNING, "Sorry but you are not allowed to do this.", e);
                }
            }
        } else {
            BungeeResourcepacks.getInstance().getLogger().log(Level.WARNING, "Sending ResourcePackSend packets to " + handler.getClass().getName() + " is not properly supported by this plugin! (Only players) Trying to handle anyways...");
        }
        if (handler instanceof PacketHandler) {
            ((PacketHandler) handler).handle(packetWrapper);
        } else {
            new UnsupportedOperationException("Unsupported handler type!").fillInStackTrace().printStackTrace();
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
                pack = new ResourcePack("backend-" + getUrl().substring(getUrl().lastIndexOf('/') + 1).replace(".zip", "").toLowerCase(), getUrl(), getHash());
                try {
                    plugin.getPackManager().addPack(pack);
                } catch (IllegalArgumentException e) {
                    // Can only happen when pack was gotten by hash but another pack had the same url
                    pack = plugin.getPackManager().getByUrl(getUrl());
                }
            }
            plugin.setBackend(usercon.getUniqueId());
            plugin.getLogger().log(BungeeResourcepacks.getInstance().getLogLevel(), "Backend mc server send pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + usercon.getName());
            plugin.getUserManager().setUserPack(usercon.getUniqueId(), pack);
        }
    }

    public void read(ByteBuf buf) {
        this.url = readString(buf);
        try {
            this.hash = readString(buf);
        } catch (IndexOutOfBoundsException ignored) {} // No hash
    }

    public void write(ByteBuf buf) {
        writeString(this.url, buf);
        writeString(this.hash, buf);
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return this.hash;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHash(String hash) {
        if(hash != null) {
            this.hash = hash.substring(0, 39).toLowerCase();
        } else {
            this.hash = Hashing.sha1().hashString(this.getUrl(), Charsets.UTF_8).toString().substring(0, 39).toLowerCase();
        }
    }

    public String toString() {
        return "ResourcePackSend(url=" + this.getUrl() + ", hash=" + this.getHash() + ")";
    }

    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        } else if(obj instanceof ResourcePackSendPacket) {
            ResourcePackSendPacket other = (ResourcePackSendPacket)obj;
            String this$url = this.getUrl();
            String other$url = other.getUrl();
            if(this$url == null && other$url == null) {
                return true;
            }
            if(this$url == null || other$url == null) {
                return false;
            }
            if(!this$url.equals(other$url)) {
                return false;
            }
            String this$hash = this.getHash();
            String other$hash = other.getHash();

            if(this$hash == null && other$hash == null) {
                return true;
            }
            if(this$hash == null || other$hash == null) {
                return false;
            }
            return this$hash.equals(other$hash);
        }
        return false;
    }

    public int hashCode() {
        int result = 1;
        String $url = this.getUrl();
        result = result * 59 + ($url == null?0:$url.hashCode());
        String $hash = this.getHash();
        result = result * 59 + ($hash == null?0:$hash.hashCode());
        return result;
    }
}
