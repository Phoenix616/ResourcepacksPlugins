package de.themoep.resourcepacksplugin.core.commands;


import com.google.common.collect.ImmutableMap;
import de.themoep.resourcepacksplugin.core.ChatColor;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

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
                if (plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".pack." + pack.getName().toLowerCase())) {
                    ResourcepacksPlayer player = null;
                    if (args.length > 1 && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.usepack.others")) {
                        player = plugin.getPlayer(args[1]);
                        if (player == null) {
                            plugin.sendMessage(sender, ChatColor.RED + "The player " + args[1] + " is not online!");
                            return true;
                        }
                    } else if(sender != null) {
                        player = sender;
                    } else {
                        plugin.getLogger().warning("You have to specify a player if you want to run this command from the console! /usepack <packname> <playername>");
                        return true;
                    }
                    ResourcePack prev = plugin.getPackManager().getUserPack(player.getUniqueId());
                    if (!pack.equals(prev)) {
                        plugin.setPack(player.getUniqueId(), pack);
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
                    plugin.sendMessage(sender, ChatColor.RED + "You don't have the permission to set the pack '" + pack.getName() + "'!");
                }
            } else {
                plugin.sendMessage(sender, ChatColor.RED + "Error: There is no pack with the name '" + args[0] + "'!");
            }
            return true;
        }
        return false;
    }
}
