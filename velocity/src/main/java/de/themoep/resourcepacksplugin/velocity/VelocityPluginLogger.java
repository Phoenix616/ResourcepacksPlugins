package de.themoep.resourcepacksplugin.velocity;

/*
 * ResourcepacksPlugins - velocity
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

import de.themoep.resourcepacksplugin.core.PluginLogger;
import de.themoep.utils.lang.LangLogger;
import org.slf4j.Logger;

import java.util.logging.Level;

public class VelocityPluginLogger implements LangLogger, PluginLogger {
    private final Logger logger;

    public VelocityPluginLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String message) {
        log(level, message, null);
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        if (level.intValue() < Level.FINER.intValue()) {
            logger.trace(message, throwable);
        } else if (level.intValue() < Level.INFO.intValue()) {
            logger.debug(message, throwable);
        } else if (level.intValue() < Level.WARNING.intValue()) {
            logger.info(message, throwable);
        } else if (level.intValue() < Level.SEVERE.intValue()) {
            logger.warn(message, throwable);
        } else {
            logger.error(message, throwable);
        }
    }
}
