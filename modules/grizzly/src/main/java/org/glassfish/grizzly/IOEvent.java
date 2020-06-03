/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.nio.channels.SelectionKey;

/**
 * Enumeration represents the I/O events, occurred on a {@link Connection}.
 *
 * @see Connection
 *
 * @author Alexey Stashok
 */
public enum IOEvent {

    /**
     * no event
     */
    NONE(0),

    /**
     * Event occurs on a {@link Connection}, once it gets available for read.
     */
    READ(SelectionKey.OP_READ),

    /**
     * Event occurs on a {@link Connection}, once it gets available for write.
     */
    WRITE(SelectionKey.OP_WRITE),

    /**
     * Event occurs on a server {@link Connection}, when it becomes ready to accept new client {@link Connection}.
     *
     * Note, this event occurs on server code for server {@link Connection}.
     */
    SERVER_ACCEPT(SelectionKey.OP_ACCEPT),

    /**
     * Event occurs on a client {@link Connection}, just after it was accepted by the server.
     *
     * Note, this event occurs on server code for client {@link Connection}.
     */
    ACCEPTED(0),

    /**
     * Event occurs on a {@link Connection}, once it was connected to server.
     *
     * (this is service IOEvent, which is not getting propagated to a {@link Processor}
     */
    CLIENT_CONNECTED(SelectionKey.OP_CONNECT),

    /**
     * Event occurs on a {@link Connection}, once it was connected to server.
     */
    CONNECTED(0),

    /**
     * Event occurs on a {@link Connection}, once it gets closed.
     */
    CLOSED(0);

    private final int selectionKeyInterest;

    IOEvent(int selectionKeyInterest) {
        this.selectionKeyInterest = selectionKeyInterest;
    }

    public int getSelectionKeyInterest() {
        return selectionKeyInterest;
    }
}
