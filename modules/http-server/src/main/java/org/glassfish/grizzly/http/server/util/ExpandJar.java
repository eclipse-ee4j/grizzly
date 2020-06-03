/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.http.server.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Expand out a jar. Adapted from Tomcat's org.apache.catalina.startup.ExpendWar classes
 *
 * @author Jeanfrancois Arcand
 */

public class ExpandJar {

    /**
     * Expand the jar file found at the specified URL into an unpacked directory structure, and return the absolute pathname
     * to the expanded directory.
     *
     * @param jar URL of the web application archive to be expanded (must start with "jar:")
     *
     * @return Absolute path as in {@link java.io.File#getAbsolutePath()} of location where to find expanded jar.
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered during expansion
     */
    public static String expand(URL jar) throws IOException {
        return expand(jar, System.getProperty("java.io.tmpdir"));
    }

    /**
     * Expand the jar file found at the specified URL into an unpacked directory structure, and return the absolute pathname
     * to the expanded directory.
     *
     * @param jar URL of the web application archive to be expanded (must start with "jar:")
     * 
     * @param workFolder the folder where the file will be expanded
     *
     * @return Absolute path as in {@link java.io.File#getAbsolutePath()} of location where to find expanded jar.
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered during expansion
     */
    public static String expand(URL jar, String workFolder) throws IOException {

        // Calculate the directory name of the expanded directory
        String pathname = jar.toString().replace('\\', '/');
        if (pathname.endsWith("!/")) {
            pathname = pathname.substring(0, pathname.length() - 2);
        }
        int period = pathname.lastIndexOf('.');
        if (period >= pathname.length() - 4) {
            pathname = pathname.substring(0, period);
        }
        int slash = pathname.lastIndexOf('/');
        if (slash >= 0) {
            pathname = pathname.substring(slash + 1);
        }
        return expand(jar, pathname, workFolder);

    }

    /**
     * Expand the jar file found at the specified URL into an unpacked directory structure, and return the absolute pathname
     * to the expanded directory.
     *
     * @param jar URL of the web application archive to be expanded (must start with "jar:")
     * @param pathname Context path name for web application
     *
     * @return Absolute path as in {@link java.io.File#getAbsolutePath()} of location where to find expanded jar.
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered during expansion
     */
    public static String expand(URL jar, String pathname, String dirname) throws IOException {

        // Make sure that there is no such directory already existing
        File appBase = new File(dirname);
        File docBase = new File(appBase, pathname);
        // Create the new document base directory
        if (!docBase.mkdir()) {
            throw new IllegalStateException(String.format("Unable to create directory: %s", docBase.getAbsolutePath()));
        }

        // Expand the jar into the new document base directory
        JarURLConnection juc = (JarURLConnection) jar.openConnection();
        juc.setUseCaches(false);
        JarFile jarFile = null;
        InputStream input = null;
        try {
            jarFile = juc.getJarFile();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                String name = jarEntry.getName();
                int last = name.lastIndexOf('/');
                if (last >= 0) {
                    File parent = new File(docBase, name.substring(0, last));
                    if (!parent.mkdirs()) {
                        throw new IllegalStateException(String.format("Unable to create directory: %s", parent.getAbsolutePath()));
                    }
                }
                if (name.endsWith("/")) {
                    continue;
                }
                input = jarFile.getInputStream(jarEntry);
                expand(input, docBase, name);
                input.close();
                input = null;
            }
        } catch (IOException e) {
            // If something went wrong, delete expanded dir to keep things
            // clean
            deleteDir(docBase);
            throw e;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
                input = null;
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable ignored) {
                }
                jarFile = null;
            }
        }

        // Return the absolute path to our new document base directory
        return docBase.getAbsolutePath();

    }

    /**
     * Copy the specified file or directory to the destination.
     *
     * @param src File object representing the source
     * @param dest File object representing the destination
     */
    public static boolean copy(File src, File dest) {

        boolean result = true;

        String files[];
        if (src.isDirectory()) {
            files = src.list();
            result = dest.mkdir();
        } else {
            files = new String[1];
            files[0] = "";
        }
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length && result; i++) {
            File fileSrc = new File(src, files[i]);
            File fileDest = new File(dest, files[i]);
            if (fileSrc.isDirectory()) {
                result = copy(fileSrc, fileDest);
            } else {
                FileChannel ic = null;
                FileChannel oc = null;
                try {
                    ic = new FileInputStream(fileSrc).getChannel();
                    oc = new FileOutputStream(fileDest).getChannel();
                    ic.transferTo(0, ic.size(), oc);
                } catch (IOException e) {

                    result = false;
                } finally {
                    if (ic != null) {
                        try {
                            ic.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (oc != null) {
                        try {
                            oc.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
        return result;

    }

    /**
     * Delete the specified directory, including all of its contents and subdirectories recursively.
     *
     * @param dir File object representing the directory to be deleted
     */
    public static boolean delete(File dir) {
        if (dir.isDirectory()) {
            return deleteDir(dir);
        } else {
            return dir.delete();
        }
    }

    /**
     * Delete the specified directory, including all of its contents and subdirectories recursively.
     *
     * @param dir File object representing the directory to be deleted
     */
    public static boolean deleteDir(File dir) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                if (!file.delete()) {
                    throw new IllegalStateException(String.format("Unable to delete file: %s", file.getAbsolutePath()));
                }
            }
        }
        return dir.delete();

    }

    /**
     * Expand the specified input stream into the specified directory, creating a file named from the specified relative
     * path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected static void expand(InputStream input, File docBase, String name) throws IOException {

        File file = new File(docBase, name);
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file));
            byte buffer[] = new byte[2048];
            while (true) {
                int n = input.read(buffer);
                if (n <= 0) {
                    break;
                }
                output.write(buffer, 0, n);
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                    // Ignore
                }
            }
        }

    }

}
