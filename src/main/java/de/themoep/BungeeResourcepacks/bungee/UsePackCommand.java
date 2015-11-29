package de.themoep.BungeeResourcepacks.bungee;


import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by Phoenix616 on 20.06.2015.
 */
public class UsePackCommand extends Command {

    BungeeResourcepacks plugin;

    public UsePackCommand(BungeeResourcepacks plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            ResourcePack pack = plugin.getPackManager().getByName(args[0]);
            if (pack != null) {
                if (sender.hasPermission(plugin.getDescription().getName().toLowerCase() + ".pack." + pack.getName().toLowerCase())) {
                    ProxiedPlayer player = null;
                    if (args.length > 1 && sender.hasPermission(plugin.getDescription().getName().toLowerCase() + ".command.usepack.others")) {
                        player = plugin.getProxy().getPlayer(args[1]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + "The player " + args[1] + " is not online!");
                            return;
                        }
                    } else if(sender instanceof ProxiedPlayer) {
                        player = (ProxiedPlayer) sender;
                    } else {
                        sender.sendMessage("You have to specify a player if you want to run this command from the console! /usepack <packname> <playername>");
                        return;
                    }
                    ResourcePack prev = plugin.getPackManager().getUserPack(player.getUniqueId());
                    if (!pack.equals(prev)) {
                        plugin.setPack(player, pack);
                        if (sender != player) {
                            sender.sendMessage(player.getName() + " now uses the pack '" + pack.getName() + "'!");
                        }
                        player.sendMessage(ChatColor.GREEN + "You now use the pack '" + pack.getName() + "'!");
                        plugin.getLogger().log(plugin.loglevel, sender.getName() + " set the pack of " + player.getName() + " to '" + pack.getName() + "'!");
                    } else {
                        sender.sendMessage(ChatColor.RED + player.getName() + " already uses the pack '" + pack.getName() + "'!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission to set the pack '" + pack.getName() + "'!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Error: There is no pack with the name '" + args[0] + "'!");
            }
        } else {
            sender.sendMessage("Usage: /" + getName() + " <packname> [<playername>]");
        }
    }
}
