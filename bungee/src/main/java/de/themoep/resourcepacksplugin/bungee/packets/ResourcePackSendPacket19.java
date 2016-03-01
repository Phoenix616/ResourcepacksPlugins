package de.themoep.resourcepacksplugin.bungee.packets;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;

public class ResourcePackSendPacket19 extends ResourcePackSendPacket {

    public ResourcePackSendPacket19(String url, String hash) {
        super(url, hash);
    }

    @Override
    public void relayPacket(UserConnection usercon, PacketWrapper packet) throws Exception {
        if(usercon.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9) {
            super.relayPacket(usercon, packet);
        } else {
            usercon.getPendingConnection().handle(packet);
        }
    }
}
