package de.themoep.resourcepacksplugin.bukkit.internal;

/*
 * ResourcepacksPlugins - bukkit
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
import org.bukkit.entity.Player;

/**
 * Created by Phoenix616 on 22.07.2016.
 */
public interface InternalHelper {

    void setResourcePack(Player player, ResourcePack pack);

    default void removeResourcePack(Player player, ResourcePack pack) {
        throw new UnsupportedOperationException("This version does not support removing resourcepacks!");
    }

    default void removeResourcePacks(Player player) {
        throw new UnsupportedOperationException("This version does not support removing resourcepacks!");
    }

}
