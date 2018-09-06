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
public interface IResourcePackSelectEvent {

    UUID getPlayerId();

    /**
     * Get the pack that was selected
     * @return The selected pack; null if the selection failed
     */
    ResourcePack getPack();

    /**
     * Set the pack. If it isn't null the status will be set to success. Otherwise you have to set the status yourself
     * @param pack The pack that was selected
     */
    void setPack(ResourcePack pack);

    /**
     * The status of the select event<br>
     * <tt>SUCCESS</tt> - Pack found and is not null<br>
     * <tt>NO_PERMISSION</tt> - Selection failed because the player does not have the permission for the pack<br>
     * <tt>WRONG_VERSION</tt> - Selection failed because there is not compatible pack<br>
     * <tt>NO_PERM_AND_WRONG_VERSION</tt> - Both failures happened<br>
     * <tt>UNKNOWN</tt> - We don't know why it failed<br>
     * @return The status of the event
     */
    Status getStatus();

    /**
     * Set the status. If it isn't SUCCESS the pack will be set to null
     * @param status The status of the select event
     */
    void setStatus(Status status);

    /**
     * The status of the select event<br>
     * <tt>SUCCESS</tt> - Pack found and is not null<br>
     * <tt>NO_PERMISSION</tt> - Selection failed because the player does not have the permission for the pack<br>
     * <tt>WRONG_VERSION</tt> - Selection failed because there is not compatible pack<br>
     * <tt>NO_PERM_AND_WRONG_VERSION</tt> - Both failures happened<br>
     * <tt>UNKNOWN</tt> - We don't know why it failed<br>
     */
    public enum Status {
        SUCCESS,
        NO_PERMISSION,
        WRONG_VERSION,
        NO_PERM_AND_WRONG_VERSION,
        UNKNOWN;
    }
}
