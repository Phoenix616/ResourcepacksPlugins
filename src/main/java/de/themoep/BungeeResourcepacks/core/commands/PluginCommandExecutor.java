package de.themoep.BungeeResourcepacks.core.commands;

import de.themoep.BungeeResourcepacks.core.ResourcepacksPlayer;
import de.themoep.BungeeResourcepacks.core.ResourcepacksPlugin;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public abstract class PluginCommandExecutor {

    protected final ResourcepacksPlugin plugin;

    public PluginCommandExecutor(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    abstract void execute(ResourcepacksPlayer sender, String[] args);
}
