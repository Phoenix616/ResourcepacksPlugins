package de.themoep.resourcepacksplugin.core;

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

import java.util.UUID;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPlayer {
    private String name;
    private UUID uniqueId;

    public ResourcepacksPlayer(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }


    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof ResourcepacksPlayer) {
            ResourcepacksPlayer other = (ResourcepacksPlayer) obj;
            return other.getUniqueId().equals(getUniqueId()) && other.getName().equals(getName());
        }
        return false;
    }
}
