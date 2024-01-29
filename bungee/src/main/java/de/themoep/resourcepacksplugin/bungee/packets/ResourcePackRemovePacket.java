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
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.beans.ConstructorProperties;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ResourcePackRemovePacket extends DefinedPacket {

    private Optional<UUID> uuid = Optional.empty();

    public ResourcePackRemovePacket() {};

    private static Field conField = null;

    static {
        try {
            conField = DownstreamBridge.class.getDeclaredField("con");
            conField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            BungeeResourcepacks.getInstance().getLogger().log(Level.SEVERE, "Error while trying to get the UserConnection field from the DownstreamBridge object. Is the plugin up to date?");
        }
    }

    @ConstructorProperties({"uuid"})
    public ResourcePackRemovePacket(UUID uuid) {
        this.uuid = Optional.of(uuid);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        if (handler instanceof DownstreamBridge) {
            if (conField != null) {
                DownstreamBridge bridge = (DownstreamBridge) handler;
                try {
                    UserConnection userConnection = (UserConnection) conField.get(bridge);
                    updatePlayer(userConnection);
                    PacketWrapper packetWrapper = new PacketWrapper(this, Unpooled.copiedBuffer(ByteBuffer.allocate(Short.MAX_VALUE)), userConnection.getCh().getEncodeProtocol());
                    userConnection.getPendingConnection().handle(packetWrapper);
                } catch (IllegalAccessException e) {
                    BungeeResourcepacks.getInstance().getLogger().log(Level.WARNING, "Sorry but you are not allowed to do this.", e);
                }
            }
        } else {
            BungeeResourcepacks.getInstance().logDebug("Sending ResourcePackSend packets to " + handler.getClass().getName() + " is not properly supported by this plugin! (Only players) Trying to handle anyways...");
            if (handler instanceof PacketHandler) {
                ((PacketHandler) handler).handle(new PacketWrapper(this, Unpooled.copiedBuffer(ByteBuffer.allocate(Short.MAX_VALUE)), Protocol.GAME));
            } else if (BungeeResourcepacks.getInstance().getLogLevel().intValue() >= Level.INFO.intValue()) {
                new UnsupportedOperationException("Unsupported handler type " + handler.getClass().getName()).fillInStackTrace().printStackTrace();
            }
        }
    }

    private void updatePlayer(UserConnection usercon) {
        BungeeResourcepacks plugin = BungeeResourcepacks.getInstance();
        if (plugin.isEnabled()) {
            if (uuid.isPresent()) {
                ResourcePack pack = plugin.getPackManager().getByUuid(uuid.get());
                if (pack != null) {
                    plugin.getUserManager().removeUserPack(usercon.getUniqueId(), pack);
                    plugin.logDebug("Backend mc server removed pack " + pack.getName() + " (" + pack.getUrl() + ") from player " + usercon.getName());
                } else {
                    plugin.logDebug("Backend mc server removed pack " + uuid.get() + " that we don't know about from player " + usercon.getName());
                }
            } else {
                plugin.getUserManager().clearUserPacks(usercon.getUniqueId());
            }
        }
    }

    public void read(ByteBuf buf) {
        boolean hasUuid = buf.readBoolean();
        if (hasUuid) {
            this.uuid = Optional.of(readUUID(buf));
        }
    }

    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        read(buf);
    }

    public void write(ByteBuf buf) {
        buf.writeBoolean(this.uuid.isPresent());
        this.uuid.ifPresent(u -> writeUUID(u, buf));
    }

    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        write(buf);
    }

    public UUID getUuid() {
        return this.uuid.orElse(null);
    }

    @Override
    public String toString() {
        return "ResourcePackRemovePacket{" +
                "uuid=" + uuid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourcePackRemovePacket that = (ResourcePackRemovePacket) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
