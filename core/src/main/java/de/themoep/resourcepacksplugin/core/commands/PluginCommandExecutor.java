package de.themoep.resourcepacksplugin.core.commands;

import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public abstract class PluginCommandExecutor {

    protected final ResourcepacksPlugin plugin;

    public PluginCommandExecutor(ResourcepacksPlugin plugin) {
        this.plugin = plugin;
    }

    abstract boolean execute(ResourcepacksPlayer sender, String[] args);
}
