package de.themoep.resourcepacksplugin.core;

/*
 * ResourcepacksPlugins - core
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

import de.themoep.resourcepacksplugin.core.commands.PluginCommandExecutor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Phoenix616 on 06.03.2017.
 */
public class PackAssignment {

    private LinkedHashSet<String> packs = new LinkedHashSet<>();
    private LinkedHashSet<String> optionalPacks = new LinkedHashSet<>();
    private long sendDelay = -1;
    private Pattern regex = null;
    private final String name;

    public PackAssignment(String name) {
        this.name = name;
    }

    public PackAssignment(PackAssignment assignment) {
        this(assignment.getName());
        this.packs.addAll(assignment.getPacks());
        this.optionalPacks.addAll(assignment.getOptionalPacks());
        this.sendDelay = assignment.getSendDelay();
        this.regex = assignment.getRegex();
    }

    /**
     * Set the main pack of this assignment
     * @param pack  The main pack
     * @return      Whether or not the value changed
     * @deprecated Use {@link #addPack(ResourcePack)} or {@link #removePack(ResourcePack)}
     */
    @Deprecated
    public boolean setPack(ResourcePack pack) {
        if (pack == null) {
            return setPack((String) null);
        }
        return setPack(pack.getName());
    }

    /**
     * Set the main pack of this assignment
     * @param pack  The name of the main pack
     * @return      Whether or not the value changed
     * @deprecated Use {@link #addPack(String)} or {@link #removePack(String)}
     */
    @Deprecated
    public boolean setPack(String pack) {
        if (pack == null) {
            if (packs.isEmpty()) {
                return false;
            }
            packs.clear();
            return true;
        }
        if (packs.size() == 1 && packs.contains(pack.toLowerCase(Locale.ROOT))) {
            return false;
        }
        packs.clear();
        addPack(pack);
        return true;
    }

    /**
     * Get the name of the main pack of this assignment
     * @return  The (lowercase) name of the pack
     * @deprecated Use {@link #getPacks()}
     */
    @Deprecated
    public String getPack() {
        return packs.stream().findFirst().orElse(null);
    }

    /**
     * Get the list of packs
     * @return The (lowercase) names of main packs
     */
    public LinkedHashSet<String> getPacks() {
        return packs;
    }

    /**
     * Check whether a certain pack is a pack in this assignment
     * @param pack  The name of the pack
     * @return      <code>true</code> if this pack list contains this pack; <code>false</code> if not
     */
    public boolean isPack(String pack) {
        return packs.contains(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Check whether certain pack is a pack in this assignment
     * @param pack  The pack
     * @return      <code>true</code> if this pack list contains this pack; <code>false</code> if not (or pack is null)
     */
    public boolean isPack(ResourcePack pack) {
        return pack != null && isPack(pack.getName());
    }

    /**
     * Add a new pack
     * @param pack  The pack to add
     * @return      <code>true</code> as defined in Collections.add
     */
    public boolean addPack(ResourcePack pack) {
        return addPack(pack.getName());
    }

    /**
     * Add a new pack
     * @param pack  The name of the pack to add
     * @return      <code>true</code> as defined in Collections.add
     */
    public boolean addPack(String pack) {
        return packs.add(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Remove a pack
     * @param pack  The pack to remove
     * @return      <code>true</code> if that pack was a main pack, <code>false</code> if not
     */
    public boolean removePack(ResourcePack pack) {
        return removePack(pack.getName());
    }

    /**
     * Remove a pack
     * @param pack  The name of the pack to remove
     * @return      <code>true</code> if that pack was a main pack, <code>false</code> if not
     */
    public boolean removePack(String pack) {
        return packs.remove(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Get a list of optional packs
     * @return  The (lowercase) names of optional packs
     */
    public LinkedHashSet<String> getOptionalPacks() {
        return optionalPacks;
    }

    /**
     * Check whether a certain pack is an optional pack in this assignment
     * @param pack  The name of the pack
     * @return      <code>true</code> if this optional pack list contains this pack; <code>false</code> if not
     */
    public boolean isOptionalPack(String pack) {
        return optionalPacks.contains(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Check whether certain pack is an optional pack in this assignment
     * @param pack  The pack
     * @return      <code>true</code> if this optional pack list contains this pack; <code>false</code> if not (or pack is null)
     */
    public boolean isOptionalPack(ResourcePack pack) {
        return pack != null && isOptionalPack(pack.getName());
    }

    /**
     * Add a new optional pack
     * @param pack  The pack to add
     * @return      <code>true</code> as defined in Collections.add
     */
    public boolean addOptionalPack(ResourcePack pack) {
        return addOptionalPack(pack.getName());
    }

    /**
     * Add a new optional pack
     * @param pack  The name of the pack to add
     * @return      <code>true</code> as defined in Collections.add
     */
    public boolean addOptionalPack(String pack) {
        return optionalPacks.add(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Remove an optional pack
     * @param pack  The pack to remove
     * @return      <code>true</code> if that pack was a optional one, <code>false</code> if not
     */
    public boolean removeOptionalPack(ResourcePack pack) {
        return removeOptionalPack(pack.getName());
    }

    /**
     * Remove an optional pack
     * @param pack  The name of the pack to remove
     * @return      <code>true</code> if that pack was a optional one, <code>false</code> if not
     */
    public boolean removeOptionalPack(String pack) {
        return optionalPacks.remove(pack.toLowerCase(Locale.ROOT));
    }

    /**
     * Get a list of secondary packs
     * @return  The (lowercase) names of secondary packs
     * @deprecated Use {@link #getOptionalPacks()}
     */
    @Deprecated
    public LinkedHashSet<String> getSecondaries() {
        return getOptionalPacks();
    }

    /**
     * Check whether or not a certain pack is a secondary in this assignment
     * @param pack  The name of the pack
     * @return      <code>true</code> if this secondary list contains this pack; <code>false</code> if not
     * @deprecated  Use {@link #isOptionalPack(String)}
     */
    @Deprecated
    public boolean isSecondary(String pack) {
        return isOptionalPack(pack);
    }

    /**
     * Check whether or not a certain pack is a secondary in this assignment
     * @param pack  The the pack
     * @return      <code>true</code> if this secondary list contains this pack; <code>false</code> if not (or pack is null)
     * @deprecated  Use {@link #isOptionalPack(ResourcePack)}
     */
    @Deprecated
    public boolean isSecondary(ResourcePack pack) {
        return isOptionalPack(pack);
    }

    /**
     * Add a new secondary pack
     * @param pack  The pack to add
     * @return      <code>true</code> as defined in Collections.add
     * @deprecated  Use {@link #addOptionalPack(ResourcePack)}
     */
    @Deprecated
    public boolean addSecondary(ResourcePack pack) {
        return addOptionalPack(pack);
    }

    /**
     * Add a new secondary pack
     * @param pack  The name of the pack to add
     * @return      <code>true</code> as defined in Collections.add
     * @deprecated  Use {@link #addOptionalPack(String)}
     */
    @Deprecated
    public boolean addSecondary(String pack) {
        return addOptionalPack(pack);
    }

    /**
     * Remove a secondary pack
     * @param pack  The pack to remove
     * @return      <code>true</code> if that pack was a secondary one, <code>false</code> if not
     * @deprecated  Use {@link #removeOptionalPack(ResourcePack)}
     */
    @Deprecated
    public boolean removeSecondary(ResourcePack pack) {
        return removeOptionalPack(pack);
    }

    /**
     * Remove a secondary pack
     * @param pack  The name of the pack to remove
     * @return      <code>true</code> if that pack was a secondary one, <code>false</code> if not
     * @deprecated  Use {@link #removeOptionalPack(String)}
     */
    @Deprecated
    public boolean removeSecondary(String pack) {
        return removeOptionalPack(pack);
    }

    /**
     * Check whether or not this assignment contains any pack settings
     * @return  <code>true</code> if it has no packs or secondaries; <code>false</code> if it has some
     */
    public boolean isEmpty() {
        return packs.isEmpty() && optionalPacks.isEmpty() && sendDelay == -1;
    }

    /**
     * Set the delay in ticks  to wait before sending the packs from this assignment
     * @param sendDelay The delay in ticks
     * @return          Whether or not the value changed
     */
    public boolean setSendDelay(long sendDelay) {
        if (this.sendDelay != sendDelay) {
            this.sendDelay = sendDelay;
            return true;
        }
        return false;
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
                .append("name=").append(getName())
                .append(", packs=[").append(String.join(", ", getPacks()))
                .append("], optional-packs=[").append(String.join(", ", getOptionalPacks()))
                .append("], sendDelay=").append(getSendDelay());
        if (getRegex() != null) {
            s.append(", regex=").append(getRegex().toString());
        }
        return s.append("}").toString();
    }

    /**
     * Set the key name regex of this assignment
     * @param regex The compiled Pattern of this regex
     * @return      Whether or not the value changed
     */
    public boolean setRegex(Pattern regex) {
        if (this.regex == null || regex == null || !this.regex.toString().equals(regex.toString())) {
            if ((this.regex == null && regex != null) || (this.regex != null && regex == null)) {
                this.regex = regex;
                return true;
            }
        }
        return false;
    }

    /**
     * Get the compiled Pattern of this assignment's key regex
     * @return The compiled regex pattern or <code>null</code> if none is set and the key should be used literally
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

    /**
     * Serialize this assignment to a map
     * @return A map holding the data of this object
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (packs.size() > 1) {
            map.put("packs", packs);
            map.put("pack", null);
        } else if (packs.isEmpty()) {
            map.put("packs", null);
            map.put("pack", null);
        } else {
            map.put("packs", null);
            map.put("pack", packs.iterator().next());
        }
        map.put("optional-packs", optionalPacks.isEmpty() ? null : new ArrayList<>(optionalPacks));
        map.put("send-delay", sendDelay > 0 ? sendDelay : null);
        map.put("regex", regex != null ? regex.toString() : null);
        return map;
    }

    /**
     * Get replacements
     * @return The placeholder replacements of this pack as an array. Index n is the placeholder, n+1 the value.
     */
    public String[] getReplacements() {
        return new String[] {
                "name", getName(),
                "pack", getPack() != null ? getPack() : "none",
                "packs", getPacks().isEmpty() ? "none" : String.join(", ", getPacks()),
                "secondaries", String.join(", ", getOptionalPacks()),
                "optional-packs", String.join(", ", getOptionalPacks()),
                "regex", getRegex() != null ? getRegex().toString() : "none",
                "send-delay", String.valueOf(getSendDelay())
        };
    }

    /**
     * Get all possible update actions
     * @return The possible update actions
     */
    protected String[] getUpdateActions() {
        return new String[] {
                "info",
                "addpack",
                "removepack",
                "addoptionalpack",
                "removeoptionalpack",
                "regex",
                "senddelay"
        };
    }

    /**
     * Update this assignment
     * @param command   The command triggering the update
     * @param sender    The sender updating it
     * @param args      The arguments for updating the assignment
     * @return Whether or not the update completed properly
     */
    public boolean update(PluginCommandExecutor command, ResourcepacksPlayer sender, String[] args) {
        boolean save;
        if (args.length == 0) {
            command.sendMessage(sender, "info", getReplacements());
            command.sendMessage(sender, "usage",
                    "command", command.getPath(),
                    "name", getName(),
                    "usage", command.getUsage() + " " + ChatColor.usage(String.join(" | ", getUpdateActions())),
                    "permission", command.getPermission(),
                    "subcommands", String.join("|", getUpdateActions())
            );
            return true;
        } else if ("info".equalsIgnoreCase(args[0])) {
            command.sendMessage(sender, "info", getReplacements());
            return true;
        } else if ("addpack".equalsIgnoreCase(args[0])) {
            ResourcePack pack = null;
            if (args.length > 1) {
                pack = command.getPlugin().getPackManager().getByName(args[1]);
                if (pack == null) {
                    command.sendMessage(sender, "unknown-pack", "input", args[1]);
                    return true;
                }
            }
            save = addPack(pack);
            command.sendMessage(sender, "updated", "assignment", getName(), "type", "added pack", "value", pack.getName());
        } else if ("removepack".equalsIgnoreCase(args[0])) {
            ResourcePack pack = null;
            if (args.length > 1) {
                pack = command.getPlugin().getPackManager().getByName(args[1]);
                if (pack == null) {
                    command.sendMessage(sender, "unknown-pack", "input", args[1]);
                    return true;
                }
            }
            save = removePack(pack);
            command.sendMessage(sender, "updated", "assignment", getName(), "type", "removed pack", "value", pack.getName());
        } else if ("regex".equalsIgnoreCase(args[0])) {
            Pattern regex = null;
            if (args.length > 1) {
                try {
                    regex = Pattern.compile(args[1]);
                } catch (PatternSyntaxException e) {
                    command.sendMessage(sender, "invalid-input", "expected", "regex", "input", args[1] + " (" + e.getMessage() + ")");
                    return true;
                }
            }
            save = setRegex(regex);
            command.sendMessage(sender, "updated", "assignment", getName(), "type", "regex", "value", regex != null ? regex.toString() : "none");
        } else if ("senddelay".equalsIgnoreCase(args[0])) {
            long sendDelay = -1;
            if (args.length > 1) {
                try {
                    sendDelay = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    command.sendMessage(sender, "invalid-input", "expected", "long", "input", args[1]);
                    return true;
                }
            }
            save = setSendDelay(sendDelay);
            command.sendMessage(sender, "updated", "assignment", getName(), "type", "send delay", "value", sendDelay > -1 ? String.valueOf(sendDelay) : "none");
        } else if (args.length < 2) {
            return false;
        } else if ("addoptionalpack".equalsIgnoreCase(args[0])
                || "addoptional".equalsIgnoreCase(args[0])
                || "addsecondary".equalsIgnoreCase(args[0])) {
            ResourcePack pack = command.getPlugin().getPackManager().getByName(args[1]);
            if (pack == null) {
                command.sendMessage(sender, "unknown-pack", "input", args[1]);
                return true;
            }
            save = addOptionalPack(pack);
            if (save) {
                command.sendMessage(sender, "optional-pack.added", "assignment", getName(), "pack", pack.getName());
            } else {
                command.sendMessage(sender, "optional-pack.already-added", "assignment", getName(), "pack", pack.getName());
            }
        } else if ("removeoptionalpack".equalsIgnoreCase(args[0])
                || "removeoptional".equalsIgnoreCase(args[0])
                || "removesecondary".equalsIgnoreCase(args[0])) {
            if (command.getPlugin().getPackManager().getByName(args[1]) == null) {
                command.sendMessage(sender, "unknown-pack", "input", args[1]);
            }
            save = removeOptionalPack(args[1]);
            if (save) {
                command.sendMessage(sender, "optional-pack.removed", "assignment", getName(), "pack", args[1]);
            } else {
                command.sendMessage(sender, "optional-pack.not-added", "assignment", getName(), "pack", args[1]);
            }
        } else {
            return false;
        }

        if (save) {
            command.getPlugin().getPackManager().setDirty(true);
        }

        return true;
    }
}
