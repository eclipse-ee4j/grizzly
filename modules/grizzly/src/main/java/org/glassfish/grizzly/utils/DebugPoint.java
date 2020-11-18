/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utility class, which may stores the current execution position to help tracking threading issues.
 *
 * @author Alexey Stashok
 */
public class DebugPoint {
    private final Exception stackTrace;
    private final String threadName;

    public DebugPoint(Exception stackTrace, String threadName) {
        this.stackTrace = stackTrace;
        this.threadName = threadName;
    }

    public Exception getStackTrace() {
        return stackTrace;
    }

    public String getThreadName() {
        return threadName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Point [current-thread=").append(Thread.currentThread().getName());
        sb.append(", debug-point-thread=").append(threadName);
        sb.append(", stackTrace=\n");
        StackTraceElement[] trace = stackTrace.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            sb.append("\tat ").append(trace[i]).append('\n');
        }

        return sb.toString();
    }
}
