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

import com.velocitypowered.api.event.ResultedEvent;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSendEvent;

import java.util.UUID;


/**
 * Created by Phoenix616 on 18.04.2015.
 */
public class ResourcePackSendEvent implements IResourcePackSendEvent, ResultedEvent<ResultedEvent.GenericResult> {

    private final UUID playerId;
    private ResourcePack pack;
    private GenericResult result = GenericResult.allowed();

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
        return !result.isAllowed();
    }

    public void setCancelled(boolean cancelled) {
        this.result = cancelled ? GenericResult.denied() : GenericResult.allowed();
    }

    @Override
    public GenericResult getResult() {
        return result;
    }

    @Override
    public void setResult(GenericResult result) {
        this.result = result;
    }
}
