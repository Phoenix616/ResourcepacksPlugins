package de.themoep.BungeeResourcepacks.bungee;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class YamlConfig {
    protected Configuration cfg;
    protected final static ConfigurationProvider ymlCfg = ConfigurationProvider.getProvider( YamlConfiguration.class );

    protected File configFile;

    /**
     * read configuration into memory
     * @param configFilePath
     * @throws java.io.IOException
     */
    public YamlConfig(String configFilePath) throws IOException {
        configFile = new File(configFilePath);

        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            configFile.createNewFile();
            cfg = ymlCfg.load(configFile);

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
            BungeeResourcepacks.getInstance().getLogger().severe("Unable to save configuration at " + configFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    public void createDefaultConfig() {
        cfg = ymlCfg.load(new InputStreamReader(BungeeResourcepacks.getInstance().getResourceAsStream("config.yml")));

        save();
    }    
    
    /**
     * deletes configuration file
     */
    public void removeConfig() {
        configFile.delete();
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
}