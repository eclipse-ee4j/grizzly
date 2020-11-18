/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

/**
 * The default {@link Broadcaster}, which iterates over set of clients and sends the same text of binary message
 * separately to each client. So the text/binary -> websocket-frame transformation is being done for each client
 * separately.
 *
 * @author Alexey Stashok
 */
public class DummyBroadcaster implements Broadcaster {

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(final Iterable<? extends WebSocket> recipients, final String text) {

        for (WebSocket websocket : recipients) {
            if (websocket.isConnected()) {
                try {
                    websocket.send(text);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(final Iterable<? extends WebSocket> recipients, final byte[] binary) {

        for (WebSocket websocket : recipients) {
            if (websocket.isConnected()) {
                try {
                    websocket.send(binary);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastFragment(final Iterable<? extends WebSocket> recipients, final String text, final boolean last) {
        for (WebSocket websocket : recipients) {
            if (websocket.isConnected()) {
                try {
                    websocket.stream(last, text);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastFragment(final Iterable<? extends WebSocket> recipients, final byte[] binary, final boolean last) {

        for (WebSocket websocket : recipients) {
            if (websocket.isConnected()) {
                try {
                    websocket.stream(last, binary, 0, binary.length);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

}
