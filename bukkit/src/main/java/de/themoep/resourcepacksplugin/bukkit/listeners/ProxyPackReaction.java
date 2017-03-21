package de.themoep.resourcepacksplugin.bukkit.listeners;

import com.google.common.io.ByteArrayDataInput;
import org.bukkit.entity.Player;

/**
 * A reaction onto a message on a certain sub channel
 */
public abstract class ProxyPackReaction {

    /**
     * Execute this action
     * @param player    The player that received the plugin message
     * @param data      The data as an input stream that you can read from
     */
    public abstract void execute(Player player, ByteArrayDataInput data);
}
