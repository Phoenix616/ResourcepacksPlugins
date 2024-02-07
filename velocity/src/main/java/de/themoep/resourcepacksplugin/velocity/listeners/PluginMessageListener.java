package de.themoep.resourcepacksplugin.velocity.listeners;

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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.themoep.resourcepacksplugin.core.SubChannelHandler;
import de.themoep.resourcepacksplugin.velocity.PluginConfig;
import de.themoep.resourcepacksplugin.velocity.VelocityResourcepacks;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

public class PluginMessageListener extends SubChannelHandler<ServerConnection> {
    private static final ChannelIdentifier CHANNEL_IDENTIFIER = MinecraftChannelIdentifier.from(MESSAGING_CHANNEL);

    private final VelocityResourcepacks plugin;

    private final PluginConfig keyConfig;

    private final AuthHandler authHandler;

    public PluginMessageListener(VelocityResourcepacks plugin) {
        super(plugin);
        this.plugin = plugin;
        authHandler = new AuthHandler(plugin);
        keyConfig = new PluginConfig(plugin, new File(plugin.getDataFolder(), "key.yml"), null);
        registerSubChannel("authLogin", (s, in) -> {
            String playerName = in.readUTF();
            UUID playerId = UUID.fromString(in.readUTF());
            if (!plugin.isAuthenticated(playerId)) {
                plugin.getProxy().getPlayer(playerId).ifPresent(authHandler::onAuth);
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public void pluginMessageReceived(PluginMessageEvent event) {
        if (!plugin.isEnabled() || !event.getIdentifier().equals(CHANNEL_IDENTIFIER))
            return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (event.getSource() instanceof ServerConnection) {
            handleMessage((ServerConnection) event.getSource(), event.getData());
        } else {
            plugin.logDebug("Received plugin message from " + event.getSource() + " which is not a ServerConnection!");
        }
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        if (plugin.isEnabled()) {
            event.getPlayer().getCurrentServer().ifPresent(this::sendKey);
        }
    }

    @Override
    protected void sendPluginMessage(ServerConnection target, byte[] data) {
        try {
            target.sendPluginMessage(CHANNEL_IDENTIFIER, data);
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to send plugin message to " + target + "! This is most likely because the player connection timed out. " + e.getMessage());
        }
    }

    @Override
    protected void saveKey(String key) {
        keyConfig.set("key", key);
        keyConfig.save();
    }

    @Override
    protected String loadKey() {
        String key = null;
        if (keyConfig.load()) {
            key = keyConfig.getString("key", null);
        } else {
            plugin.log(Level.SEVERE, "Unable to load key.yml! Forwarding info to the plugin on the Minecraft server will not work!");
        }
        if (key == null) {
            key = generateKey();
            saveKey(key);
        }
        return key;
    }
}
