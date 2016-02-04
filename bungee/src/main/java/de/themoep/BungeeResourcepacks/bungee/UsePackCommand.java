package de.themoep.BungeeResourcepacks.bungee;

import de.themoep.BungeeResourcepacks.core.ResourcepacksPlayer;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlugin;
import de.themoep.BungeeResourcepacks.core.commands.UsePackExecutor;
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
        new UsePackExecutor(plugin).execute(s, args);
    }
}
