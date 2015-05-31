package de.themoep.BungeeResourcepacks.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
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
        if (args.length == 1) {
            if(args[0].equalsIgnoreCase("reload")) {
                if(plugin.isEnabled()) {
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Reloaded BungeeResourcepacks' config!");
                } else {
                    sender.sendMessage(ChatColor.RED + "BungeeResourcepacks is not enabled!");
                }
            } else if(args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(ChatColor.GREEN + "BungeeResourcepacks' version: " + plugin.getDescription().getVersion());
            }
        } else {
            sender.sendMessage("Usage: /brp [reload|version]");
        }
    }
}
