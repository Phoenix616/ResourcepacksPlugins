package de.themoep.resourcepacksplugin.velocity.events;

/*
 * ResourcepacksPlugins - velocity
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
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

import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSelectEvent implements IResourcePackSelectEvent {
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
