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
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSelectEvent extends AbstractEvent implements IResourcePackSelectEvent, TargetPlayerEvent {
    private final Player player;
    private final Cause cause;
    private List<ResourcePack> packs;
    private Status status;

    public ResourcePackSelectEvent(Player player, List<ResourcePack> packs, Cause cause) {
        this(player, packs, packs.isEmpty() ? Status.UNKNOWN : Status.SUCCESS, cause);
    }

    public ResourcePackSelectEvent(Player player, List<ResourcePack> packs, Status status, Cause cause) {
        this.player = player;
        this.packs = new ArrayList<>(packs);
        this.status = status;
        this.cause = cause;
    }

    @Override
    public UUID getPlayerId() {
        return player.getUniqueId();
    }

    @Override
    public List<ResourcePack> getPacks() {
        return null;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
        if (status != Status.SUCCESS) {
            packs.clear();
        }
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
