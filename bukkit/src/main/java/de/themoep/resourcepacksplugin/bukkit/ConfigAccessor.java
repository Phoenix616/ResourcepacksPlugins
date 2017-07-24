package de.themoep.resourcepacksplugin.bukkit;

/*
* Copyright (C) 2012
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigAccessor {

    private final String fileName;
    protected final Plugin plugin;

    private File configFile;
    private FileConfiguration fileConfiguration;
    private FileConfiguration defaults;

    public ConfigAccessor(Plugin plugin, String fileName) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");
        this.plugin = plugin;
        this.fileName = fileName;
        File dataFolder = plugin.getDataFolder();
        if (dataFolder == null)
            throw new IllegalStateException();
        this.configFile = new File(plugin.getDataFolder(), fileName);
        this.defaults = getDefaults();
    }

    public void reloadConfig() {
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        if (defaults != null) {
            fileConfiguration.setDefaults(defaults);
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            if (defaults != null) {
                return defaults;
            } else {
                reloadConfig();
            }
        }
        return fileConfiguration;
    }

    public FileConfiguration getDefaults() {
        if (defaults == null) {
            // Look for defaults in the jar
            InputStream defConfigStream = plugin.getResource(fileName);
            if (defConfigStream != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
        return defaults;
    }

    public void saveConfig() {
        if (fileConfiguration != null && configFile != null) {
            try {
                getConfig().save(configFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            this.plugin.saveResource(fileName, false);
        }
    }

}

