/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.grizzly.Grizzly;

/**
 *
 * @since 2.2.11
 */
public class JdkVersion implements Comparable<JdkVersion> {
    private static final Logger LOGGER = Grizzly.logger(JdkVersion.class);
    
    // take max 4 parts of the JDK version and cut the rest (usually the build number)
    private static final Pattern VERSION_PATTERN = Pattern.compile(
                "([0-9]+)(\\.([0-9]+))?(\\.([0-9]+))?([_\\.]([0-9]+))?.*");

    private static final JdkVersion UNKNOWN_VERSION = new JdkVersion(-1, -1, -1, -1);
    private static final JdkVersion JDK_VERSION = parseVersion(System.getProperty("java.version"));

    private final int major;
    private final int minor;
    private final int maintenance;
    private final int update;

    // ------------------------------------------------------------ Constructors

    private JdkVersion(final int major,
                       final int minor,
                       final int maintenance,
                       final int update) {
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        this.update = update;
    }

    // ---------------------------------------------------------- Public Methods

    public static JdkVersion parseVersion(final String versionString) {

        try {
            final Matcher matcher = VERSION_PATTERN.matcher(versionString);
            if (matcher.matches()) {
                return new JdkVersion(parseInt(matcher.group(1)),
                        parseInt(matcher.group(3)),
                        parseInt(matcher.group(5)),
                        parseInt(matcher.group(7)));
            }
            
            LOGGER.log(Level.FINE,
                    "Can't parse the JDK version {0}", versionString);
            
        } catch (Exception e) {
            LOGGER.log(Level.FINE,
                    "Error parsing the JDK version " + versionString, e);
        }

        return UNKNOWN_VERSION;
    }

    public static JdkVersion getJdkVersion() {
        return JDK_VERSION;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMajor() {
        return major;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMinor() {
        return minor;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMaintenance() {
        return maintenance;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("JdkVersion");
        sb.append("{major=").append(major);
        sb.append(", minor=").append(minor);
        sb.append(", maintenance=").append(maintenance);
        sb.append(", update=").append(update);
        sb.append('}');
        return sb.toString();
    }

    // ------------------------------------------------- Methods from Comparable

    public int compareTo(String versionString) {
        return compareTo(JdkVersion.parseVersion(versionString));
    }

    @Override
    public int compareTo(JdkVersion otherVersion) {
        if (major < otherVersion.major) {
            return -1;
        }
        if (major > otherVersion.major) {
            return 1;
        }
        if (minor < otherVersion.minor) {
            return -1;
        }
        if (minor > otherVersion.minor) {
            return 1;
        }
        if (maintenance < otherVersion.maintenance) {
            return -1;
        }
        if (maintenance > otherVersion.maintenance) {
            return 1;
        }
        if (update < otherVersion.update) {
            return -1;
        }
        if (update > otherVersion.update) {
            return 1;
        }
        return 0;
    }

    private static int parseInt(final String s) {
        return s != null ? Integer.parseInt(s) : 0;
    }
}
