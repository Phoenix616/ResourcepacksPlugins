package de.themoep.resourcepacksplugin.bungee.events;

import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSelectEvent extends Event implements IResourcePackSelectEvent {
    private final UUID playerId;
    private ResourcePack pack;
    private Status status;

    public ResourcePackSelectEvent(UUID playerId, ResourcePack pack) {
        this.playerId = playerId;
        setPack(pack);
    }

    public ResourcePackSelectEvent(UUID playerId, ResourcePack pack, Status status) {
        this.playerId = playerId;
        this.pack = pack;
        this.status = status;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ResourcePack getPack() {
        return pack;
    }

    public void setPack(ResourcePack pack) {
        this.pack = pack;
        if(pack != null) {
            status = Status.SUCCESS;
        } else {
            status = Status.UNKNOWN;
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
        if(status != Status.SUCCESS) {
            pack = null;
        }
    }
}
