package de.themoep.resourcepacksplugin.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class YamlConfig {
    private final Configuration defaultCfg;
    protected Configuration cfg;
    protected final static ConfigurationProvider ymlCfg = ConfigurationProvider.getProvider( YamlConfiguration.class );

    protected File configFile;

    private Plugin plugin;
    
    /**
     * Read configuration into memory
     * @param plugin The plugin this yaml config is for
     * @param configFilePath The path where the config's file should be saved in
     * @throws java.io.IOException When it could not create or load the file
     */
    public YamlConfig(Plugin plugin, String configFilePath) throws IOException {
        this.plugin = plugin;
        
        configFile = new File(configFilePath);
        defaultCfg = ymlCfg.load(new InputStreamReader(plugin.getResourceAsStream("bungee-config.yml")));

        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            configFile.createNewFile();

            createDefaultConfig();
        } else {
            cfg = ymlCfg.load(configFile);
        }
    }

    /**
     * save configuration to disk
     */
    public void save() {
        try {
            ymlCfg.save(cfg, configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save configuration at " + configFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    public void createDefaultConfig() {
        cfg = defaultCfg;

        save();
    }    
    
    /**
     * deletes configuration file
     */
    public void removeConfig() {
        configFile.delete();
    }

    public boolean getBoolean(String path) {
        return cfg.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return cfg.getBoolean(path, def);
    }

    public int getInt(String path) {
        return cfg.getInt(path);
    }

    public int getInt(String path, int def) {
        return cfg.getInt(path, def);
    }

    public String getString(String path) {
        return cfg.getString(path);
    }

    public String getString(String path, String def) {
        return cfg.getString(path, def);
    }

    public List<String> getStringList(String path) {
        return cfg.getStringList(path);
    }

    public Configuration getSection(String path) {
        return cfg.getSection(path);
    }

    public Configuration getDefaults() {
        return defaultCfg;
    }

    public boolean isSet(String path) {
        return cfg.get(path) != null;
    }

    public void set(String path, Object value) {
        cfg.set(path, value);
    }
}