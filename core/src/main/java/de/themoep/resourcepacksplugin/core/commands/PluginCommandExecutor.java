package de.themoep.resourcepacksplugin.core.commands;

/*
 * ResourcepacksPlugins - core
 * Copyright (C) 2018 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.themoep.resourcepacksplugin.core.ChatColor;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public abstract class PluginCommandExecutor {

    protected final ResourcepacksPlugin plugin;
    private String name;
    private List<String> aliases = new ArrayList<>();
    protected String permission;
    private String usage;
    private PluginCommandExecutor parent = null;
    protected Map<String, PluginCommandExecutor> subCommands = new LinkedHashMap<>();
    protected Map<String, PluginCommandExecutor> subCommandAliases = new LinkedHashMap<>();

    public PluginCommandExecutor(ResourcepacksPlugin plugin, String usage) {
        this(plugin, usage, null);
    }

    public PluginCommandExecutor(ResourcepacksPlugin plugin, String usage, String permission) {
        this(plugin, null, usage, permission);
    }

    public PluginCommandExecutor(ResourcepacksPlugin plugin, PluginCommandExecutor parent, String usage) {
        this(plugin, parent, usage, null);
    }

    public PluginCommandExecutor(ResourcepacksPlugin plugin, PluginCommandExecutor parent, String usage, String permission) {
        this(plugin, parent, usage, permission, new String[0]);
    }

    public PluginCommandExecutor(ResourcepacksPlugin plugin, PluginCommandExecutor parent, String usage, String permission, String... aliases) {
        this.plugin = plugin;
        this.parent = parent;
        if (usage == null) {
            usage = "";
        }
        if (usage.isEmpty() && parent != null) {
            throw new IllegalArgumentException("You have to set a command name/usage!");
        }
        this.name = usage.contains(" ") ? usage.substring(0, usage.indexOf(' ')).toLowerCase() : usage.toLowerCase();
        this.usage = usage.contains(" ") ? usage.substring(usage.indexOf(' ') + 1) : "";
        this.permission = permission;
        if (permission == null) {
            if (parent != null) {
                this.permission = parent.permission + "." + name;
            } else {
                this.permission = plugin.getName().toLowerCase() + ".command." + getPath().replace(' ', '.');
            }
        }
        Collections.addAll(this.aliases, aliases);
    }

    public abstract boolean run(ResourcepacksPlayer sender, String[] args);

    public boolean execute(ResourcepacksPlayer sender, String[] args) {
        if (permission != null && !permission.isEmpty() && !plugin.checkPermission(sender, permission)) {
            plugin.sendMessage(sender, "command.no-permission",
                    "command", getPath(),
                    "name", getName(),
                    "usage", getUsage(),
                    "permission", permission,
                    "subcommands", String.join("|", subCommands.keySet())
            );
            return false;
        }
        if (args.length > 0) {
            PluginCommandExecutor subCommand = getSubCommand(args[0]);
            if (subCommand != null) {
                return subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        if (!run(sender, args)) {
            sendMessage(sender, "usage",
                    "command", getPath(),
                    "name", getName(),
                    "usage", getUsage(),
                    "permission", permission,
                    "subcommands", String.join("|", subCommands.keySet())
            );
            return false;
        }
        return true;
    }

    public Map<String, PluginCommandExecutor> getSubCommands() {
        return subCommands;
    }

    public PluginCommandExecutor getSubCommand(String name) {
        PluginCommandExecutor subCommand = subCommands.get(name.toLowerCase());
        if (subCommand == null) {
            subCommand = subCommandAliases.get(name.toLowerCase());
        }
        return subCommand;
    }

    public void sendMessage(ResourcepacksPlayer sender, String key, String... replacements) {
        plugin.sendMessage(sender, getMessageKey(sender, key), replacements);
    }

    protected String getMessage(ResourcepacksPlayer sender, String key, String... replacements) {
        return plugin.getMessage(sender, getMessageKey(sender, key), replacements);
    }

    private String getMessageKey(ResourcepacksPlayer sender, String key) {
        String resultKey = getKey() + "." + key;
        while (!plugin.hasMessage(sender, "command." + resultKey)) {
            if (!resultKey.contains(".")) {
                return "command." + getKey() + "." + key;
            }
            resultKey = resultKey.substring(resultKey.indexOf('.') + 1);
        }
        return "command." + resultKey;
    }

    public String getPath() {
        String parentPath = parent != null ? parent.getPath() : "";
        return parentPath.isEmpty() ? getName() : parentPath + " " + getName();
    }

    public String getKey() {
        String parentKey = parent != null ? parent.getKey() : "";
        return parentKey.isEmpty() ? name : parentKey + (name.isEmpty() ? "" : "." + name);
    }

    public void registerSubCommands(PluginCommandExecutor... subCommands) {
        for (PluginCommandExecutor subCommand : subCommands) {
            this.subCommands.put(subCommand.name, subCommand);
            for (String alias : subCommand.aliases) {
                this.subCommandAliases.putIfAbsent(alias.toLowerCase(), subCommand);
            }
        }
    }

    public String getName() {
        if (name.isEmpty() && parent == null) {
            return plugin.getName().charAt(0) + "rp";
        }
        return name;
    }

    public String getUsage() {
        String usage = this.usage;
        if (usage.isEmpty() && !subCommands.isEmpty()) {
            usage = "[" + String.join(" | ", subCommands.keySet()) + "]";
        }
        return ChatColor.usage(usage);
    }

    public ResourcepacksPlugin getPlugin() {
        return plugin;
    }

    public String getPermission() {
        return permission;
    }

    public String[] getAliases() {
        return aliases.toArray(new String[0]);
    }
}
