package de.themoep.resourcepacksplugin.bungee;

/*
 * ResourcepacksPlugins - bungee
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
import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ForwardingCommand extends Command {
    private final PluginCommandExecutor executor;

    public ForwardingCommand(PluginCommandExecutor executor) {
        super(executor.getName(), executor.getPermission(), executor.getAliases());
        this.executor = executor;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ResourcepacksPlayer s = null;
        if(sender instanceof ProxiedPlayer) {
            s = executor.getPlugin().getPlayer(((ProxiedPlayer) sender).getUniqueId());
        }
        executor.execute(s, args);
    }
}
