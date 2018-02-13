/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.junit.Assert;
import org.junit.Test;

public class ServletBasedTest {
    /**
     * This tests the up front registration of applications from places such as Servlet.init().  This is likely the
     * common case
     */
    @Test
    public void declarative() throws IOException, InstantiationException, InterruptedException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);
        final EchoWebSocketApplication app = new EchoWebSocketApplication();
        WebSocketEngine.getEngine().register(app);
        HttpServer httpServer = HttpServer.createSimpleServer(".", WebSocketsTest.PORT);
        httpServer.getServerConfiguration().setHttpServerName("WebSocket Server");
        httpServer.getServerConfiguration().setName("WebSocket Server");
        for (NetworkListener networkListener : httpServer.getListeners()) {
            networkListener.registerAddOn(new WebSocketAddOn());
        }
        httpServer.start();
        
        try {
            WebSocketClient socket = new WebSocketClient(String.format("ws://localhost:%s/echo", WebSocketsTest.PORT),
                new WebSocketAdapter() {
                    public void onMessage(WebSocket socket, String frame) {
                        latch.countDown();
                    }
                });
            socket.connect();
            socket.send("echo me back");
            Assert.assertTrue(latch.await(WebSocketEngine.DEFAULT_TIMEOUT, TimeUnit.SECONDS));
        } finally {
            WebSocketEngine.getEngine().unregister(app);
            httpServer.shutdownNow();
        }
    }
}
