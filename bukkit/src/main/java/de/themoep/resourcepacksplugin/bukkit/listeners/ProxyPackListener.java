package de.themoep.resourcepacksplugin.bukkit.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Level;

/**
 * Created by Phoenix616 on 02.02.2016.
 */
public class ProxyPackListener implements PluginMessageListener {

    private final WorldResourcepacks plugin;

    public ProxyPackListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] message) {
        if(!channel.equals(plugin.getName())) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();

        if(subchannel.equals("packChange")) {
            String playerName = in.readUTF();
            String packName = in.readUTF();
            String packUrl = in.readUTF();
            String packHash = in.readUTF();

            Player player = plugin.getServer().getPlayer(playerName);
            if(player == null || !player.isOnline()) {
                return;
            }

            ResourcePack pack = plugin.getPackManager().getByName(packName);
            if(pack == null) {
                pack = plugin.getPackManager().getByUrl(packUrl);
            }
            if(pack == null) {
                pack = plugin.getPackManager().getByHash(packHash);
            }
            if(pack == null) {
                pack = new ResourcePack(packName, packUrl, packHash);
                plugin.getPackManager().addPack(pack);
            }

            plugin.getLogger().log(plugin.loglevel, "BungeeCord proxy send pack " + pack.getName() + " (" + pack.getUrl() + ") to player " + player.getName());
            plugin.getPackManager().setUserPack(player.getUniqueId(), pack);
        } else if(subchannel.equals("clearPack")) {
            String playerName = in.readUTF();
            Player player = plugin.getServer().getPlayer(playerName);
            if(player == null || !player.isOnline()) {
                return;
            }

            plugin.getLogger().log(plugin.loglevel, "BungeeCord proxy send command to clear the pack of player " + player.getName());
            plugin.clearPack(player);

        } else {
            plugin.getLogger().log(Level.WARNING, "Unknown subchannel " + subchannel + "! Please make sure you are running a compatible plugin version on your BungeeCord!");
        }
    }
}