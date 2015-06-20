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
            if(args[0].equalsIgnoreCase("reload") && sender.hasPermission(plugin.getDescription().getName().toLowerCase() + ".command.reload")) {
                if(plugin.isEnabled()) {
                    boolean resend = args.length > 1 && "resend".equalsIgnoreCase(args[1]);
                    plugin.reloadConfig(resend);
                    sender.sendMessage(ChatColor.GREEN + "Reloaded " + plugin.getDescription().getName() + "' config!" + (resend ? " Resend packs to all online players!":""));
                } else {
                    sender.sendMessage(ChatColor.RED  + plugin.getDescription().getName() + " is not enabled!");
                }
            } else if(args[0].equalsIgnoreCase("version") && sender.hasPermission(plugin.getDescription().getName().toLowerCase() + ".command.version")) {
                sender.sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + "' version: " + plugin.getDescription().getVersion());
            }
        } else {
            sender.sendMessage("Usage: /" + getName() + " [reload|version]");
        }
    }
}
