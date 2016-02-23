package de.themoep.resourcepacksplugin.bukkit;

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.commands.UsePackCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Phoenix616 on 20.06.2015.
 */
public class UsePackCommand implements CommandExecutor {

    ResourcepacksPlugin plugin;

    public UsePackCommand(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ResourcepacksPlayer s = null;
        if(sender instanceof Player) {
            s = plugin.getPlayer(((Player) sender).getUniqueId());
        }
        return new UsePackCommandExecutor(plugin).execute(s, args);
    }
}
