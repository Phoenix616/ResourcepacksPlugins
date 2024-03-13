package de.themoep.resourcepacksplugin.bukkit.internal;

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

import de.themoep.resourcepacksplugin.bukkit.WorldResourcepacks;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Phoenix616 on 22.07.2016.
 */
public class InternalHelper_fallback implements InternalHelper {

    private final WorldResourcepacks plugin;

    private Method getHandle = null;
    private Method setResourcePack = null;
    private boolean hasAddResourcePack = false;
    private boolean hasSetIdResourcePack = false;
    private boolean hasSetResourcePack = false;
    private boolean hasRemoveResourcepacks = false;

    public InternalHelper_fallback(WorldResourcepacks plugin) {
        this.plugin = plugin;
        try {
            hasAddResourcePack = Player.class.getMethod("addResourcePack", UUID.class, String.class, byte[].class, String.class, boolean.class) != null;
        } catch (NoSuchMethodException ignored) {}
        try {
            hasSetIdResourcePack = Player.class.getMethod("setResourcePack", UUID.class, String.class, byte[].class) != null;
        } catch (NoSuchMethodException ignored) {}
        try {
            hasSetResourcePack = Player.class.getMethod("setResourcePack", String.class, byte[].class) != null;
        } catch (NoSuchMethodException ignored) {}

        if (!hasAddResourcePack && !hasSetIdResourcePack && !hasSetResourcePack) {
            // Old version, methods still not there
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            try {
                Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
                getHandle = craftPlayer.getDeclaredMethod("getHandle");

                Class<?> entityPlayer = Class.forName("net.minecraft.server." + serverVersion + ".EntityPlayer");
                setResourcePack = entityPlayer.getDeclaredMethod("setResourcePack", String.class, String.class);

            } catch (ClassNotFoundException | NoSuchMethodException e) {
                plugin.log(Level.SEVERE, "Unable to find method which enables us to efficiently send a resource pack!", e);
            }
        }
        try {
            hasRemoveResourcepacks = Player.class.getMethod("removeResourcePack", String.class) != null;
        } catch (NoSuchMethodException ignored) {}
    }

    @Override
    public void setResourcePack(Player player, ResourcePack pack) {
        if (hasAddResourcePack) {
            player.addResourcePack(pack.getUuid(), plugin.getPackManager().getPackUrl(pack), pack.getRawHash(), null, false);
            return;
        }

        if (hasSetIdResourcePack) {
            player.setResourcePack(pack.getUuid(), plugin.getPackManager().getPackUrl(pack), pack.getRawHash(), null, false);
            return;
        }

        if (hasSetResourcePack) {
            player.setResourcePack(plugin.getPackManager().getPackUrl(pack), pack.getRawHash());
            return;
        }

        try {
            if (getHandle != null && setResourcePack != null) {
                Object entityPlayer = getHandle.invoke(player);
                setResourcePack.invoke(entityPlayer, plugin.getPackManager().getPackUrl(pack), pack.getHash());
                return;
            }
        } catch (InvocationTargetException | IllegalAccessException ignored) {}
        player.setResourcePack(pack.getUrl());
    }

    @Override
    public void removeResourcePack(Player player, ResourcePack pack) {
        try {
            if (hasRemoveResourcepacks) {
                player.removeResourcePack(pack.getUuid());
            }
        } catch (NoSuchMethodError e) {
            // Method not found, fallback to old method
            hasRemoveResourcepacks = false;
            throw new UnsupportedOperationException("This version does not support removing resourcepacks!");
        }
    }

    @Override
    public void removeResourcePacks(Player player) {
        try {
            if (hasRemoveResourcepacks) {
                player.removeResourcePacks();
            }
        } catch (NoSuchMethodError e) {
            // Method not found, fallback to old method
            hasRemoveResourcepacks = false;
            throw new UnsupportedOperationException("This version does not support removing resourcepacks!");
        }
    }
}
