package de.themoep.resourcepacksplugin.bungee.packets;

public class IdMapping {
    private final String name;
    private final int protocolVersion;
    private final int packetId;

    public IdMapping(String name, int protocolVersion, int packetId) {
        this.name = name;
        this.protocolVersion = protocolVersion;
        this.packetId = packetId;
    }

    public String getName() {
        return name;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public int getPacketId() {
        return packetId;
    }
}
