package de.themoep.BungeeResourcepacks.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Phoenix616 on 25.03.2015.
 */
public class PackManager {

    /**
     * packname -> ResourcePack
     */
    private Map<String, ResourcePack> packmap = new HashMap<String, ResourcePack>();

    /**
     * packhash -> packname 
     */
    private Map<String, String> hashmap = new HashMap<String, String>();
    
    /**
     * packurl -> packname 
     */
    private Map<String, String> urlmap = new HashMap<String, String>();

    /**
     * playerid -> packname 
     */
    private Map<UUID, String> usermap = new ConcurrentHashMap<UUID, String>();

    /**
     * Name of the empty pack, null if none is set
     */
    private String empty = null;
    
    /**
     * Name of the global pack, null if none is set
     */
    private String global = null;
    
    /**
     * servername -> packname 
     */
    private Map<String, String> servermap = new HashMap<String, String>();

    /**
     * Registeres a new resource pack with the packmanager
     * @param pack The resourcepack to register
     * @return If a pack with that name was known before it returns the past pack, null if none was known
     */
    public ResourcePack addPack(ResourcePack pack) {
        hashmap.put(pack.getHash(), pack.getName().toLowerCase());
        urlmap.put(pack.getUrl(), pack.getName().toLowerCase());
        return packmap.put(pack.getName().toLowerCase(), pack);
    }

    /**
     * Get the resourcepack by its name
     * @param name The name of the pack to get
     * @return The resourcepack with that name, null if there is none
     */
    public ResourcePack getByName(String name) {
        return packmap.get(name.toLowerCase());
    }
    
    /**
     * Get the resourcepack by its hash
     * @param hash The hash of the pack to get
     * @return The resourcepack with that hash, null if there is none
     */
    public ResourcePack getByHash(String hash) {
        String name = hashmap.get(hash);
        return (name == null) ? null : getByName(name);
    }

    /**
     * Get the resourcepack by its url
     * @param url The url of the pack to get
     * @return The resourcepack with that url, null if there is none
     */
    public ResourcePack getByUrl(String url) {
        String name = urlmap.get(url);
        return (name == null) ? null : getByName(name);
    }

    /**
     * Set the empty Resource Pack
     * @param pack The pack to set as empty pack
     * @return The previous empty pack, null if none was set
     */
    public ResourcePack setEmptyPack(ResourcePack pack) {
        return setEmptyPack((pack == null) ? null : pack.getName());
    }

    /**
     * Set the empty Resource Pack
     * @param packname The name of the pack to set as empty pack
     * @return The previous empty pack, null if none was set
     */
    public ResourcePack setEmptyPack(String packname) {
        ResourcePack rp = getEmptyPack();
        empty = packname;
        return rp;
    }

    /**
     * Get the empty Resource Pack
     * @return The empty pack, null if none is set
     */
    public ResourcePack getEmptyPack() {
        return (empty == null) ? null : getByName(empty);
    }
    
    /**
     * Set the global Resource Pack
     * @param pack The pack to set as global
     * @return The previous global pack, null if none was set
     */
    public ResourcePack setGlobalPack(ResourcePack pack) {
        return setGlobalPack((pack == null) ? null : pack.getName());
    }

    /**
     * Set the global Resource Pack
     * @param packname The name of the pack to set as global
     * @return The previous global pack, null if none was set
     */
    public ResourcePack setGlobalPack(String packname) {
        ResourcePack rp = getGlobalPack();
        global = packname;
        return rp;
    }

    /**
     * Get the global Resource Pack
     * @return The global pack, null if none is set
     */
    public ResourcePack getGlobalPack() {
        return (global == null) ? null : getByName(global);
    }

    /**
     * Get the resourcepack of a server
     * @param server The name of the server, "!global" for the global pack
     * @return The resourcepack of the server, null if there is none
     */
    public ResourcePack getServerPack(String server) {
        String name = servermap.get(server);
        return (name == null) ? null : getByName(name);
    }
    
    /**
     * Get the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player has selected, null if he has none/isn't known
     */
    public ResourcePack getUserPack(UUID playerid) {
        String name = usermap.get(playerid);
        return (name == null) ? null : getByName(name);
    }
    
    /**
     * Set the resourcepack of a user
     * @param playerid The UUID of this player
     * @param pack The resourcepack of the user
     * @return The resourcepack the player had selected previous, null if he had none before
     */
    public ResourcePack setUserPack(UUID playerid, ResourcePack pack) {
        String previous = usermap.put(playerid, pack.getName());
        return (previous == null) ? null : getByName(previous);
    }

    /**
     * Clear the resourcepack of a user
     * @param playerid The UUID of this player
     * @return The resourcepack the player had selected previous, null if he had none before
     */
    public ResourcePack clearUserPack(UUID playerid) {
        String previous = usermap.remove(playerid);
        return (previous == null) ? null : getByName(previous);
    }
    

    /**
     * Add a server to a resourcepack
     * @param server The server this pack should be active on
     * @param pack The resourcepack
     */
    public void addServer(String server, ResourcePack pack) {
        pack.addServer(server);
        servermap.put(server, pack.getName().toLowerCase());
    }
    
    /**
     * Add a server to a resourcepack
     * @param server The server this pack should be active on
     * @return True if the server had a pack, false if not
     */
    public boolean removeServer(String server) {
        String packname = servermap.remove(server);
        if(packname != null && packmap.containsKey(packname)) {
            return packmap.get(packname).removeServer(server);
        }
        return false;
    }
}
