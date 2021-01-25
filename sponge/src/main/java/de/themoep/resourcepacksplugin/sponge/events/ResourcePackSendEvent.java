package de.themoep.resourcepacksplugin.sponge.events;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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

import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSendEvent extends AbstractEvent implements IResourcePackSendEvent, TargetPlayerEvent, Cancellable {

    private boolean cancelled = false;
    private final Player player;
    private ResourcePack pack;
    private final Cause cause;

    public ResourcePackSendEvent(Player player, ResourcePack pack, Cause cause) {
        this.player = player;
        this.pack = pack;
        this.cause = cause;
    }

    public UUID getPlayerId() {
        return player.getUniqueId();
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
    public Player getTargetEntity() {
        return player;
    }

    @Override
    public Cause getCause() {
        return cause;
    }
}
