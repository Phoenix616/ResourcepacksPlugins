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

import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.events.IResourcePackSelectEvent.Status;

import java.util.logging.Level;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResetPackCommandExecutor extends PluginCommandExecutor {

    public ResetPackCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin, "resetpack [<playername>] [<temp>]");
    }

    public boolean run(ResourcepacksPlayer sender, String[] args) {
        String tempStr = null;
        if (args.length > 0 && plugin.checkPermission(sender, permission + ".temporary")) {
            tempStr = args[args.length - 1];
        }
        boolean temp = plugin.isUsepackTemporary();
        if ("false".equalsIgnoreCase(tempStr)) {
            temp = false;
        } else if ("true".equalsIgnoreCase(tempStr)) {
            temp = true;
        } else if (tempStr != null && args.length > 1) {
            sendMessage(sender, "invalid-temporary", "input", tempStr);
            return true;
        } else {
            // temporary value is not true or false
            tempStr = null;
        }

        ResourcepacksPlayer player = null;
        if ((args.length > 1 || (tempStr == null && args.length > 0)) && plugin.checkPermission(sender, permission + ".others")) {
            player = plugin.getPlayer(args[0]);
            if (player == null) {
                sendMessage(sender, "player-not-online", "input", args[0]);
                return true;
            }
        } else if (sender != null) {
            player = sender;
        } else {
            plugin.log(Level.WARNING, "You have to specify a player if you want to run this command from the console! /resetpack <playername> [<temp>]");
            return true;
        }

        if (plugin.getPlayerClientType(player.getUniqueId()) == ClientType.BEDROCK) {
            sendMessage(sender, "bedrock-not-supported");
            return true;
        }

        if (!temp) {
            String storedPack = plugin.getStoredPack(player.getUniqueId());
            if (storedPack == null) {
                sendMessage(sender, "no-pack-stored");
            } else {
                sendMessage(sender, "had-stored-pack", "pack", storedPack);
            }
        }

        if (plugin.getPackManager().setPack(player.getUniqueId(), null, temp) == Status.SUCCESS) {
            if (!player.equals(sender)) {
                sendMessage(sender, "success-other", "player", player.getName());
            }
            sendMessage(player, "success");
            String senderName = sender != null ? sender.getName() : "CONSOLE";
            plugin.logDebug(senderName + " reset the pack of " + player.getName());
        } else {
            if (player.equals(sender)) {
                sendMessage(sender, "no-pack");
            } else {
                sendMessage(sender, "no-pack-other", "player", player.getName());
            }
        }
        return true;
    }
}
