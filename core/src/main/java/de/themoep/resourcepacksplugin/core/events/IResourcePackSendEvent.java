package de.themoep.resourcepacksplugin.core.events;

import de.themoep.resourcepacksplugin.core.ResourcePack;

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public interface IResourcePackSendEvent {

    UUID getPlayerId();

    ResourcePack getPack();

    void setPack(ResourcePack pack);

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
