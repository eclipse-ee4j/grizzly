/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * An object, describing the reason why {@link Connection} was closed.
 * 
 * @author Alexey Stashok
 */
public class CloseReason {
    private static final IOException LOCALLY_CLOSED;
    private static final IOException REMOTELY_CLOSED;
    
    public static final CloseReason LOCALLY_CLOSED_REASON;
    public static final CloseReason REMOTELY_CLOSED_REASON;
    
    static {
        LOCALLY_CLOSED = new IOException("Locally closed");
        LOCALLY_CLOSED.setStackTrace(new StackTraceElement[0]);
        
        REMOTELY_CLOSED = new IOException("Remotely closed");
        REMOTELY_CLOSED.setStackTrace(new StackTraceElement[0]);

        LOCALLY_CLOSED_REASON =
                new CloseReason(org.glassfish.grizzly.CloseType.LOCALLY, LOCALLY_CLOSED);
        REMOTELY_CLOSED_REASON =
                new CloseReason(org.glassfish.grizzly.CloseType.REMOTELY, REMOTELY_CLOSED);
    }
    
    private final CloseType type;
    private final IOException cause;

    public CloseReason(final CloseType type, final IOException cause) {
        this.type = type;
        this.cause = cause != null
                ? cause
                : (type == CloseType.LOCALLY
                          ? LOCALLY_CLOSED
                          : REMOTELY_CLOSED);
    }

    /**
     * Return information whether {@link Connection} was closed locally or remotely.
     * 
     * @return information whether {@link Connection} was closed locally or remotely
     */
    public CloseType getType() {
        return type;
    }

    /**
     * Returns information about an error, that caused the {@link Connection} to
     * be closed.
     * 
     * If the cause wasn't specified by user - the default value {@link #DEFAULT_CAUSE} will be returned.
     * 
     * @return information about an error, that caused the {@link Connection} to
     * be closed
     */
    public IOException getCause() {
        return cause;
    }
}
