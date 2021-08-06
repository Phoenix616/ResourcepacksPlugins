package de.themoep.resourcepacksplugin.velocity.integrations;

/*
 * ResourcepacksPlugins - VelocityResourcepacks
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

import com.velocitypowered.api.plugin.PluginContainer;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.logging.Level;

public class FloodgateIntegration {

    public FloodgateIntegration(VelocityResourcepacks plugin, PluginContainer floodgatePlugin) {
        plugin.log(Level.INFO, "Detected " + floodgatePlugin.getDescription().getName().orElse(floodgatePlugin.getDescription().getId()) + " " + floodgatePlugin.getDescription().getVersion().orElse(""));
    }

    public boolean hasPlayer(UUID playerId) {
        return FloodgateApi.getInstance().getPlayer(playerId) != null;
    }
}
