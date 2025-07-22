# ResourcepacksPlugins
[![Total downloads](https://img.shields.io/github/downloads/Phoenix616/ResourcepacksPlugins/total.svg)](https://github.com/Phoenix616/ResourcepacksPlugins/releases) ![Work time](https://buttons.phoenix616.dev//ResourcepacksPluginsWork.svg)

Repository for the different resourcepacks plugins by Phoenix616: [BungeeResourcepacks](https://modrinth.com/plugin/bungeeresourcepacks), [WorldResourcepacks](https://modrinth.com/plugin/worldresourcepacks) and [VelocityResourcepacks](https://modrinth.com/plugin/velocityresourcepacks).

These plugins allow you to manage resourcepacks on your server. They can be used to download packs per server or world or apply them globally on the whole network as well as give players the ability to select packs themselves as well as send packs based on the player's client version.

> [!TIP]
> If you want to support the development of my projects then take a look at the enhanced version of this plugin which adds more features and is available as a [premium resource on SpigotMC](https://frp.pheonix616.dev/).
> 
> It adds the ability to force players to download the server resourcepack as well as configure unique actions based on the loading (or not) of the pack. It also integrates with PlaceholderAPI and WorldGuard.
> 
> **It is still Free Software licensed under GPLv3+ with buyers being able to access and re-use the source code as they see fit!**

## Development

Dev builds: https://ci.minebench.de/job/ResourcepacksPlugins/

### Integrating

Javadocs: https://docs.phoenix616.dev/resourcepacksplugins/

You can easily depend on it via maven (or gradle). The core contains all the management and the bukkit/bungee artifacts the platform depended code.

#### Repository
```xml
    <repositories>
        <repository>
            <id>minebench-repo</id>
            <url>https://repo.minebench.de/</url>
        </repository>
    </repositories>
```

#### Artifacts
```xml
    <dependencies>
        <dependency>
            <groupId>de.themoep.resourcepacksplugin</groupId>
            <artifactId>bukkit</artifactId>
            <version>1.9.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>de.themoep.resourcepacksplugin</groupId>
            <artifactId>bungee</artifactId>
            <version>1.9.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>de.themoep.resourcepacksplugin</groupId>
            <artifactId>velocity</artifactId>
            <version>1.9.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>de.themoep.resourcepacksplugin</groupId>
            <artifactId>core</artifactId>
            <version>1.9.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

## [License](LICENSE)

The plugin is licensed under the GPLv3:

```
ResourcepacksPlugins
Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```