package de.themoep.resourcepacksplugin.core.events;

/*
 * ResourcepacksPlugins - core
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
