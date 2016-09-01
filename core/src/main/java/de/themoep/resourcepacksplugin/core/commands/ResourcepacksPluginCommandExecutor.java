package de.themoep.resourcepacksplugin.core.commands;


import de.themoep.resourcepacksplugin.core.ChatColor;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPluginCommandExecutor extends PluginCommandExecutor {

    public ResourcepacksPluginCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin);
    }

    public boolean execute(ResourcepacksPlayer sender, String[] args) {
        if (args.length > 0) {
            if(args[0].equalsIgnoreCase("reload") && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.reload")) {
                if(plugin.isEnabled()) {
                    boolean resend = args.length > 1 && "resend".equalsIgnoreCase(args[1]);
                    plugin.reloadConfig(resend);
                    plugin.sendMessage(sender, ChatColor.GREEN + "Reloaded " + plugin.getName() + "' config!" + (resend ? " Resend packs to all online players!":""));
                } else {
                    plugin.sendMessage(sender, ChatColor.RED  + plugin.getName() + " is not enabled!");
                }
            } else if(args[0].equalsIgnoreCase("version") && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.version")) {
                plugin.sendMessage(sender, ChatColor.GREEN + plugin.getName() + "' version: " + plugin.getVersion());
            } else if ("generatehashes".equalsIgnoreCase(args[0]) && plugin.checkPermission(sender, plugin.getName().toLowerCase() + ".command.generatehashes")) {
                plugin.getPackManager().generateHashes(sender);
            }
            return true;
        }
        return false;
    }
}
