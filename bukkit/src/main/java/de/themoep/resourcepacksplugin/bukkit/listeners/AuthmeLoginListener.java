package de.themoep.resourcepacksplugin.bukkit.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Created by Phoenix616 on 24.04.2016.
 */
public class AuthmeLoginListener implements Listener {

    private final WorldResourcepacks plugin;
    private boolean firstPlayer = true;

    public AuthmeLoginListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    /**
     * Send information to a Bungee that it should wait for proper AuthMe validation
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if(firstPlayer && plugin.isEnabled() && plugin.getConfig().getBoolean("bungeeauthmedetect", true)) {
            firstPlayer = false;
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("detectedAuthMe");
            event.getPlayer().sendPluginMessage(plugin, "Resourcepack", out.toByteArray());
        }
    }

    /**
     * Send a plugin message to the Bungee when a player logged into AuthMe on subchannel authMeLogin with the name and UUID
     * @param event AuthMe's LoginEvent
     */
    @EventHandler
    public void onAuthMeLogin(LoginEvent event) {
        if(plugin.isEnabled()) {
            plugin.getPackManager().applyPack(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getName());

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("authMeLogin");
            out.writeUTF(event.getPlayer().getName());
            out.writeUTF(event.getPlayer().getUniqueId().toString());
            event.getPlayer().sendPluginMessage(plugin, "Resourcepack", out.toByteArray());
        }
    }
}
