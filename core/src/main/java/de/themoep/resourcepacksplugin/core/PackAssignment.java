package de.themoep.resourcepacksplugin.core;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Created by Phoenix616 on 06.03.2017.
 */
public class PackAssignment {

    private String pack = null;
    private LinkedHashSet<String> secondaries = new LinkedHashSet<>();
    private long sendDelay = -1;
    private Pattern regex = null;
    private final String name;

    public PackAssignment(String name) {
        this.name = name;
    }

    /**
     * Set the main pack of this assignment
     * @param pack  The main pack
     */
    public void setPack(ResourcePack pack) {
        this.pack = pack != null ? pack.getName().toLowerCase() : null;
    }

    /**
     * Set the main pack of this assignment
     * @param pack  The name of the main pack
     */
    public void setPack(String pack) {
        this.pack = pack != null ? pack.toLowerCase() : null;
    }

    /**
     * Get the name of the main pack of this assignment
     * @return  The (lowercase) name of the apck
     */
    public String getPack() {
        return pack;
    }

    /**
     * Get a list of secondary packs
     * @return  The (lowercase) names of secondary packs
     */
    public LinkedHashSet<String> getSecondaries() {
        return secondaries;
    }

    /**
     * Check whether or not a certain pack is a secondary in this assignment
     * @param pack  The name of the pack
     * @return      <tt>true</tt> if this secondary list contains this pack; <tt>false</tt> if not
     */
    public boolean isSecondary(String pack) {
        return secondaries.contains(pack.toLowerCase());
    }

    /**
     * Check whether or not a certain pack is a secondary in this assignment
     * @param pack  The the pack
     * @return      <tt>true</tt> if this secondary list contains this pack; <tt>false</tt> if not (or pack is null)
     */
    public boolean isSecondary(ResourcePack pack) {
        return pack != null && isSecondary(pack.getName());
    }

    /**
     * Add a new secondary pack
     * @param pack  The pack to add
     * @return      <tt>true</tt> as defined in Collections.add
     */
    public boolean addSecondary(ResourcePack pack) {
        return addSecondary(pack.getName());
    }

    /**
     * Add a new secondary pack
     * @param pack  The name of the pack to add
     * @return      <tt>true</tt> as defined in Collections.add
     */
    public boolean addSecondary(String pack) {
        return secondaries.add(pack.toLowerCase());
    }

    /**
     * Remove a secondary pack
     * @param pack  The pack to remove
     * @return      <tt>true</tt> if that pack was a secondary one, <tt>false</tt> if not
     */
    public boolean removeSecondary(ResourcePack pack) {
        return removeSecondary(pack.getName());
    }

    /**
     * Remove a secondary pack
     * @param pack  The name of the pack to remove
     * @return      <tt>true</tt> if that pack was a secondary one, <tt>false</tt> if not
     */
    public boolean removeSecondary(String pack) {
        return secondaries.remove(pack.toLowerCase());
    }

    /**
     * Check whether or not this assignment contains any pack settings
     * @return  <tt>true</tt> if it has no packs or secondaries; <tt>false</tt> if it has some
     */
    public boolean isEmpty() {
        return pack == null && secondaries.isEmpty() && sendDelay == -1;
    }

    /**
     * Set the delay in ticks  to wait before sending the packs from this assignment
     * @param sendDelay The delay in ticks
     */
    public void setSendDelay(long sendDelay) {
        this.sendDelay = sendDelay;
    }

    /**
     * Get the delay in ticks to wait before sending the packs from this assignment
     * @return  The delay in ticks; -1 if there was no special one configured
     */
    public long getSendDelay() {
        return sendDelay;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName()).append("{")
                .append("name=").append(name)
                .append(", pack=").append(pack)
                .append(", secondaries=[");
        for (Iterator<String> it = secondaries.iterator(); it.hasNext();) {
            s.append(it.next());
            if (it.hasNext()) {
                s.append(", ");
            }
        }
        s.append("], sendDelay=").append(sendDelay);
        if (regex != null) {
            s.append(", regex=").append(regex.toString());
        }
        return s.append("}").toString();
    }

    /**
     * Set the key name regex of this assignment
     * @param regex The compiled Pattern of this regex
     */
    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    /**
     * Get the compiled Pattern of this assignment's key regex
     * @return The compiled regex pattern or <tt>null</tt> if none is set and the key should be used literally
     */
    public Pattern getRegex() {
        return regex;
    }

    /**
     * Get the name of this assignment
     * @return  The name of this assignment
     */
    public String getName() {
        return name;
    }
}
