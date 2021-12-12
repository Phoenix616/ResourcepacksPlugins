package de.themoep.resourcepacksplugin.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/*
 * ResourcepacksPlugins - core
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

public enum MinecraftVersion {
    MINECRAFT_1_18_1(757),
    MINECRAFT_1_18(757),
    MINECRAFT_1_17_1(756),
    MINECRAFT_1_17(755),
    MINECRAFT_1_16_5(754),
    MINECRAFT_1_16_4(754),
    MINECRAFT_1_16_3(753),
    MINECRAFT_1_16_2(751),
    MINECRAFT_1_16_1(736),
    MINECRAFT_1_16(735),
    MINECRAFT_1_15_2(578),
    MINECRAFT_1_15_1(575),
    MINECRAFT_1_15(573),
    MINECRAFT_1_14_4(498),
    MINECRAFT_1_14_3(490),
    MINECRAFT_1_14_2(485),
    MINECRAFT_1_14_1(480),
    MINECRAFT_1_14(477),
    MINECRAFT_1_13_2(404),
    MINECRAFT_1_13_1(401),
    MINECRAFT_1_13(393),
    MINECRAFT_1_12_2(340),
    MINECRAFT_1_12_1(338),
    MINECRAFT_1_12(335),
    MINECRAFT_1_11_2(316),
    MINECRAFT_1_11_1(316),
    MINECRAFT_1_11(315),
    MINECRAFT_1_10(210),
    MINECRAFT_1_9_4(110),
    MINECRAFT_1_9_2(109),
    MINECRAFT_1_9_1(108),
    MINECRAFT_1_9(107),
    MINECRAFT_1_8(47),
    MINECRAFT_1_7_6(5),
    MINECRAFT_1_7_2(4),
    UNKNOWN(0);

    private final int number;
    private static Map<Integer, MinecraftVersion> numbers;

    static {
        numbers = new LinkedHashMap<>();
        for(MinecraftVersion version : values()) {
            numbers.put(version.number, version);
        }
    }

    MinecraftVersion(int versionNumber) {
        this.number = versionNumber;
    }

    /**
     * Parse the version from a string. The string can either be a string representation of the version or the protocol number
     * @param versionString The string representation of the version or the protocol number
     * @return The MinecraftVersion
     * @throws IllegalArgumentException If no version can be parsed from the string
     */
    public static MinecraftVersion parseVersion(String versionString) throws IllegalArgumentException {
        try {
            int parsedVersion = Integer.parseInt(versionString);
            MinecraftVersion version = getVersion(parsedVersion);
            if (parsedVersion == 0 || version != UNKNOWN) {
                return version;
            }
        } catch (NumberFormatException ignored) {}
        String getVersion = versionString.toUpperCase().replace('.', '_');
        if (!getVersion.startsWith("MINECRAFT_")) {
            getVersion = "MINECRAFT_" + getVersion;
        }
        try {
            return valueOf(getVersion);
        } catch (IllegalArgumentException ignored) {}

        throw new IllegalArgumentException(versionString + " is not a valid MinecraftVersion definition!");
    }

    /**
     * Get the Minecraft version from the protocol version
     * @param protocolVersion The protocol version
     * @return The Minecraft version or {@link MinecraftVersion#UNKNOWN} if not known
     */
    public static MinecraftVersion getVersion(int protocolVersion) {
        MinecraftVersion minecraftVersion = numbers.get(protocolVersion);
        if (minecraftVersion != null) {
            return minecraftVersion;
        }
        for(MinecraftVersion version : values()) {
            if(version.getProtocolNumber() <= protocolVersion) {
                return version;
            }
        }
        return UNKNOWN;
    }

    /**
     * Gets the Minecraft version that matches the protocol version exactly
     * @param protocolVersion The protocol version
     * @return The Minecraft version or null if not known
     */
    public static MinecraftVersion getExactVersion(int protocolVersion) {
        return numbers.get(protocolVersion);
    }

    /**
     * Get the human readable config string for this version which can be parsed by parseVersion
     * @return The human readable config string
     */
    public String toConfigString() {
        return this == UNKNOWN ? name() : name().toLowerCase(Locale.ROOT).substring("MINECRAFT_".length()).replace('_', '.');
    }

    public int getProtocolNumber() {
        return number;
    }
}
