package de.themoep.resourcepacksplugin.bungee;

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.commands.UsePackCommandExecutor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by Phoenix616 on 20.06.2015.
 */
public class UsePackCommand extends Command {

    ResourcepacksPlugin plugin;

    public UsePackCommand(BungeeResourcepacks plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ResourcepacksPlayer s = null;
        if(sender instanceof ProxiedPlayer) {
            s = plugin.getPlayer(((ProxiedPlayer) sender).getUniqueId());
        }
        if(!new UsePackCommandExecutor(plugin).execute(s, args)) {
            sender.sendMessage("Usage: /usepack <packname> [<playername>]");
        }
    }
}
