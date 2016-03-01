package de.themoep.resourcepacksplugin.bungee.packets;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;

public class ResourcePackSendPacket18 extends ResourcePackSendPacket {

    public ResourcePackSendPacket18(String url, String hash) {
        super(url, hash);
    }

    @Override
    public void relayPacket(UserConnection usercon, PacketWrapper packet) throws Exception {
        if(usercon.getPendingConnection().getVersion() == ProtocolConstants.MINECRAFT_1_8) {
            super.relayPacket(usercon, packet);
        } else {
            usercon.getPendingConnection().handle(packet);
        }
    }
}
