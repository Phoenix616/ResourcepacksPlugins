package de.themoep.BungeeResourcepacks.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by Phoenix616 on 17.04.2015.
 */
public class BungeeResouecepacksCommand extends Command {

    public BungeeResouecepacksCommand(BungeeResourcepacks plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!BungeeResourcepacks.getInstance().enabled) {
            sender.sendMessage(ChatColor.RED + "BungeeResourcepacks is not enabled!");
            return;
        }
        
        if (args.length == 1) {
            if(args[0].equalsIgnoreCase("reload")) {
                BungeeResourcepacks.getInstance().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Reloaded BungeeResourcepacks' config!");
                return;
            } else if(args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(ChatColor.GREEN + "BungeeResourcepacks' version: " + BungeeResourcepacks.getInstance().getDescription().getVersion());
                return;
            }
        }
        
        sender.sendMessage("Usage: /brp [reload|version]");
    }
}
