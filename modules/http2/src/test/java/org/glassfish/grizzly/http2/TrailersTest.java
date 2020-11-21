/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpTrailer;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.junit.After;
import org.junit.Test;

public class TrailersTest extends AbstractHttp2Test {

    private static final int PORT = 18903;

    private HttpServer httpServer;

    // ----------------------------------------------------------- Test Methods

    @After
    public void tearDown() {
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }

    @Test
    public void testTrailers() throws Exception {
        configureHttpServer();
        startHttpServer(new HttpHandler() {
            @Override
            public void service(final Request request, final Response response) throws Exception {
                response.setContentType("text/plain");
                final InputStream in = request.getInputStream();
                final StringBuilder sb = new StringBuilder();
                int b;
                while ((b = in.read()) != -1) {
                    sb.append((char) b);
                }

                response.setTrailers(new Supplier<Map<String, String>>() {
                    @Override
                    public Map<String, String> get() {
                        if (!request.areTrailersAvailable()) {
                            throw new RuntimeException("Trailers aren't available");
                        }
                        return request.getTrailers();
                    }
                });
                response.getWriter().write(sb.toString());
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicInteger contentCount = new AtomicInteger();
        final AtomicBoolean lastProcessed = new AtomicBoolean();

        final Filter filter = new BaseFilter() {
            @Override
            public NextAction handleRead(final FilterChainContext ctx) throws IOException {
                final HttpContent httpContent = ctx.getMessage();
                try {
                    if (lastProcessed.get()) {
                        fail("Already processed last packet");
                    }
                    if (httpContent.isLast()) {
                        lastProcessed.compareAndSet(false, true);
                        assertTrue(httpContent instanceof HttpTrailer);
                        final MimeHeaders trailers = ((HttpTrailer) httpContent).getHeaders();

                        assertEquals(2, trailers.size());
                        assertEquals("value-a", trailers.getHeader("trailer-a"));
                        assertEquals("value-b", trailers.getHeader("trailer-b"));
                        latch.countDown();
                    } else {
                        assertFalse(httpContent instanceof HttpTrailer);
                        final int result = contentCount.incrementAndGet();
                        if (result == 1) {
                            assertTrue(httpContent.getContent().remaining() == 0); // response
                        } else if (result == 2) {
                            assertEquals("a=b&c=d", httpContent.getContent().toStringContent()); // response body
                        } else {
                            fail("Unexpected content");
                        }
                    }
                } catch (final Throwable t) {
                    error.set(t);
                    latch.countDown();
                }

                return ctx.getStopAction();
            }
        };
        final Connection<?> c = getConnection("localhost", PORT, filter);
        final HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
        final HttpRequestPacket request = builder.method(Method.POST).uri("/echo")
            .protocol(Protocol.HTTP_2_0).host("localhost:" + PORT).build();
        c.write(HttpTrailer.builder(request).content(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, "a=b&c=d"))
            .last(true).header("trailer-a", "value-a").header("trailer-b", "value-b").build());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        final Throwable t = error.get();
        if (t != null) {
            t.printStackTrace();
            fail();
        }

    }

    @Test
    public void testNoContentTrailers() throws Exception {
        configureHttpServer();
        startHttpServer(new HttpHandler() {
            @Override
            public void service(final Request request, final Response response) throws Exception {
                response.setContentType("text/plain");
                final InputStream in = request.getInputStream();
                // noinspection StatementWithEmptyBody
                while (in.read() != -1) {
                }

                response.setTrailers(new Supplier<Map<String, String>>() {
                    @Override
                    public Map<String, String> get() {
                        return request.getTrailers();
                    }
                });
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        final Filter filter = new BaseFilter() {
            @Override
            public NextAction handleRead(final FilterChainContext ctx) throws IOException {
                final HttpContent httpContent = ctx.getMessage();
                try {
                    if (httpContent.isLast()) {
                        assertTrue(httpContent instanceof HttpTrailer);
                        final MimeHeaders trailers = ((HttpTrailer) httpContent).getHeaders();
                        assertEquals(2, trailers.size());
                        assertEquals("value-a", trailers.getHeader("trailer-a"));
                        assertEquals("value-b", trailers.getHeader("trailer-b"));
                    }
                } catch (final Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }

                return ctx.getStopAction();
            }
        };
        final Connection<?> c = getConnection("localhost", PORT, filter);
        final HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
        final HttpRequestPacket request = builder.method(Method.POST).uri("/echo")
            .protocol(Protocol.HTTP_2_0).host("localhost:" + PORT).build();
        // write the request
        c.write(HttpContent.builder(request).last(false).build());
        // write the trailer
        c.write(HttpTrailer.builder(request).content(Buffers.EMPTY_BUFFER).last(true)
            .header("trailer-a", "value-a").header("trailer-b", "value-b").build());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        final Throwable t = error.get();
        if (t != null) {
            t.printStackTrace();
            fail();
        }
    }

    // -------------------------------------------------------- Private Methods

    private void configureHttpServer() throws Exception {
        httpServer = createServer(null, PORT, false, true);
        httpServer.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
    }

    private void startHttpServer(final HttpHandler handler) throws Exception {
        httpServer.getServerConfiguration().addHttpHandler(handler, "/echo");
        httpServer.start();
    }

    private Connection getConnection(final String host, final int port, final Filter clientFilter) throws Exception {

        final FilterChain clientChain = createClientFilterChainAsBuilder(false, true, clientFilter).build();

        final TCPNIOTransport transport = httpServer.getListener("grizzly").getTransport();
        final SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(transport).processor(clientChain).build();
        final Future<Connection> connectFuture = connectorHandler.connect(host, port);
        return connectFuture.get(10, TimeUnit.SECONDS);

    }

}
