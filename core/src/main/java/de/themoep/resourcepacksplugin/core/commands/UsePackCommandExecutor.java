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

import com.google.common.collect.ImmutableMap;
import de.themoep.resourcepacksplugin.core.ChatColor;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class UsePackCommandExecutor extends PluginCommandExecutor {

    public UsePackCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin);
    }

    public boolean execute(ResourcepacksPlayer sender, String[] args) {
        if (args.length > 0) {
            ResourcePack pack = plugin.getPackManager().getByName(args[0]);
            if (pack != null) {
                if (!pack.isRestricted() || plugin.checkPermission(sender, pack.getPermission())) {
                    String tempStr = null;
                    if (args.length > 1 && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.usepack.temporary")) {
                        tempStr = args[args.length -1];
                    }
                    boolean temp = plugin.isUsepackTemporary();
                    if ("false".equalsIgnoreCase(tempStr)) {
                        temp = false;
                    } else if ("true".equalsIgnoreCase(tempStr)) {
                        temp = true;
                    } else if (tempStr != null && args.length > 2) {
                        plugin.sendMessage(sender, ChatColor.RED + tempStr + " is not a valid Boolean temporary value! (true/false)");
                        return true;
                    } else {
                        // temporary value is not true or false
                        tempStr = null;
                    }

                    ResourcepacksPlayer player = null;
                    if ((args.length > 2 || (tempStr == null && args.length > 1)) && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.usepack.others")) {
                        player = plugin.getPlayer(args[1]);
                        if (player == null) {
                            plugin.sendMessage(sender, ChatColor.RED + "The player " + args[1] + " is not online!");
                            return true;
                        }
                    } else if(sender != null) {
                        player = sender;
                    } else {
                        plugin.getLogger().warning("You have to specify a player if you want to run this command from the console! /usepack <packname> <playername> [<temp>]");
                        return true;
                    }
                    if (plugin.getPackManager().setPack(player.getUniqueId(), pack, temp)) {
                        if (!player.equals(sender)) {
                            plugin.sendMessage(sender, args[1] + " now uses the pack '" + pack.getName() + "'!");
                        }
                        plugin.sendMessage(player, ChatColor.GREEN + plugin.getMessage("usepack", ImmutableMap.of("pack", pack.getName())));
                        String senderName = sender != null ? sender.getName() : "CONSOLE";
                        plugin.getLogger().log(plugin.getLogLevel(), senderName + " set the pack of " + player.getName() + " to '" + pack.getName() + "'!");
                    } else {
                        plugin.sendMessage(sender, ChatColor.RED + player.getName() + " already uses the pack '" + pack.getName() + "'!");
                    }
                } else {
                    plugin.sendMessage(sender, ChatColor.RED + "You don't have the permission " + pack.getPermission() + " to set the restricted pack '" + pack.getName() + "'!");
                }
            } else {
                plugin.sendMessage(sender, ChatColor.RED + "Error: There is no pack with the name '" + args[0] + "'!");
            }
        } else {
            plugin.sendMessage(sender, ChatColor.GREEN + plugin.getMessage("packlisthead"));
            List<ResourcePack> packs = plugin.getPackManager().getPacks();
            if(packs.size() > 0) {
                ResourcePack userPack = sender != null ? plugin.getUserManager().getUserPack(sender.getUniqueId()) : null;
                List<ResourcePack> applicablePacks = sender == null ? packs : packs.stream()
                        .filter(pack -> pack.getFormat() <= plugin.getPlayerPackFormat(sender.getUniqueId())
                                && (!pack.isRestricted() || plugin.checkPermission(sender, pack.getPermission())))
                        .collect(Collectors.toList());
                
                if(applicablePacks.size() > 0) {
                    for(ResourcePack pack : applicablePacks) {
                        String msg = pack.getName();
                        if(userPack != null && userPack.equals(pack)) {
                            msg = ">" + msg;
                        } else {
                            msg = " " + msg;
                        }
                        if(pack.getFormat() > 0) {
                            msg += ChatColor.GRAY + " (Format: " + pack.getFormat() + ")";
                        }
                        plugin.sendMessage(sender, ChatColor.YELLOW + msg);
                    }
                    return false;
                }
            }
            plugin.sendMessage(sender, ChatColor.RED + " " + plugin.getMessage("nopacks"));
        }
        return true;
    }
}
