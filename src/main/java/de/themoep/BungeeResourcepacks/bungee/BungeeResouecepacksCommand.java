package de.themoep.BungeeResourcepacks.bungee;

import de.themoep.BungeeResourcepacks.core.ResourcePack;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by Phoenix616 on 17.04.2015.
 */
public class BungeeResouecepacksCommand extends Command {
    
    BungeeResourcepacks plugin;

    public BungeeResouecepacksCommand(BungeeResourcepacks plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("bungeeresourcepacks.command.reload")) {
                if(plugin.isEnabled()) {
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Reloaded BungeeResourcepacks' config!");
                } else {
                    sender.sendMessage(ChatColor.RED + "BungeeResourcepacks is not enabled!");
                }
            } else if(args[0].equalsIgnoreCase("version") && sender.hasPermission("bungeeresourcepacks.command.version")) {
                sender.sendMessage(ChatColor.GREEN + "BungeeResourcepacks' version: " + plugin.getDescription().getVersion());
            } else if(sender instanceof ProxiedPlayer && sender.hasPermission("bungeeresourcepacks.command.usepack")) {
                ResourcePack pack = plugin.getPackManager().getByName(args[0]);
                if(pack != null) {
                    if(sender.hasPermission("bungeeresourcepacks.pack." + pack.getName().toLowerCase())) {
                        ProxiedPlayer player = (ProxiedPlayer) sender;
                        if(args.length > 1 && sender.hasPermission("bungeeresourcepacks.command.usepack.others")) {
                            player = plugin.getProxy().getPlayer(args[1]);
                            if(player == null) {
                                sender.sendMessage(ChatColor.RED + "The player " + args[1] + " is not online!");
                                return;
                            }
                        }
                        ResourcePack prev = plugin.getPackManager().getUserPack(player.getUniqueId());
                        if(!pack.equals(prev)) {
                            plugin.setPack(player, pack);
                            player.sendMessage(ChatColor.GREEN + "You know use the pack '" + pack.getName() + "'!");
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
            }
        } else {
            sender.sendMessage("Usage: /brp [<pack>|reload|version]");
        }
    }
}
