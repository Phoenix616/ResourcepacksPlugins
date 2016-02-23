package de.themoep.resourcepacksplugin.bukkit;

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;
import de.themoep.resourcepacksplugin.core.commands.ResourcepacksPluginCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Phoenix616 on 17.04.2015.
 */
public class WorldResourcepacksCommand implements CommandExecutor {
    
    ResourcepacksPlugin plugin;

    public WorldResourcepacksCommand(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ResourcepacksPlayer s = null;
        if(sender instanceof Player) {
            s = plugin.getPlayer(((Player) sender).getUniqueId());
        }
        return new ResourcepacksPluginCommandExecutor(plugin).execute(s, args);
    }
}
