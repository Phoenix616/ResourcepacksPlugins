package de.themoep.resourcepacksplugin.bukkit;

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

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.commands.UsePackCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Phoenix616 on 20.06.2015.
 */
public class UsePackCommand implements CommandExecutor {

    private final ResourcepacksPlugin plugin;
    private final UsePackCommandExecutor usepackCommand;

    public UsePackCommand(WorldResourcepacks plugin) {
        this.plugin = plugin;
        usepackCommand = new UsePackCommandExecutor(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ResourcepacksPlayer s = null;
        if(sender instanceof Player) {
            s = plugin.getPlayer(((Player) sender).getUniqueId());
        }
        if(!usepackCommand.execute(s, args)) {
            plugin.sendMessage(s, "command.usepack.usage");
        }
        return true;
    }
}
