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

import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class UsePackCommandExecutor extends PluginCommandExecutor {

    public UsePackCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin, "usepack <packname> [<playername>] [<temp>]");
    }

    public boolean run(ResourcepacksPlayer sender, String[] args) {
        if (args.length > 0) {
            ResourcePack pack = plugin.getPackManager().getByName(args[0]);
            if (pack != null) {
                if (!pack.isRestricted() || plugin.checkPermission(sender, pack.getPermission())) {
                    String tempStr = null;
                    if (args.length > 1 && plugin.checkPermission(sender, permission + ".temporary")) {
                        tempStr = args[args.length - 1];
                    }
                    boolean temp = plugin.isUsepackTemporary();
                    if ("false".equalsIgnoreCase(tempStr)) {
                        temp = false;
                    } else if ("true".equalsIgnoreCase(tempStr)) {
                        temp = true;
                    } else if (tempStr != null && args.length > 2) {
                        sendMessage(sender, "invalid-temporary", "input", tempStr);
                        return true;
                    } else {
                        // temporary value is not true or false
                        tempStr = null;
                    }

                    ResourcepacksPlayer player = null;
                    if ((args.length > 2 || (tempStr == null && args.length > 1)) && plugin.checkPermission(sender, permission + ".others")) {
                        player = plugin.getPlayer(args[1]);
                        if (player == null) {
                            sendMessage(sender, "player-not-online", "input", args[1]);
                            return true;
                        }
                    } else if (sender != null) {
                        player = sender;
                    } else {
                        plugin.log(Level.WARNING, "You have to specify a player if you want to run this command from the console! /usepack <packname> <playername> [<temp>]");
                        return true;
                    }
                    switch (plugin.getPackManager().setPack(player.getUniqueId(), pack, temp)) {
                        case SUCCESS:
                            if (!player.equals(sender)) {
                                sendMessage(sender, "success-other", "player", player.getName(), "pack", pack.getName());
                            }
                            sendMessage(player, "success", "pack", pack.getName());
                            String senderName = sender != null ? sender.getName() : "CONSOLE";
                            plugin.logDebug(senderName + " set the pack of " + player.getName() + " to '" + pack.getName() + "'!");
                            break;
                        case NO_PERMISSION:
                            sendMessage(sender, "no-variant-found.permission", "player", player.getName(), "pack", pack.getName());
                            break;
                        case WRONG_VERSION:
                            sendMessage(sender, "no-variant-found.version", "player", player.getName(), "pack", pack.getName());
                            break;
                        case NO_PERM_AND_WRONG_VERSION:
                            sendMessage(sender, "no-variant-found.perm-and-version", "player", player.getName(), "pack", pack.getName());
                            break;
                        case UNKNOWN:
                            sendMessage(sender, "already-in-use", "player", player.getName(), "pack", pack.getName());
                            break;
                    }
                } else {
                    sendMessage(sender, "restricted", "permission", pack.getPermission(), "pack", pack.getName());
                }
            } else {
                sendMessage(sender, "unknown-pack", "input", args[0]);
            }
        } else {
            sendMessage(sender, "pack-list.head");
            List<ResourcePack> packs = plugin.getPackManager().getPacks();
            if (packs.size() > 0) {
                ResourcePack userPack = sender != null ? plugin.getUserManager().getUserPack(sender.getUniqueId()) : null;
                List<ResourcePack> applicablePacks = sender == null ? packs : packs.stream()
                        .filter(pack -> pack.getFormat() <= plugin.getPlayerPackFormat(sender.getUniqueId())
                                && pack.getVersion() <= plugin.getPlayerProtocol(sender.getUniqueId())
                                && (!pack.isRestricted() || plugin.checkPermission(sender, pack.getPermission())))
                        .collect(Collectors.toList());

                if (applicablePacks.size() > 0) {
                    for (ResourcePack pack : applicablePacks) {
                        sendMessage(sender, "pack-list.entry" + (userPack != null && userPack.equals(pack) ? "-selected" : ""),
                                "pack", pack.getName(),
                                "hash", pack.getHash(),
                                "url", pack.getUrl(),
                                "format", String.valueOf(pack.getFormat()),
                                "version", String.valueOf(pack.getVersion()),
                                "selected", userPack != null && userPack.equals(pack) ? ">" : " ",
                                "optional-format", pack.getFormat() > 0 ? plugin.getMessage(sender, "command.usepack.pack-list.optional-format", "format", String.valueOf(pack.getFormat())) : "",
                                "optional-version", pack.getVersion() > 0 ? plugin.getMessage(sender, "command.usepack.pack-list.optional-version", "version", String.valueOf(pack.getVersion())) : ""
                        );
                    }
                    return false;
                }
            }
            sendMessage(sender, "no-packs");
        }
        return true;
    }
}
