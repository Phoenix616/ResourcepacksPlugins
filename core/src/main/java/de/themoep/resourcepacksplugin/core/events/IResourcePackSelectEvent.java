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

import java.util.List;
import java.util.UUID;

/**
 * Created by Phoenix616 on 18.04.2015.
 */
public interface IResourcePackSelectEvent {

    UUID getPlayerId();

    /**
     * Get the pack that was selected
     * @return The selected pack; null if the selection failed
     * @deprecated Use {@link #getPacks()}
     */
    @Deprecated
    default ResourcePack getPack() {
        return !getPacks().isEmpty() ? getPacks().get(0) : null;
    }

    /**
     * Get the packs that were selected
     * @return The selected packs; null if the selection failed
     */
    List<ResourcePack> getPacks();

    /**
     * Set the pack. If it isn't null the status will be set to success. Otherwise you have to set the status yourself
     * @param pack The pack that was selected
     * @deprecated Directly add to or remove from {@link #getPacks()}
     */
    @Deprecated
    default void setPack(ResourcePack pack) {
        getPacks().clear();
        if (pack != null) {
            getPacks().add(pack);
        }
    }

    /**
     * The status of the select event<br>
     * <code>SUCCESS</code> - Pack found and is not null<br>
     * <code>NO_PERMISSION</code> - Selection failed because the player does not have the permission for the pack<br>
     * <code>WRONG_VERSION</code> - Selection failed because there is not compatible pack<br>
     * <code>NO_PERM_AND_WRONG_VERSION</code> - Both failures happened<br>
     * <code>UNKNOWN</code> - We don't know why it failed<br>
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
     * <code>SUCCESS</code> - Pack found and is not null<br>
     * <code>NO_PERMISSION</code> - Selection failed because the player does not have the permission for the pack<br>
     * <code>WRONG_VERSION</code> - Selection failed because there is not compatible pack<br>
     * <code>NO_PERM_AND_WRONG_VERSION</code> - Both failures happened<br>
     * <code>UNKNOWN</code> - We don't know why it failed<br>
     */
    public enum Status {
        SUCCESS,
        NO_PERMISSION,
        WRONG_VERSION,
        NO_PERM_AND_WRONG_VERSION,
        UNKNOWN;
    }
}
