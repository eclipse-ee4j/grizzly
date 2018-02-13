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

package org.glassfish.grizzly.nio;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author oleksiys
 */
public final class DefaultSelectionKeyHandler implements SelectionKeyHandler {

    private static final Logger LOGGER = Grizzly.logger(DefaultSelectionKeyHandler.class);

// Comment the mapping array and use if instead (appear to be faster)
//
//    private static final int[] ioEvent2SelectionKeyInterest = {
//        0, SelectionKey.OP_ACCEPT, 0, SelectionKey.OP_CONNECT, 0,
//        SelectionKey.OP_READ, SelectionKey.OP_WRITE, 0};

    private final static IOEvent[][] ioEventMap;

    static {
        ioEventMap = new IOEvent[32][];
        for (int i = 0; i < ioEventMap.length; i++) {
            int idx = 0;
            IOEvent[] tmpArray = new IOEvent[4];
            if ((i & SelectionKey.OP_READ) != 0) {
                tmpArray[idx++] = IOEvent.READ;
            }

            if ((i & SelectionKey.OP_WRITE) != 0) {
                tmpArray[idx++] = IOEvent.WRITE;
            }

            if ((i & SelectionKey.OP_CONNECT) != 0) {
                tmpArray[idx++] = IOEvent.CLIENT_CONNECTED;
            }

            if ((i & SelectionKey.OP_ACCEPT) != 0) {
                tmpArray[idx++] = IOEvent.SERVER_ACCEPT;
            }

            ioEventMap[i] = Arrays.copyOf(tmpArray, idx);
        }
    }

    @Override
    public void onKeyRegistered(SelectionKey key) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "KEY IS REGISTERED: {0}", key);
        }
    }

    @Override
    public void onKeyDeregistered(SelectionKey key) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "KEY IS DEREGISTERED: {0}", key);
        }
    }

    @Override
    public void cancel(SelectionKey key) throws IOException {
        onKeyDeregistered(key);
        key.cancel();
    }

    @Override
    public int ioEvent2SelectionKeyInterest(IOEvent ioEvent) {
        switch (ioEvent) {
            case READ: return SelectionKey.OP_READ;
            case WRITE: return SelectionKey.OP_WRITE;
            case SERVER_ACCEPT: return SelectionKey.OP_ACCEPT;
            case CLIENT_CONNECTED: return SelectionKey.OP_CONNECT;
            default: return 0;
        }
    }

    @Override
    public IOEvent[] getIOEvents(int interest) {
        return ioEventMap[interest];
    }

    @Override
    public IOEvent selectionKeyInterest2IoEvent(int selectionKeyInterest) {
        if ((selectionKeyInterest & SelectionKey.OP_READ) != 0) {
            return IOEvent.READ;
        } else if ((selectionKeyInterest & SelectionKey.OP_WRITE) != 0) {
            return IOEvent.WRITE;
        } else if ((selectionKeyInterest & SelectionKey.OP_ACCEPT) != 0) {
            return IOEvent.SERVER_ACCEPT;
        } else if ((selectionKeyInterest & SelectionKey.OP_CONNECT) != 0) {
            return IOEvent.CLIENT_CONNECTED;
        }

        return IOEvent.NONE;
    }

    @Override
    public boolean onProcessInterest(SelectionKey key, int interest)
            throws IOException {
        return true;
    }

    @Override
    public NIOConnection getConnectionForKey(SelectionKey selectionKey) {
        return (NIOConnection) selectionKey.attachment();
    }

    @Override
    public void setConnectionForKey(NIOConnection connection,
            SelectionKey selectionKey) {
        selectionKey.attach(connection);
    }
}
