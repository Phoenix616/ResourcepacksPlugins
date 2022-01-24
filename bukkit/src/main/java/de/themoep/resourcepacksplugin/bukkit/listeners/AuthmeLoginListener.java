package de.themoep.resourcepacksplugin.bukkit.listeners;

/*
 * ResourcepacksPlugins - bukkit
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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class AuthmeLoginListener implements Listener {

    private final WorldResourcepacks plugin;

    public AuthmeLoginListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a plugin message to the Bungee when a player logged into AuthMe on subchannel authMeLogin with the name and UUID
     * @param event AuthMe's LoginEvent
     */
    @EventHandler
    public void onAuthMeLogin(LoginEvent event) {
        if (plugin.isEnabled()) {
            if (plugin.getConfig().getBoolean("use-auth-plugin", plugin.getConfig().getBoolean("useauth", false))) {
                long sendDelay = plugin.getPackManager().getAssignment(event.getPlayer().getWorld().getName()).getSendDelay();
                if (sendDelay < 0) {
                    sendDelay = plugin.getPackManager().getGlobalAssignment().getSendDelay();
                }
                if (sendDelay > 0) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getPackManager().applyPack(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getName()), sendDelay);
                } else {
                    plugin.getPackManager().applyPack(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getName());
                }
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("authMeLogin");
            out.writeUTF(event.getPlayer().getName());
            out.writeUTF(event.getPlayer().getUniqueId().toString());
            event.getPlayer().sendPluginMessage(plugin, "rp:plugin", out.toByteArray());
        }
    }
}
