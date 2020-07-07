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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.GenericCloseListener;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.Futures;

import junit.framework.TestCase;

/**
 * Testing HTTP keep-alive
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class KeepAliveTest extends TestCase {
    private static final int PORT = PORT();

    static int PORT() {
        try {
            int port = 18895 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void testHttp11KeepAlive() throws Exception {
        final String msg = "Hello world #";

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            connectFuture.get(10, TimeUnit.SECONDS);

            Future<Buffer> resultFuture = client
                    .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

            Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

            assertEquals("Hello world #0", buffer.toStringContent());

            resultFuture = client
                    .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

            buffer = resultFuture.get(10, TimeUnit.SECONDS);

            assertEquals("Hello world #1", buffer.toStringContent());

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testHttp11KeepAliveWithConnectionCloseHeader() throws Exception {
        final String msg = "Hello world #";

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            connectFuture.get(10, TimeUnit.SECONDS);

            Future<Buffer> resultFuture = client.get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1)
                    .header("Connection", "close").header("Host", "localhost:" + PORT).build());

            Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

            assertEquals("Hello world #0", buffer.toStringContent());

            try {
                resultFuture = client
                        .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

                buffer = resultFuture.get(10, TimeUnit.SECONDS);

                fail("IOException expected");
            } catch (ExecutionException ee) {
                final Throwable cause = ee.getCause();
                assertTrue("IOException expected, but got" + cause.getClass() + " " + cause.getMessage(), cause instanceof IOException);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testHttp11KeepAliveMaxRequests() throws Exception {
        final String msg = "Hello world #";

        final int maxKeepAliveRequests = 5;

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");
        server.getListener("grizzly").getKeepAlive().setMaxRequestsCount(maxKeepAliveRequests);

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            connectFuture.get(10, TimeUnit.SECONDS);

            for (int i = 0; i < maxKeepAliveRequests; i++) {
                final Future<Buffer> resultFuture = client
                        .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

                final Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

                assertEquals("Hello world #" + i, buffer.toStringContent());
            }

            try {
                final Future<Buffer> resultFuture = client
                        .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

                final Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

                fail("IOException expected");
            } catch (ExecutionException ee) {
                final Throwable cause = ee.getCause();
                assertTrue("IOException expected, but got" + cause.getClass() + " " + cause.getMessage(), cause instanceof IOException);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testHttp11KeepAliveUnlimitedMaxRequests() throws Exception {
        final String msg = "Hello world #";

        final int maxKeepAliveRequests = 150;

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");
        server.getListener("grizzly").getKeepAlive().setMaxRequestsCount(-1);

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            connectFuture.get(10, TimeUnit.SECONDS);

            for (int i = 0; i <= maxKeepAliveRequests; i++) {
                final Future<Buffer> resultFuture = client
                        .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

                final Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

                assertEquals("Hello world #" + i, buffer.toStringContent());
            }

            try {
                final Future<Buffer> resultFuture = client
                        .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

                resultFuture.get(10, TimeUnit.SECONDS);

            } catch (ExecutionException ee) {
                final Throwable cause = ee.getCause();
                cause.printStackTrace();
                fail("Unexpected exception: " + cause);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testIdleTimeoutAfterConnect() throws Exception {
        final int idleTimeoutSeconds = 2;

        HttpServer server = createServer(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
            }
        }, "/path");

        server.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(idleTimeoutSeconds);
        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            final Connection clientConnection = connectFuture.get(10, TimeUnit.SECONDS);

            final CountDownLatch latch = new CountDownLatch(1);
            clientConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(idleTimeoutSeconds * 4, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testIdleTimeoutBetweenRequests() throws Exception {
        final int idleTimeoutSeconds = 2;
        final String msg = "Hello world #";

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");
        server.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(idleTimeoutSeconds);

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            Thread.sleep(100);
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            Connection clientConnection = connectFuture.get(10, TimeUnit.SECONDS);

            final CountDownLatch latch = new CountDownLatch(1);
            clientConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    latch.countDown();
                }
            });

            Future<Buffer> resultFuture = client
                    .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

            Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

            assertEquals("Hello world #0", buffer.toStringContent());

            assertTrue(latch.await(idleTimeoutSeconds * 4, TimeUnit.SECONDS));

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testInfiniteIdleTimeoutAfterConnect() throws Exception {
        final int idleTimeoutSeconds = -1;

        HttpServer server = createServer(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
            }
        }, "/path");

        server.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(idleTimeoutSeconds);
        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            final Connection clientConnection = connectFuture.get(10, TimeUnit.SECONDS);

            final CountDownLatch latch = new CountDownLatch(1);
            clientConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    latch.countDown();
                }
            });

            assertFalse(latch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    public void testInfiniteIdleTimeoutBetweenRequests() throws Exception {
        final int idleTimeoutSeconds = -1;
        final String msg = "Hello world #";

        HttpServer server = createServer(new HttpHandler() {
            private final AtomicInteger ai = new AtomicInteger();

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                response.getWriter().write(msg + ai.getAndIncrement());
            }

        }, "/path");
        server.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(idleTimeoutSeconds);

        final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        final HttpClient client = new HttpClient(clientTransport);

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = client.connect("localhost", PORT);
            Connection clientConnection = connectFuture.get(10, TimeUnit.SECONDS);

            final CountDownLatch latch = new CountDownLatch(1);
            clientConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    latch.countDown();
                }
            });

            Future<Buffer> resultFuture = client
                    .get(HttpRequestPacket.builder().method("GET").uri("/path").protocol(Protocol.HTTP_1_1).header("Host", "localhost:" + PORT).build());

            Buffer buffer = resultFuture.get(10, TimeUnit.SECONDS);

            assertEquals("Hello world #0", buffer.toStringContent());

            assertFalse(latch.await(5, TimeUnit.SECONDS));

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            client.close();
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    // --------------------------------------------------------- Private Methods

    private static class HttpClient {
        private final TCPNIOTransport transport;
        private volatile Connection connection;
        private volatile FutureImpl<Buffer> asyncFuture;

        public HttpClient(TCPNIOTransport transport) {
            this.transport = transport;
        }

        public Future<Connection> connect(String host, int port) throws IOException {
            FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
            filterChainBuilder.add(new TransportFilter());
            filterChainBuilder.add(new HttpClientFilter());
            filterChainBuilder.add(new HttpResponseFilter());

            final SocketConnectorHandler connector = TCPNIOConnectorHandler.builder(transport).processor(filterChainBuilder.build()).build();

            final FutureImpl<Connection> future = Futures.createSafeFuture();
            connector.connect(new InetSocketAddress(host, port), Futures.toCompletionHandler(future, new EmptyCompletionHandler<Connection>() {

                @Override
                public void completed(Connection result) {
                    connection = result;
                }
            }));

            return future;
        }

        public Future<Buffer> get(HttpRequestPacket request) throws IOException {
            final FutureImpl<Buffer> localFuture = SafeFutureImpl.create();
            asyncFuture = localFuture;
            connection.write(request, new EmptyCompletionHandler() {

                @Override
                public void failed(Throwable throwable) {
                    localFuture.failure(throwable);
                }
            });

            connection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(final Closeable connection, final CloseType closeType) throws IOException {
                    localFuture.failure(new IOException());
                }
            });
            return localFuture;
        }

        public void close() throws IOException {
            if (connection != null) {
                connection.closeSilently();
            }
        }

        private class HttpResponseFilter extends BaseFilter {
            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                if (message.isLast()) {
                    final FutureImpl<Buffer> localFuture = asyncFuture;
                    asyncFuture = null;
                    localFuture.result(message.getContent());

                    return ctx.getStopAction();
                }

                return ctx.getStopAction(message);
            }
        }
    }

    private HttpServer createServer(final HttpHandler httpHandler, final String... mappings) {

        HttpServer server = new HttpServer();
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, PORT);
        server.addListener(listener);
        if (httpHandler != null) {
            server.getServerConfiguration().addHttpHandler(httpHandler, mappings);
        }
        return server;

    }
}
