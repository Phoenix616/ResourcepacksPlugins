package de.themoep.BungeeResourcepacks.core;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoenix616 on 25.03.2015.
 */
public class ResourcePack {
    private String name;
    private String url;
    private String hash;
    private List<String> servers = new ArrayList<String>();

    /**
     * Object representation of a resourcepack set in the plugin's config file.
     * @param name The name of the resourcepack as set in the config. Serves as an uinque identifier. Correct case.
     * @param url The url where this resourcepack is located at and where the client will download it from
     * @param hash The hash set for this resourcepack. Ideally this is the zip file's sha1 hash.
     */
    public ResourcePack(String name, String url, String hash) {
        this.name = name;
        this.url = url;
        if(hash != null && hash.length() == 40) {
            this.hash = hash;
        } else {
            this.hash = Hashing.sha1().hashString(url, Charsets.UTF_8).toString().substring(0, 39).toLowerCase();
        }
    }
    
    /**
     * Get the name of the resourcepack as set in the config. Serves as an uinque identifier. Correct case.
     * @return The name as a string in correct case
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the url where this resourcepack is located at and where the client will download it from
     * @return The url as a string
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the hash set for this resourcepack. Ideally this is the zip file's sha1 hash.
     * @return The 40 digit lowercase hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * Don't use this method! Use PackManager.addServer() instead!
     */
    void addServer(String name) {
        if(!this.servers.contains(name.toLowerCase())) {
            this.servers.add(name.toLowerCase());
        }
    }
    
    /**
     * Don't use this method! Use PackManager.removeServer() instead!
     */
    boolean removeServer(String server) {
        return this.servers.remove(server.toLowerCase());
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof ResourcePack)) {
            return false;
        } else {
            ResourcePack other = (ResourcePack)o;
            String this$name = this.getName();
            String other$name = other.getName();
            if (this$name == null) {
                if (other$name != null) {
                    return false;
                }
            } else if (!this$name.equals(other$name)) {
                return false;
            }
            
            String this$url = this.getUrl();
            String other$url = other.getUrl();
            if (this$url == null) {
                if (other$url != null) {
                    return false;
                }
            } else if (!this$url.equals(other$url)) {
                return false;
            }

            String this$hash = this.getHash();
            String other$hash = other.getHash();
            if (this$hash == null) {
                if (other$hash != null) {
                    return false;
                }
            } else if (!this$hash.equals(other$hash)) {
                return false;
            }

            return true;
        }
    }
}
