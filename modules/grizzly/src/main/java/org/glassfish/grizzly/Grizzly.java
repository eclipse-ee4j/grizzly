/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import java.io.IOException;
import java.util.Properties;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class contains information about Grizzly framework.
 * 
 * @author Charlie Hunt
 * @author Hubert Iwaniuk
 */
public class Grizzly {
    private static final Pattern versionPattern = Pattern.compile("((\\d+)\\.(\\d+)(\\.\\d+)*){1}(?:-(.+))?");
    
    public static final AttributeBuilder DEFAULT_ATTRIBUTE_BUILDER = AttributeBuilder.DEFAULT_ATTRIBUTE_BUILDER;
    
    private static final String dotedVersion;
    private static final int major;
    private static final int minor;

    private static boolean isTrackingThreadCache;
    
    public static Logger logger(Class clazz) {
        return Logger.getLogger(clazz.getName());
    }

    /** Reads version from properties and parses it. */
    static {
        InputStream is = null;
        Properties prop = new Properties();
        try {
            is = Grizzly.class.getResourceAsStream("version.properties");
            prop.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
        }
        String version = prop.getProperty("grizzly.version");
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches()) {
            dotedVersion = matcher.group(1);
            major = Integer.parseInt(matcher.group(2));
            minor = Integer.parseInt(matcher.group(3));
        } else {
            dotedVersion = "no.version";
            major = -1;
            minor = -1;
        }
    }

    public static void main(String[] args) {
        System.out.println(Grizzly.getDotedVersion());
    }

    /**
     * Return the dotted version of the current release.
     *
     * @return like "2.0.1"
     */
    public static String getDotedVersion() {
        return dotedVersion;
    }

    /**
     * Get Grizzly framework major version
     * 
     * @return Grizzly framework major version
     */
    public static int getMajorVersion() {
        return major;
    }
    
    /**
     * Get Grizzly framework minor version
     * 
     * @return Grizzly framework minor version
     */
    public static int getMinorVersion(){
        return minor;
    }
    
    /**
     * Checks if current Grizzly framework version equals to one passed
     * 
     * @param major Grizzly framework major version
     * @param minor Grizzly framework minor version
     * @return true, if versions are equal; false otherwise
     */
    public static boolean equalVersion(int major, int minor) {
        return minor == Grizzly.minor && major == Grizzly.major;
    }

    public static boolean isTrackingThreadCache() {
        return isTrackingThreadCache;
    }

    public static void setTrackingThreadCache(boolean isTrackingThreadCache) {
        Grizzly.isTrackingThreadCache = isTrackingThreadCache;
    }
}
