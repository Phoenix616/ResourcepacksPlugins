package de.themoep.resourcepacksplugin.bukkit.internal;

import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Phoenix616 on 22.07.2016.
 */
public class InternalHelper_fallback implements InternalHelper {


    private Method setPackWithHashMethod = null;

    public InternalHelper_fallback() {
        try {
            setPackWithHashMethod = Player.class.getDeclaredMethod("setResourcePack", String.class, String.class);
        } catch (NoSuchMethodException e) {
            // Old version, method not there
        }
    }

    @Override
    public void setResourcePack(Player player, String url, String hash) {
        if (setPackWithHashMethod != null) {
            try {
                setPackWithHashMethod.invoke(player, url, hash);
                return;
            } catch (InvocationTargetException e) {
                // invokation failed
            } catch (IllegalAccessException e) {
                // not allowed to acces it?
            }
        }
        player.setResourcePack(url);
    }
}
