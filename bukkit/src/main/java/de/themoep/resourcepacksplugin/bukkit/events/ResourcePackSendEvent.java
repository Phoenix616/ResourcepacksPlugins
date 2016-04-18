package de.themoep.resourcepacksplugin.bukkit.events;

import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSendEvent extends Event implements IResourcePackSendEvent, Cancellable {
    private final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private final UUID playerId;
    private ResourcePack pack;

    public ResourcePackSendEvent(UUID playerId, ResourcePack pack) {
        this.playerId = playerId;
        this.pack = pack;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ResourcePack getPack() {
        return pack;
    }

    public void setPack(ResourcePack pack) {
        this.pack = pack;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
