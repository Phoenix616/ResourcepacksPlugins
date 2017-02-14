package de.themoep.resourcepacksplugin.bukkit.internal;

import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Phoenix616 on 22.07.2016.
 */
public class InternalHelper_fallback implements InternalHelper {

    private Method setPackWithHashMethod = null;

    private Method getHandle = null;
    private Method setResourcePack = null;

    public InternalHelper_fallback() {
        try {
            setPackWithHashMethod = Player.class.getDeclaredMethod("setResourcePack", String.class, Array.class);
        } catch (NoSuchMethodException e) {
            // Old version, method still not there
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            try {
                Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
                getHandle = craftPlayer.getDeclaredMethod("getHandle");

                Class<?> entityPlayer = Class.forName("net.minecraft.server." + serverVersion + ".EntityPlayer");
                setResourcePack = entityPlayer.getDeclaredMethod("setResourcePack", String.class, String.class);

            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void setResourcePack(Player player, ResourcePack pack) {
        try {
            if (setPackWithHashMethod != null) {
                setPackWithHashMethod.invoke(player, pack.getUrl(), pack.getRawHash());
                return;
            } else if (getHandle != null && setResourcePack != null) {
                Object entityPlayer = getHandle.invoke(player);
                setResourcePack.invoke(entityPlayer, pack.getUrl(), pack.getHash());
                return;
            }
        } catch (InvocationTargetException e) {
            // invokation failed
        } catch (IllegalAccessException e) {
            // not allowed to access it?
        }
        player.setResourcePack(pack.getUrl());
    }
}
