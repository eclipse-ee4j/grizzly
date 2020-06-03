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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;

/**
 * WebSocketHolder object, which gets associated with the Grizzly {@link org.glassfish.grizzly.Connection}.
 */
public final class WebSocketHolder {
    public volatile WebSocket webSocket;
    public volatile HandShake handshake;
    public volatile WebSocketApplication application;
    public volatile Buffer buffer;
    public volatile ProtocolHandler handler;

    private static final Attribute<WebSocketHolder> webSocketAttribute = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("web-socket");

    private WebSocketHolder(final ProtocolHandler handler, final WebSocket socket) {
        this.handler = handler;
        webSocket = socket;
    }

    public static boolean isWebSocketInProgress(final Connection connection) {
        return get(connection) != null;
    }

    public static WebSocket getWebSocket(Connection connection) {
        final WebSocketHolder holder = get(connection);
        return holder == null ? null : holder.webSocket;
    }

    public static WebSocketHolder get(final Connection connection) {
        return webSocketAttribute.get(connection);
    }

    public static WebSocketHolder set(final Connection connection, final ProtocolHandler handler, final WebSocket socket) {
        final WebSocketHolder holder = new WebSocketHolder(handler, socket);
        webSocketAttribute.set(connection, holder);
        return holder;
    }
}
