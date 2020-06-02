/*
 * Copyright (c) 2016, 2020 Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class Http2SemanticsTest extends AbstractHttp2Test {

    private final boolean isSecure;
    private final boolean priorKnowledge;
    private HttpServer httpServer;
    private static final int PORT = 18893;

    // ----------------------------------------------------------- Constructors

    public Http2SemanticsTest(final boolean isSecure, final boolean priorKnowledge) {
        this.isSecure = isSecure;
        this.priorKnowledge = priorKnowledge;
    }

    // -------------------------------------------------- Junit Support Methods

    @Parameterized.Parameters
    public static Collection<Object[]> configure() {
        return AbstractHttp2Test.configure();
    }

    @Before
    public void before() throws Exception {
        configureHttpServer();
    }

    @After
    public void after() throws Exception {
        httpServer.shutdownNow();
    }

    // ----------------------------------------------------------- Test Methods

    @Test
    public void invalidHeaderCharactersTest() throws Exception {
        startHttpServer(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write("FAILED");
            }
        }, "/path");

        byte[] headerName = "test".getBytes();
        byte[] temp = new byte[headerName.length + 1];
        System.arraycopy(headerName, 0, temp, 0, headerName.length);
        temp[temp.length - 1] = 0x7; // visual bell

        final Connection c = getConnection("localhost", PORT, null);
        HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
        HttpRequestPacket request = builder.method(Method.GET).uri("/path").protocol(Protocol.HTTP_1_1).host("localhost:" + PORT).build();
        request.setHeader(new String(temp, Charsets.ASCII_CHARSET), "value");
        c.write(HttpContent.builder(request).content(Buffers.EMPTY_BUFFER).last(true).build());
        Thread.sleep(1000);
        final Http2Stream stream = Http2Stream.getStreamFor(request);
        assertThat(stream, notNullValue());
        assertThat(stream.isOpen(), is(false));
    }

    @Test
    public void testHeaderHandling() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        startHttpServer(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                try {
                    assertThat(request.getHeaders(Header.Cookie), hasItems("a=b", "c=d", "e=f"));
                    assertThat(request.getHeaders("test"), hasItems("a", "b"));
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }
        }, "/path");

        final Connection c = getConnection("localhost", PORT, null);
        HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
        HttpRequestPacket request = builder.method(Method.GET).uri("/path").protocol(Protocol.HTTP_1_1).header(Header.Cookie, "a=b")
                .header(Header.Cookie, "c=d").header(Header.Cookie, "e=f").header("test", "a").header("test", "b").host("localhost:" + PORT).build();
        c.write(HttpContent.builder(request).content(Buffers.EMPTY_BUFFER).last(true).build());
        latch.await(5, TimeUnit.SECONDS);
        final Throwable t = error.get();
        if (t != null) {
            throw t;
        }
    }

    // -------------------------------------------------------- Private Methods

    private void configureHttpServer() throws Exception {
        httpServer = createServer(null, PORT, isSecure, true);
        httpServer.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
    }

    private void startHttpServer(final HttpHandler handler, final String path) throws Exception {
        httpServer.getServerConfiguration().addHttpHandler(handler, path);
        httpServer.start();
    }

    private Connection getConnection(final String host, int port, final Filter filter) throws Exception {

        final FilterChain clientChain = createClientFilterChainAsBuilder(isSecure, priorKnowledge).build();

        if (filter != null) {
            clientChain.add(filter);
        }

        final int idx = clientChain.indexOfType(Http2ClientFilter.class);
        assert idx != -1;
        final Http2ClientFilter clientFilter = (Http2ClientFilter) clientChain.get(idx);
        clientFilter.getConfiguration().setPriorKnowledge(true);

        SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(httpServer.getListener("grizzly").getTransport()).processor(clientChain)
                .build();

        Future<Connection> connectFuture = connectorHandler.connect(host, port);
        return connectFuture.get(10, TimeUnit.SECONDS);
    }

}
