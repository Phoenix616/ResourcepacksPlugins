package de.themoep.resourcepacksplugin.core.commands;


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

import de.themoep.resourcepacksplugin.core.PackAssignment;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.Arrays;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPluginCommandExecutor extends PluginCommandExecutor {

    public ResourcepacksPluginCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin, null, plugin.getName().toLowerCase().charAt(0) + "rp", plugin.getName().toLowerCase() + ".command", new String[]{plugin.getName().toLowerCase()});
        registerSubCommands(
                new PluginCommandExecutor(plugin, this, "reload") {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (plugin.isEnabled()) {
                            boolean resend = args.length > 1 && "resend".equalsIgnoreCase(args[1]);
                            plugin.reloadConfig(resend);
                            plugin.sendMessage(sender, "command.reloaded",
                                    "plugin", plugin.getName(),
                                    "optional-resend", resend ? plugin.getMessage(sender, "command.optional-resend") : ""
                            );
                        } else {
                            plugin.sendMessage(sender, "command.not-enabled", "plugin", plugin.getName());
                        }
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "version") {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        plugin.sendMessage(sender, "command.version",
                                "plugin", plugin.getName(),
                                "version", plugin.getVersion()
                        );
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "generatehashes") {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        plugin.getPackManager().generateHashes(sender);
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "addpack <name> <url>") {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length < 2) {
                            return false;
                        }
                        if (plugin.getPackManager().getByName(args[0]) != null) {
                            sendMessage(sender, "name-already-used", "name", args[0]);
                            return true;
                        }
                        ResourcePack urlPack = plugin.getPackManager().getByUrl(args[1]);
                        if (urlPack != null) {
                            sendMessage(sender, "url-already-used", "name", urlPack.getName(), "url", urlPack.getUrl());
                            return true;
                        }
                        ResourcePack pack = new ResourcePack(args[0], args[1], null);
                        plugin.getPackManager().addPack(pack);
                        plugin.saveConfigChanges();
                        sendMessage(sender, "added", pack.getReplacements());
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "pack <pack> [url|hash|format|restricted|permission]") {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length == 0) {
                            return false;
                        }

                        ResourcePack pack = plugin.getPackManager().getByName(args[0]);
                        if (pack == null) {
                            sendMessage(sender, "unknown-pack", "input", args[0]);
                            return true;
                        }

                        boolean save = false;
                        if (args.length == 1) {
                            sendMessage(sender, "info", pack.getReplacements());
                            return false;
                        } else if (args.length == 2) {
                            return false;
                        } else if ("url".equalsIgnoreCase(args[1])) {
                            save = plugin.getPackManager().setPackUrl(pack, args[2]);
                            sendMessage(sender, "updated", "pack", pack.getName(), "type", "url", "value", pack.getUrl());
                        } else if ("hash".equalsIgnoreCase(args[1])) {
                            save = plugin.getPackManager().setPackHash(pack, args[2]);
                            sendMessage(sender, "updated", "pack", pack.getName(), "type", "hash", "value", pack.getHash());
                        } else if ("permission".equalsIgnoreCase(args[1])) {
                            save = pack.setPermission(args[2]);
                            sendMessage(sender, "updated", "pack", pack.getName(), "type", "permission", "value", pack.getPermission());
                        } else if ("format".equalsIgnoreCase(args[1])) {
                            try {
                                save = pack.setFormat(Integer.parseInt(args[2]));
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "permission", "value", pack.getPermission());
                            } catch (NumberFormatException e) {
                                sendMessage(sender, "invalid-input", "expected", "number", "input", args[2]);
                            }
                        } else if ("restricted".equalsIgnoreCase(args[1])) {
                            try {
                                save = pack.setRestricted(Boolean.parseBoolean(args[2]));
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "restricted", "value", String.valueOf(pack.isRestricted()));
                            } catch (NumberFormatException e) {
                                sendMessage(sender, "invalid-input", "expected", "boolean", "input", args[2]);
                            }
                        } else {
                            return false;
                        }

                        if (save) {
                            plugin.saveConfigChanges();
                        }

                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "listassignments", null, new String[] {"assignments"}) {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        sendMessage(sender, "head");
                        plugin.getPackManager().getAssignments().forEach(a -> sendMessage(sender, "entry", a.getReplacements()));
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "deleteassignment <assignment>", null, new String[] {"removeassignment"}) {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length == 0) {
                            return false;
                        }

                        PackAssignment assignment = plugin.getPackManager().getAssignment(args[0]);
                        if (assignment == null) {
                            sendMessage(sender, "unknown-assignment", "input", args[0]);
                            return false;
                        }
                        plugin.getPackManager().setDirty(true);
                        plugin.getPackManager().removeAssignment(assignment);
                        sendMessage(sender, "deleted", "name", assignment.getName());
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "assignment <assignment>", null, new String[] {"assign"}) {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length == 0) {
                            return false;
                        }

                        PackAssignment assignment = plugin.getPackManager().getAssignmentByName(args[0]);
                        if (assignment == null) {
                            sendMessage(sender, "new-assignment", "input", args[0]);
                            assignment = new PackAssignment(args[0]);
                        } else {
                            plugin.getPackManager().removeAssignment(assignment);
                        }

                        boolean success = assignment.update(this, sender, Arrays.copyOfRange(args, 1, args.length));
                        plugin.getPackManager().setDirty(true);
                        plugin.getPackManager().addAssignment(assignment);
                        return success;
                    }
                },
                new PluginCommandExecutor(plugin, this, "globalassignment", null, new String[] {"global"}) {
                    @Override
                    boolean run(ResourcepacksPlayer sender, String[] args) {
                        return plugin.getPackManager().getGlobalAssignment().update(this, sender, args);
                    }

                    @Override
                    public String getKey() {
                        return ResourcepacksPluginCommandExecutor.this.getKey() + ".assignment";
                    }
                }
        );
    }

    public boolean run(ResourcepacksPlayer sender, String[] args) {
        return false;
    }
}
