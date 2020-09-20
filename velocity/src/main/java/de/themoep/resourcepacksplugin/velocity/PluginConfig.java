package de.themoep.resourcepacksplugin.velocity;

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
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PluginConfig {

    private static final Pattern PATH_PATTERN = Pattern.compile("\\.");

    private final VelocityResourcepacks plugin;
    private final File configFile;
    private final String defaultFile;
    private final YAMLConfigurationLoader configLoader;
    private ConfigurationNode config;
    private ConfigurationNode defaultConfig;

    public PluginConfig(VelocityResourcepacks plugin, File configFile) {
        this(plugin, configFile, configFile.getName());
    }

    public PluginConfig(VelocityResourcepacks plugin, File configFile, String defaultFile) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.defaultFile = defaultFile;
        configLoader = YAMLConfigurationLoader.builder().setIndent(2).setPath(configFile.toPath()).build();
    }

    public boolean load() {
        try {
            config = configLoader.load();
            if (defaultFile != null) {
                defaultConfig = config = YAMLConfigurationLoader.builder()
                        .setIndent(2)
                        .setSource(() -> new BufferedReader(new InputStreamReader(plugin.getResourceAsStream(defaultFile))))
                        .build().load();
            }
            plugin.getLogger().log(Level.INFO, "Loaded " + configFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load configuration file " + configFile.getName(), e);
            return false;
        }
    }

    public boolean createDefaultConfig() throws IOException {
        try (InputStream in = plugin.getResourceAsStream(defaultFile)) {
            if (in == null) {
                plugin.getLogger().log(Level.WARNING, "No default config '" + defaultFile + "' found in " + plugin.getName() + "!");
                return false;
            }
            if (!configFile.exists()) {
                File parent = configFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                try {
                    Files.copy(in, configFile.toPath());
                    return true;
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save " + configFile.getName() + " to " + configFile, ex);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not load default config from " + defaultFile, ex);
        }
        return false;
    }

    public void save() {
        try {
            configLoader.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object set(String path, Object value) {
        ConfigurationNode node = config.getNode(splitPath(path));
        Object prev = node.getValue();
        node.setValue(value);
        return prev;
    }

    public ConfigurationNode remove(String path) {
        ConfigurationNode node = config.getNode(splitPath(path));
        return node.isVirtual() ? node : node.setValue(null);
    }

    public ConfigurationNode getRawConfig() {
        return config;
    }

    public ConfigurationNode getRawConfig(String path) {
        return getRawConfig().getNode(splitPath(path));
    }

    public boolean has(String path) {
        return !getRawConfig(path).isVirtual();
    }

    public boolean isSection(String path) {
        return getRawConfig(path).hasMapChildren();
    }
    
    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        return getRawConfig(path).getInt(def);
    }
    
    public double getDouble(String path) {
        return getDouble(path, 0);
    }

    public double getDouble(String path, double def) {
        return getRawConfig(path).getDouble(def);
    }
    
    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String def) {
        return getRawConfig(path).getString(def);
    }
    
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        return getRawConfig(path).getBoolean(def);
    }

    private static Object[] splitPath(String key) {
        return PATH_PATTERN.split(key);
    }
}
