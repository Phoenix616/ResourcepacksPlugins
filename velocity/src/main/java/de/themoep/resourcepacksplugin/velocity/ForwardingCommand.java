package de.themoep.resourcepacksplugin.velocity;

/*
 * ResourcepacksPlugins - velocity
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
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

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ForwardingCommand implements SimpleCommand {
    private final PluginCommandExecutor executor;

    public ForwardingCommand(PluginCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Invocation i) {
        ResourcepacksPlayer s = null;
        if (i.source() instanceof Player) {
            s = ((VelocityResourcepacks) executor.getPlugin()).getPlayer((Player) i.source());
        }
        executor.execute(s, i.arguments());
    }

    @Override
    public boolean hasPermission(Invocation i) {
        return i.source().hasPermission(executor.getPermission());
    }

    @Override
    public List<String> suggest(Invocation i) {
        PluginCommandExecutor last = executor;
        for (String a : i.arguments()) {
            last = last.getSubCommand(a);
            if (last == null) {
                return Collections.emptyList();
            }
        }
        return last.getSubCommands().values().stream()
                .filter(c -> c.getPermission() == null || i.source().hasPermission(c.getPermission()))
                .map(PluginCommandExecutor::getName)
                .collect(Collectors.toList());
    }

}
