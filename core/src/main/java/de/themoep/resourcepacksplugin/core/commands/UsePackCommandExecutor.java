package de.themoep.resourcepacksplugin.core.commands;


import com.google.common.collect.ImmutableMap;
import de.themoep.resourcepacksplugin.core.ChatColor;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.ArrayList;
import java.util.List;

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
                if (plugin.checkPermission(sender, pack.getPermission())) {
                    String tempStr = null;
                    if (args.length > 1 && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.usepack.temporary")) {
                        tempStr = args[args.length -1];
                    }
                    boolean temp = false;
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
                    plugin.sendMessage(sender, ChatColor.RED + "You don't have the permission " + pack.getPermission() + " to set the pack '" + pack.getName() + "'!");
                }
            } else {
                plugin.sendMessage(sender, ChatColor.RED + "Error: There is no pack with the name '" + args[0] + "'!");
            }
        } else {
            plugin.sendMessage(sender, ChatColor.GREEN + plugin.getMessage("packlisthead"));
            List<ResourcePack> packs = plugin.getPackManager().getPacks();
            if(packs.size() > 0) {
                ResourcePack userPack = sender != null ? plugin.getUserManager().getUserPack(sender.getUniqueId()) : null;
                List<ResourcePack> applicablePacks = new ArrayList<>();
                for(ResourcePack pack : packs) {
                    if(sender == null || pack.getFormat() <= plugin.getPlayerPackFormat(sender.getUniqueId()) && plugin.checkPermission(sender, pack.getPermission())) {
                        applicablePacks.add(pack);
                    }
                }
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
