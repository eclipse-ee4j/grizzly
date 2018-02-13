/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.junit.Assert;
import org.junit.Test;

public class TimeoutTest extends BaseWebSocketTestUtilities {

    private static final int PORT = 9119;

    // ------------------------------------------------------------ Test Methods

    @Test
    public void testIndependentTimeout() throws Exception {
        HttpServer httpServer = HttpServer.createSimpleServer(".", PORT);
        httpServer.getServerConfiguration().setHttpServerName("WebSocket Server");
        httpServer.getServerConfiguration().setName("WebSocket Server");
        for (NetworkListener networkListener : httpServer.getListeners()) {
            networkListener.registerAddOn(new WebSocketAddOn());
            networkListener.getKeepAlive().setIdleTimeoutInSeconds(5);
        }
        WebSocketEngine.getEngine().register("", "/echo", new EchoApplication());

        final EchoWebSocketApplication app = new EchoWebSocketApplication();
        WebSocketClient socket = null;
        try {
            httpServer.start();
            WebSocketEngine.getEngine().register(app);
            socket = new WebSocketClient("wss://localhost:" + PORT + "/echo");
            socket.connect();
            Thread.sleep(10000);
            Assert.assertTrue(socket.isConnected());
        } finally {
            if (socket != null) {
                socket.close();
            }
            httpServer.shutdownNow();
        }
    }

    @Test
    public void testConfiguredIndependentTimeout() throws Exception {
        HttpServer httpServer = HttpServer.createSimpleServer(".", PORT);
        httpServer.getServerConfiguration().setHttpServerName("WebSocket Server");
        httpServer.getServerConfiguration().setName("WebSocket Server");
        WebSocketEngine.getEngine().register("", "/echo", new EchoApplication());
        httpServer.start();
        for (NetworkListener networkListener : httpServer.getListeners()) {
            networkListener.getKeepAlive().setIdleTimeoutInSeconds(5);
            final Transport t = networkListener.getTransport();
            FilterChain c = (FilterChain) t.getProcessor();
            final int httpServerFilterIdx = c.indexOfType(HttpServerFilter.class);

            if (httpServerFilterIdx >= 0) {
                // Insert the WebSocketFilter right after HttpCodecFilter
                c.add(httpServerFilterIdx, new WebSocketFilter(8)); // in seconds
            }
        }

        final EchoWebSocketApplication app = new EchoWebSocketApplication();
        WebSocketClient socket = null;
        try {
            WebSocketEngine.getEngine().register(app);
            socket = new WebSocketClient("wss://localhost:" + PORT + "/echo");
            socket.connect();
            Thread.sleep(10000);
            Assert.assertFalse(socket.isConnected());
        } finally {
            if (socket != null) {
                socket.close();
            }
            httpServer.shutdownNow();
        }
    }

}
