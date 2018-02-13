/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectWithPriorKnowledgeTest extends AbstractHttp2Test {

    private static String MESSAGE = "ECHO ECHO ECHO";
    private static final int PORT = 18892;
    private HttpServer httpServer;


    // ----------------------------------------------------------- Test Methods


    @Test
    public void testConnectWithPriorKnowledge() throws Exception {
        configureHttpServer();
        startHttpServer();
        final CountDownLatch latch = new CountDownLatch(1);
        final Connection c = getConnection("localhost", PORT, latch);
        HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
        HttpRequestPacket request = builder.method(Method.GET)
                .uri("/echo")
                .protocol(Protocol.HTTP_2_0)
                .host("localhost:" + PORT).build();
        c.write(HttpContent.builder(request).content(Buffers.EMPTY_BUFFER).last(true).build());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }


    // -------------------------------------------------------- Private Methods


    private void configureHttpServer() throws Exception {
        httpServer = createServer(null, PORT, false, true);
        httpServer.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
    }

    private void startHttpServer() throws Exception {
        httpServer.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(MESSAGE);
            }
        }, "/echo");
        httpServer.start();
    }

    private Connection getConnection(final String host, final int port, final CountDownLatch latch)
            throws Exception {

        final FilterChain clientChain =
                createClientFilterChainAsBuilder(false, true, new BaseFilter() {
                    @Override
                    public NextAction handleRead(FilterChainContext ctx) throws IOException {
                        final HttpContent httpContent = ctx.getMessage();
                        if (httpContent.isLast()) {
                            assertEquals(MESSAGE, httpContent.getContent().toStringContent());
                            latch.countDown();
                        }
                        return ctx.getStopAction();
                    }
                }).build();

        SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(
                httpServer.getListener("grizzly").getTransport())
                .processor(clientChain)
                .build();

        Future<Connection> connectFuture = connectorHandler.connect(host, port);
        return connectFuture.get(10, TimeUnit.SECONDS);

    }
}
