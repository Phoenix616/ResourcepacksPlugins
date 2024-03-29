package de.themoep.resourcepacksplugin.bukkit.internal;

/*
 * ResourcepacksPlugins - bukkit-v1_9_R1
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

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Created by Phoenix616 on 22.07.2016.
 */
public class InternalHelper_v1_9_R1 implements InternalHelper {

    private WorldResourcepacks plugin;

    public InternalHelper_v1_9_R1(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setResourcePack(Player player, ResourcePack pack) {
        ((CraftPlayer) player).getHandle().setResourcePack(plugin.getPackManager().getPackUrl(pack), pack.getHash());
    }
}
