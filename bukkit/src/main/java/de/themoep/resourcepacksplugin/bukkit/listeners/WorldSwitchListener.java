package de.themoep.resourcepacksplugin.bukkit.listeners;

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Created by Phoenix616 on 14.05.2015.
 */
public class WorldSwitchListener implements Listener {

    WorldResourcepacks plugin;

    public WorldSwitchListener(WorldResourcepacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        calculatePack(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Send out pack after the proxy server sent us the info about the previous pack
        // Also helps to wait till the client properly loaded to display the confirmation dialog
        new BukkitRunnable() {
            @Override
            public void run() {
                calculatePack(event.getPlayer());
            }
        }.runTaskLater(plugin, 10);

    }

    private void calculatePack(Player player) {
        if(plugin.isEnabled()) {
            ResourcePack prev = plugin.getPackManager().getUserPack(player.getUniqueId());
            ResourcePack pack = null;
            if(plugin.getPackManager().isGlobalSecondary(prev)) {
                return;
            }
            if(plugin.getPackManager().isServerSecondary(player.getWorld().getName(), prev)) {
                return;
            }
            pack = plugin.getPackManager().getServerPack(player.getWorld().getName());
            if(pack == null) {
                pack = plugin.getPackManager().getGlobalPack();
            }
            if(pack == null && prev != null) {
                List<String> serversecondary = plugin.getPackManager().getServerSecondary(player.getWorld().getName());
                if(serversecondary.size() > 0) {
                    pack = plugin.getPackManager().getByName(serversecondary.get(0));
                }
                if(pack == null) {
                    List<String> globalsecondary = plugin.getPackManager().getGlobalSecondary();
                    if(globalsecondary.size() > 0) {
                        pack = plugin.getPackManager().getByName(globalsecondary.get(0));
                    }
                }
                if(pack == null) {
                    pack = plugin.getPackManager().getEmptyPack();
                }
            }
            if(pack != null) {
                if(!pack.equals(prev)) {
                    plugin.setPack(player, pack);
                }
            }
        }
    }
}