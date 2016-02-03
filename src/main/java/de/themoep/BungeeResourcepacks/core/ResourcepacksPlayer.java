package de.themoep.BungeeResourcepacks.core;

import java.util.UUID;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPlayer {
    private String name;
    private UUID uniqueId;

    public ResourcepacksPlayer(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }


    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof ResourcepacksPlayer) {
            ResourcepacksPlayer other = (ResourcepacksPlayer) obj;
            return other.getUniqueId() == getUniqueId() && other.getName().equals(getName());
        }
        return false;
    }
}
