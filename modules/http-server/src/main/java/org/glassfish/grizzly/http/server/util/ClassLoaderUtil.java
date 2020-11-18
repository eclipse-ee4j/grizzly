/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;

/**
 * Simple {@link ClassLoader} utility.
 *
 * @author Jeanfrancois Arcand
 */
public class ClassLoaderUtil {
    private static final Logger LOGGER = Grizzly.logger(ClassLoaderUtil.class);

    /**
     * Create a class loader that can load classes from the specified file directory. The file directory must contains .jar
     * or .zip
     *
     * @param libDir Directory with jars.
     * @param cl the parent {@link ClassLoader}, or null if none.
     * @return A {@link URLClassLoader} that can load classes from a directory that contains jar and zip files.
     * @throws java.io.IOException I/O fail
     * @deprecated removal candidate, never used
     */
    @Deprecated
    public static ClassLoader createClassloader(File libDir, ClassLoader cl) throws IOException {
        URLClassLoader urlClassloader = null;
        if (libDir.exists()) {
            if (libDir.isDirectory()) {
                String[] jars = libDir.list(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar") || name.endsWith(".zip");
                    }
                });
                URL[] urls = new URL[jars.length];
                for (int i = 0; i < jars.length; i++) {
                    String path = new File(libDir.getName() + File.separator + jars[i]).getCanonicalFile().toURI().toURL().toString();
                    urls[i] = new URL(path);
                }
                urlClassloader = createClassLoaderWithSecCheck(urls, cl);
            }
        }
        return urlClassloader;
    }

    /**
     * Construct a {@link URLClassLoader} based on a canonical file location.
     * 
     * @param dirPath a canonical path location
     * @return a {@link URLClassLoader}
     * @throws java.io.IOException I/O
     * @throws java.net.MalformedURLException Invalid URL
     */
    public static URLClassLoader createURLClassLoader(String dirPath) throws IOException {

        String path;
        File file;
        URL appRoot;
        URL classesURL;

        if (!dirPath.endsWith(File.separator) && !dirPath.endsWith(".war") && !dirPath.endsWith(".jar")) {
            dirPath += File.separator;
        }

        // Must be a better way because that sucks!
        String separator = System.getProperty("os.name").toLowerCase().startsWith("win") ? "/" : "//";

        if (dirPath != null && (dirPath.endsWith(".war") || dirPath.endsWith(".jar"))) {
            file = new File(dirPath);
            appRoot = new URL("jar:file:" + separator + file.getCanonicalPath().replace('\\', '/') + "!/");
            classesURL = new URL("jar:file:" + separator + file.getCanonicalPath().replace('\\', '/') + "!/WEB-INF/classes/");

            path = ExpandJar.expand(appRoot);

            // DEBUG
            // classesURL = new URL("file:/" + path + "WEB-INF/classes/");
            // appRoot = new URL("file:/" + path);

        } else {
            path = dirPath;
            classesURL = new URL("file://" + path + "WEB-INF/classes/");
            appRoot = new URL("file://" + path);
        }

        String absolutePath = new File(path).getAbsolutePath();
        URL[] urls;
        File libFiles = new File(absolutePath + File.separator + "WEB-INF" + File.separator + "lib");
        int arraySize = 4;

        if (libFiles.exists() && libFiles.isDirectory()) {
            urls = new URL[libFiles.listFiles().length + arraySize];
            for (int i = 0; i < libFiles.listFiles().length; i++) {
                urls[i] = new URL("jar:file:" + separator + libFiles.listFiles()[i].toString().replace('\\', '/') + "!/");
            }
        } else {
            urls = new URL[arraySize];
        }

        urls[urls.length - 1] = classesURL;
        urls[urls.length - 2] = appRoot;
        urls[urls.length - 3] = new URL("file://" + path + "/WEB-INF/classes/");
        urls[urls.length - 4] = new URL("file://" + path);

        return createClassLoaderWithSecCheck(urls, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Construct a {@link URLClassLoader} based on a canonical file location.
     *
     * @param location a canonical path location
     * @param parent {@link ClassLoader} to be used as parent for returned one.
     * @return a {@link URLClassLoader}
     * @throws java.io.IOException I/O
     * @throws java.net.MalformedURLException Invalid URL
     */
    public static URLClassLoader createURLClassLoader(String location, ClassLoader parent) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(parent);
        try {
            return createURLClassLoader(location);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    /**
     * Load a class using the current {link Thread#getContextClassLoader}
     * 
     * @param clazzName The name of the class you want to load.
     * @return an instance of clazzname
     */
    public static Object load(String clazzName) {
        return load(clazzName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Load a class using the provided {@link ClassLoader}
     * 
     * @param clazzName The name of the class you want to load.
     * @param classLoader A classloader to use for loading a class.
     * @return an instance of clazzname
     */
    public static Object load(String clazzName, ClassLoader classLoader) {
        Class className;
        try {
            className = Class.forName(clazzName, true, classLoader);
            return className.newInstance();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unable to load class " + clazzName, t);
        }

        return null;
    }

    private static URLClassLoader createClassLoaderWithSecCheck(final URL[] urls, final ClassLoader parent) {
        if (System.getSecurityManager() == null) {
            return new URLClassLoader(urls, parent);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(urls, parent);
                }
            });
        }
    }
}
