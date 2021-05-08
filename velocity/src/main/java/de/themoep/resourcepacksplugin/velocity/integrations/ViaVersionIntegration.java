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
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.util.UUID;
import java.util.logging.Level;

public class ViaVersionIntegration {
    private final ViaAPI viaApi;

    public ViaVersionIntegration(VelocityResourcepacks plugin, PluginContainer viaPlugin) {
        viaApi = Via.getAPI();
        plugin.log(Level.INFO, "Detected " + viaPlugin.getDescription().getName().orElse(viaPlugin.getDescription().getId()) + " " + viaPlugin.getDescription().getVersion().orElse(""));
    }

    public int getPlayerVersion(UUID playerId) {
        return viaApi.getPlayerVersion(playerId);
    }
}
