package de.themoep.resourcepacksplugin.sponge;

/*
 * ResourcepacksPlugins - sponge
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
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

import de.themoep.minedown.adventure.MineDown;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;
import net.kyori.adventure.text.serializer.spongeapi.SpongeComponentSerializer;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 20.06.2015.
 */
public class ForwardingCommand implements CommandCallable {

    private final PluginCommandExecutor executor;

    public ForwardingCommand(PluginCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public CommandResult process(CommandSource sender, String args) throws CommandException {
        ResourcepacksPlayer s = null;
        if (sender instanceof Player) {
            s = executor.getPlugin().getPlayer(((Player) sender).getUniqueId());
        }
        executor.execute(s, args.split(" "));
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        PluginCommandExecutor last = executor;
        for (String a : arguments.split(" ")) {
            last = last.getSubCommand(a);
            if (last == null) {
                return Collections.emptyList();
            }
        }
        return last.getSubCommands().values().stream()
                .filter(c -> c.getPermission() == null || source.hasPermission(c.getPermission()))
                .map(PluginCommandExecutor::getName)
                .collect(Collectors.toList());
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return executor.getPermission() == null || source.hasPermission(executor.getPermission());
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(getUsage(source));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(getUsage(source));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return SpongeComponentSerializer.get().serialize(MineDown.parse(executor.getUsage()));
    }
}
