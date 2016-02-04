package de.themoep.BungeeResourcepacks.core.commands;


import com.google.common.collect.ImmutableMap;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlayer;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlugin;
import net.md_5.bungee.api.ChatColor;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class UsePackExecutor extends PluginCommandExecutor {

    public UsePackExecutor(ResourcepacksPlugin plugin) {
        super(plugin);
    }

    public void execute(ResourcepacksPlayer sender, String[] args) {
        if (args.length > 0) {
            ResourcePack pack = plugin.getPackManager().getByName(args[0]);
            if (pack != null) {
                if (plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".pack." + pack.getName().toLowerCase())) {
                    ResourcepacksPlayer player = null;
                    if (args.length > 1 && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.usepack.others")) {
                        player = plugin.getPlayer(args[1]);
                        if (player == null) {
                            plugin.sendMessage(sender, ChatColor.RED + "The player " + args[1] + " is not online!");
                            return;
                        }
                    } else if(sender != null) {
                        player = sender;
                    } else {
                        plugin.getLogger().warning("You have to specify a player if you want to run this command from the console! /usepack <packname> <playername>");
                        return;
                    }
                    ResourcePack prev = plugin.getPackManager().getUserPack(player.getUniqueId());
                    if (!pack.equals(prev)) {
                        plugin.setPack(player.getUniqueId(), pack);
                        if (!player.equals(sender)) {
                            plugin.sendMessage(sender, args[1] + " now uses the pack '" + pack.getName() + "'!");
                        }
                        plugin.sendMessage(player, ChatColor.GREEN + plugin.getMessage("usepack", ImmutableMap.of("pack", pack.getName())));
                        plugin.getLogger().log(plugin.getLogLevel(), sender.getName() + " set the pack of " + player.getName() + " to '" + pack.getName() + "'!");
                    } else {
                        plugin.sendMessage(sender, ChatColor.RED + player.getName() + " already uses the pack '" + pack.getName() + "'!");
                    }
                } else {
                    plugin.sendMessage(sender, ChatColor.RED + "You don't have the permission to set the pack '" + pack.getName() + "'!");
                }
            } else {
                plugin.sendMessage(sender, ChatColor.RED + "Error: There is no pack with the name '" + args[0] + "'!");
            }
        } else {
            plugin.sendMessage(sender, "Usage: /usepack <packname> [<playername>]");
        }
    }
}
