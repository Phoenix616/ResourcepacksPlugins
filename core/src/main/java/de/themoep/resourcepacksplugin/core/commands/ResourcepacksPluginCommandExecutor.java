package de.themoep.resourcepacksplugin.core.commands;


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

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPluginCommandExecutor extends PluginCommandExecutor {

    public ResourcepacksPluginCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin);
    }

    public boolean execute(ResourcepacksPlayer sender, String[] args) {
        if (args.length > 0) {
            if(args[0].equalsIgnoreCase("reload") && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.reload")) {
                if(plugin.isEnabled()) {
                    boolean resend = args.length > 1 && "resend".equalsIgnoreCase(args[1]);
                    plugin.reloadConfig(resend);
                    plugin.sendMessage(sender, "command.reloaded",
                            "plugin", plugin.getName(),
                            "optional-resend", resend ? plugin.getMessage(sender, "command.optional-resend") : ""
                    );
                } else {
                    plugin.sendMessage(sender, "command.not-enabled", "plugin", plugin.getName());
                }
            } else if(args[0].equalsIgnoreCase("version") && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.version")) {
                plugin.sendMessage(sender, "command.version",
                        "plugin", plugin.getName(),
                        "version", plugin.getVersion()
                );
            } else if ("generatehashes".equalsIgnoreCase(args[0]) && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.generatehashes")) {
                plugin.getPackManager().generateHashes(sender);
            }
            return true;
        }
        plugin.sendMessage(sender, "command.usage", "command", plugin.getName().charAt(0) + "rp");
        return false;
    }
}
