package de.themoep.resourcepacksplugin.bungee.events;

/*
 * ResourcepacksPlugins - bungee
 * Copyright (C) 2018 Max Lee aka Phoenix616 (mail@moep.tv)
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
import net.md_5.bungee.api.plugin.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSelectEvent extends Event implements IResourcePackSelectEvent {
    private final UUID playerId;
    private List<ResourcePack> packs;
    private Status status;

    public ResourcePackSelectEvent(UUID playerId, ResourcePack pack) {
        this.playerId = playerId;
        setPack(pack);
    }

    public ResourcePackSelectEvent(UUID playerId, List<ResourcePack> packs, Status status) {
        this.playerId = playerId;
        this.packs = new ArrayList<>(packs);
        this.status = status;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public List<ResourcePack> getPacks() {
        return packs;
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
}
