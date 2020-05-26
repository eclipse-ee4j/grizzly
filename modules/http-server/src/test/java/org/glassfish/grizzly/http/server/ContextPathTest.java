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

package org.glassfish.grizzly.http.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing {@link HttpHandler} context-path
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class ContextPathTest {
    public static final int PORT = 18896;

    private HttpServer httpServer;

    @Before
    public void before() throws Exception {
        ByteBufferWrapper.DEBUG_MODE = true;
        configureHttpServer();
    }

    @After
    public void after() throws Exception {
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }

    @Test
    public void testRuntimeContextPathResolving() throws Exception {
        startHttpServer(new StaticHttpHandler() {

        }, "/context-path");

        final HttpRequestPacket request1 = HttpRequestPacket.builder().method("GET").uri("/pom.xml").protocol("HTTP/1.1").header("Host", "localhost").build();

        final HttpRequestPacket request2 = HttpRequestPacket.builder().method("GET").uri("/context-path/pom.xml").protocol("HTTP/1.1")
                .header("Host", "localhost").build();

        final Future<HttpContent> responseFuture1 = send("localhost", PORT, request1);
        final HttpContent response1 = responseFuture1.get(10, TimeUnit.SECONDS);

        assertEquals(404, ((HttpResponsePacket) response1.getHttpHeader()).getStatus());

        final Future<HttpContent> responseFuture2 = send("localhost", PORT, request2);
        final HttpContent response2 = responseFuture2.get(10, TimeUnit.SECONDS);
        assertEquals(200, ((HttpResponsePacket) response2.getHttpHeader()).getStatus());
    }

    private void configureHttpServer() throws Exception {
        httpServer = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, PORT);

        httpServer.addListener(listener);
    }

    private void startHttpServer(HttpHandler httpHandler, String... mappings) throws Exception {
        httpServer.getServerConfiguration().addHttpHandler(httpHandler, mappings);
        httpServer.start();
    }

    private Future<HttpContent> send(String host, int port, HttpPacket request) throws Exception {
        final FutureImpl<HttpContent> future = SafeFutureImpl.create();

        final FilterChainBuilder builder = FilterChainBuilder.stateless();
        builder.add(new TransportFilter());

        builder.add(new HttpClientFilter());
        builder.add(new HttpMessageFilter(future));

        SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(httpServer.getListener("grizzly").getTransport()).processor(builder.build())
                .build();

        Future<Connection> connectFuture = connectorHandler.connect(host, port);
        final Connection connection = connectFuture.get(10, TimeUnit.SECONDS);

        connection.write(request);

        return future;
    }

    private static class HttpMessageFilter extends BaseFilter {

        private final FutureImpl<HttpContent> future;

        public HttpMessageFilter(FutureImpl<HttpContent> future) {
            this.future = future;
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            final HttpContent content = ctx.getMessage();
            try {
                if (!content.isLast()) {
                    return ctx.getStopAction(content);
                }

                future.result(content);
            } catch (Exception e) {
                future.failure(e);
                e.printStackTrace();
            }

            return ctx.getStopAction();
        }
    }
}
