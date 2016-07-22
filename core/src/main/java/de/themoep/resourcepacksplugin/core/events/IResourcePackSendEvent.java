package de.themoep.resourcepacksplugin.core.events;

import de.themoep.resourcepacksplugin.core.ResourcePack;

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public interface IResourcePackSendEvent {

    UUID getPlayerId();

    ResourcePack getPack();

    /**
     * Set the pack that should be send. Set to null if you want to remove all packs/send the empty one
     * @param pack The pack to send, if it is null it will reset the pack to the empty one
     */
    void setPack(ResourcePack pack);

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
