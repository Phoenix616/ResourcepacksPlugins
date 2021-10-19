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

import de.themoep.resourcepacksplugin.core.ClientType;
import de.themoep.resourcepacksplugin.core.MinecraftVersion;
import de.themoep.resourcepacksplugin.core.PackAssignment;
import de.themoep.resourcepacksplugin.core.ResourcePack;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlayer;
import de.themoep.resourcepacksplugin.core.ResourcepacksPlugin;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Created by Phoenix616 on 03.02.2016.
 */
public class ResourcepacksPluginCommandExecutor extends PluginCommandExecutor {

    public ResourcepacksPluginCommandExecutor(ResourcepacksPlugin plugin) {
        super(plugin, null, plugin.getName().toLowerCase(Locale.ROOT).charAt(0) + "rp", plugin.getName().toLowerCase(Locale.ROOT) + ".command", new String[]{plugin.getName().toLowerCase(Locale.ROOT)});
        registerSubCommands(
                new PluginCommandExecutor(plugin, this, "reload") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (plugin.isEnabled()) {
                            boolean resend = args.length > 0 && "resend".equalsIgnoreCase(args[0]);
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
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        plugin.sendMessage(sender, "command.version",
                                "plugin", plugin.getName(),
                                "version", plugin.getVersion()
                        );
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "generatehashes") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        plugin.getPackManager().generateHashes(sender);
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "addpack <name> <url>") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
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
                new PluginCommandExecutor(plugin, this, "removepack <name>") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length < 1) {
                            return false;
                        }
                        ResourcePack pack = plugin.getPackManager().getByName(args[0]);
                        if (pack == null) {
                            sendMessage(sender, "unknown-pack", "input", args[0]);
                            return true;
                        }
                        plugin.getPackManager().removePack(pack);
                        plugin.saveConfigChanges();
                        sendMessage(sender, "removed", pack.getReplacements());
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "pack <pack> [url|hash|format|restricted|permission|type]") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
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
                            sendPackInfo(sender, pack);
                            return false;
                        } else if (args.length == 2) {
                            return false;
                        } else if ("url".equalsIgnoreCase(args[1])) {
                            save = plugin.getPackManager().setPackUrl(pack, args[2]);
                            sendMessage(sender, "updated", "pack", pack.getName(), "type", "url", "value", pack.getUrl());
                        } else if ("hash".equalsIgnoreCase(args[1])) {
                            try {
                                save = plugin.getPackManager().setPackHash(pack, args[2]);
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "hash", "value", pack.getHash());
                            } catch (IllegalArgumentException e) {
                                sendMessage(sender, "invalid-input", "expected", "40 characters long sha1 hash sum", "input", args[2]);
                            }
                        } else if ("permission".equalsIgnoreCase(args[1])) {
                            save = pack.setPermission(args[2]);
                            sendMessage(sender, "updated", "pack", pack.getName(), "type", "permission", "value", pack.getPermission());
                        } else if ("type".equalsIgnoreCase(args[1])) {
                            try {
                                save = pack.setType(ClientType.valueOf(args[2].toUpperCase(Locale.ROOT)));
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "type", "value", pack.getType().humanName());
                            } catch (IllegalArgumentException e) {
                                sendMessage(sender, "invalid-input", "expected", "type (" + Arrays.stream(ClientType.values()).map(ClientType::humanName).collect(Collectors.joining(", ")) + ")", "input", args[2]);
                            }
                        } else if ("format".equalsIgnoreCase(args[1])) {
                            try {
                                save = pack.setFormat(Integer.parseInt(args[2]));
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "format", "value", String.valueOf(pack.getFormat()));
                            } catch (NumberFormatException e) {
                                sendMessage(sender, "invalid-input", "expected", "number", "input", args[2]);
                            }
                        } else if ("version".equalsIgnoreCase(args[1])) {
                            try {
                                save = pack.setVersion(Integer.parseInt(args[2]));
                                sendMessage(sender, "updated", "pack", pack.getName(), "type", "version", "value", String.valueOf(pack.getVersion()));
                            } catch (NumberFormatException e) {
                                try {
                                    save = pack.setVersion(MinecraftVersion.parseVersion(args[2]).getProtocolNumber());
                                    sendMessage(sender, "updated", "pack", pack.getName(), "type", "version", "value", args[2]);
                                } catch (IllegalArgumentException e1) {
                                    sendMessage(sender, "invalid-input", "expected", "version", "input", args[2]);
                                }
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

                    private void sendPackInfo(ResourcepacksPlayer sender, ResourcePack pack) {
                        if (pack.getVariants().isEmpty()) {
                            sendMessage(sender, "info", pack.getReplacements());
                        } else {
                            sendMessage(sender, "info-variants", pack.getReplacements());
                            for (ResourcePack variant : pack.getVariants()) {
                                sendPackInfo(sender, variant);
                            }
                        }
                    }
                },
                new PluginCommandExecutor(plugin, this, "listassignments", null, "assignments") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        sendMessage(sender, "head");
                        plugin.getPackManager().getAssignments().forEach(a -> sendMessage(sender, "entry", a.getReplacements()));
                        return true;
                    }
                },
                new PluginCommandExecutor(plugin, this, "deleteassignment <assignment>", null, "removeassignment") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        if (args.length == 0) {
                            return false;
                        }

                        PackAssignment assignment = plugin.getPackManager().getAssignmentByName(args[0]);
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
                new PluginCommandExecutor(plugin, this, "assignment <assignment>", null, "assign") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
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
                new PluginCommandExecutor(plugin, this, "globalassignment", null, "global") {
                    @Override
                    public boolean run(ResourcepacksPlayer sender, String[] args) {
                        boolean success = plugin.getPackManager().getGlobalAssignment().update(this, sender, args);
                        plugin.getPackManager().setDirty(true);
                        plugin.getPackManager().checkDirty();
                        return success;
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
