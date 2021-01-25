package de.themoep.resourcepacksplugin.sponge.listeners;

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

import com.google.common.io.ByteArrayDataInput;
import org.spongepowered.api.entity.living.player.Player;

/**
 * A reaction onto a message on a certain sub channel
 */
public abstract class ProxyPackReaction {

    /**
     * Execute this action
     * @param player    The player that received the plugin message
     * @param data      The data as an input stream that you can read from
     */
    public abstract void execute(Player player, ByteArrayDataInput data);
}
