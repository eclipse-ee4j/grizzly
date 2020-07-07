/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Exceptions utilities.
 *
 * @author Alexey Stashok
 */
public class Exceptions {

    /**
     * Returns the {@link Throwable}'s stack trace information as {@link String}. The result {@link String} format will be
     * the same as reported by {@link Throwable#printStackTrace()}.
     *
     * @param t {@link Throwable}.
     * @return the {@link Throwable}'s stack trace information as {@link String}.
     */
    public static String getStackTraceAsString(final Throwable t) {
        final StringWriter stringWriter = new StringWriter(2048);
        final PrintWriter pw = new PrintWriter(stringWriter);
        t.printStackTrace(pw);

        pw.close();
        return stringWriter.toString();
    }

    /**
     * Wrap the given {@link Throwable} by {@link IOException}.
     *
     * @param t {@link Throwable}.
     * @return {@link IOException}.
     */
    public static IOException makeIOException(final Throwable t) {
        if (IOException.class.isAssignableFrom(t.getClass())) {
            return (IOException) t;
        }

        return new IOException(t);
    }

    /**
     * @return {@link String} representation of all the JVM threads
     *
     * @see Thread#getAllStackTraces()
     */
    public static String getAllStackTracesAsString() {
        final StringBuilder sb = new StringBuilder(256);

        final Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();

        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            sb.append(entry.getKey()).append('\n');

            for (StackTraceElement traceElement : entry.getValue()) {
                sb.append("\tat ").append(traceElement).append('\n');
            }

        }

        return sb.toString();
    }
}
