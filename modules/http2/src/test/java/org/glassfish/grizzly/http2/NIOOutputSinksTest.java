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

package org.glassfish.grizzly.http2;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.grizzly.http.Protocol.HTTP_1_1;
import static org.glassfish.grizzly.memory.Buffers.EMPTY_BUFFER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.Writer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.io.NIOOutputStream;
import org.glassfish.grizzly.http.io.NIOWriter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NIOOutputSinksTest extends AbstractHttp2Test {
    private static Logger LOGGER = Grizzly.logger(NIOOutputSinksTest.class);
    private static int PORT = PORT();
    
    static int PORT() {
        try {
            int port = 9339 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isSecure;
    private boolean priorKnowledge;

    public NIOOutputSinksTest(boolean isSecure, boolean priorKnowledge) {
        this.isSecure = isSecure;
        this.priorKnowledge = priorKnowledge;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> configure() {
        return AbstractHttp2Test.configure();
    }

    @Test
    public void testBinaryOutputSink() throws Exception {
        int singleMessageSize = 256000;
        int maxWindowSize = singleMessageSize * 2;

        FutureImpl<Integer> parseResult = SafeFutureImpl.create();

        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure, priorKnowledge);
        filterChainBuilder.add(new BaseFilter() {

            private int bytesRead;

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session http2Session = Http2Session.get(ctx.getConnection());
                if (http2Session != null) { // we're going over TLS
                    http2Session.getHttp2State().addReadyListener(session -> {
                        sendRequest(ctx);
                        ctx.resume(ctx.getStopAction());
                    });
                    
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = 
                    HttpRequestPacket.builder()
                                     .method("GET")
                                     .uri("/path")
                                     .protocol(HTTP_1_1)
                                     .header("Host", "localhost:" + PORT)
                                     .build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                int remaining = b.remaining();

                if (b.hasRemaining()) {
                    try {
                        check(b.toStringContent(), bytesRead % singleMessageSize, remaining, singleMessageSize);
                    } catch (Exception e) {
                        parseResult.failure(e);
                    }

                    bytesRead += remaining;
                }

                if (message.isLast()) {
                    parseResult.result(bytesRead);
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();

        FilterChain clientChain = filterChainBuilder.build();
        setInitialHttp2WindowSize(clientChain, maxWindowSize);

        clientTransport.setProcessor(clientChain);
        AtomicInteger writeCounter = new AtomicInteger();
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {

                clientTransport.pause();
                response.setContentType("text/plain");
                NIOOutputStream out = response.getNIOOutputStream();

                while (out.canWrite()) {
                    byte[] b = new byte[singleMessageSize];
                    fill(b);
                    writeCounter.addAndGet(b.length);
                    out.write(b);
                    Thread.yield();
                }

                response.suspend();

                out.notifyCanWrite(new WriteHandler() {
                    @Override
                    public void onWritePossible() {
                        callbackInvoked.compareAndSet(false, true);
                        clientTransport.pause();

                        assertTrue(out.canWrite());

                        clientTransport.resume();
                        try {
                            byte[] b = new byte[singleMessageSize];
                            fill(b);
                            writeCounter.addAndGet(b.length);
                            out.write(b);
                            out.flush();
                            out.close();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        response.resume();

                    }

                    @Override
                    public void onError(Throwable t) {
                        response.resume();
                        throw new RuntimeException(t);
                    }
                });

                clientTransport.resume();
            }

        };

        HttpServer server = createWebServer(httpHandler);
        http2Addon.getConfiguration().setInitialWindowSize(maxWindowSize);

        try {
            Thread.sleep(5);
            server.start();

            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                int length = parseResult.get(30, SECONDS);
                assertEquals(writeCounter.get(), length);
                assertTrue(callbackInvoked.get());
            } finally {
                LOGGER.log(Level.INFO, "Written {0}", writeCounter);
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testBlockingBinaryOutputSink() throws Exception {
        int bufferSize = 4096;
        int maxWindowSize = bufferSize * 3 / 4;
        int bytesToSend = bufferSize * 1024 * 4;

        FutureImpl<Integer> parseResult = SafeFutureImpl.create();
        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {

            private int bytesRead;

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(new Http2State.ReadyListener() {
                        @Override
                        public void ready(Http2Session http2Session) {
                            sendRequest(ctx);
                            ctx.resume(ctx.getStopAction());
                        }
                    });
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/path").protocol(HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                int remaining = b.remaining();

                if (b.hasRemaining()) {
                    try {
                        check(b.toStringContent(), bytesRead % bufferSize, remaining, bufferSize);
                    } catch (Exception e) {
                        parseResult.failure(e);
                    }

                    bytesRead += remaining;
                }

                if (message.isLast()) {
                    parseResult.result(bytesRead);
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        FilterChain clientChain = filterChainBuilder.build();
        setInitialHttp2WindowSize(clientChain, maxWindowSize);

        clientTransport.setProcessor(clientChain);

        AtomicInteger writeCounter = new AtomicInteger();

        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                NIOOutputStream out = response.getNIOOutputStream();

                int sent = 0;

                byte[] b = new byte[bufferSize];
                fill(b);
                try {
                    while (sent < bytesToSend) {
                        out.write(b);
                        sent += bufferSize;
                        writeCounter.addAndGet(bufferSize);
                    }
                } catch (Throwable e) {
                    LOGGER.log(SEVERE, "Unexpected error", e);
                    parseResult.failure(new IllegalStateException("Error", e));
                }
            }
        };

        HttpServer server = createWebServer(httpHandler);
        http2Addon.getConfiguration().setInitialWindowSize(maxWindowSize);

        try {
            Thread.sleep(5);
            server.start();

            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                int length = parseResult.get(60, SECONDS);
                assertEquals("Received " + length + " bytes", bytesToSend, length);
            } finally {
                LOGGER.log(Level.INFO, "Written {0}", writeCounter);
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testCharacterOutputSink() throws Exception {
        int singleMessageSize = 256000;
        int maxWindowSize = singleMessageSize * 2;

        FutureImpl<Integer> parseResult = SafeFutureImpl.create();

        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {

            private int bytesRead;

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(session -> {
                        sendRequest(ctx);
                        ctx.resume(ctx.getStopAction());
                    });
                    
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = 
                    HttpRequestPacket.builder()
                                     .method("GET")
                                     .uri("/path")
                                     .protocol(HTTP_1_1)
                                     .header("Host", "localhost:" + PORT)
                                     .build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                int remaining = b.remaining();

                if (b.hasRemaining()) {
                    try {
                        check(b.toStringContent(), bytesRead % singleMessageSize, remaining, singleMessageSize);
                    } catch (Exception e) {
                        parseResult.failure(e);
                    }

                    bytesRead += remaining;
                }

                if (message.isLast()) {
                    parseResult.result(bytesRead);
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        FilterChain clientChain = filterChainBuilder.build();
        setInitialHttp2WindowSize(clientChain, maxWindowSize);

        clientTransport.setProcessor(clientChain);

        AtomicInteger writeCounter = new AtomicInteger();
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {
                clientTransport.pause();

                response.setContentType("text/plain");
                NIOWriter out = response.getNIOWriter();

                while (out.canWrite()) {
                    char[] data = new char[singleMessageSize];
                    fill(data);
                    writeCounter.addAndGet(data.length);
                    out.write(data);
                    Thread.yield();
                }

                response.suspend();
                notifyCanWrite(out, response);

                clientTransport.resume();
            }

            private void notifyCanWrite(NIOWriter out, Response response) {

                out.notifyCanWrite(new WriteHandler() {

                    @Override
                    public void onWritePossible() {
                        callbackInvoked.compareAndSet(false, true);
                        clientTransport.pause();
                        assertTrue(out.canWrite());
                        clientTransport.resume();
                        try {
                            char[] c = new char[singleMessageSize];
                            fill(c);
                            writeCounter.addAndGet(c.length);
                            out.write(c);
                            out.flush();
                            out.close();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        response.resume();
                    }

                    @Override
                    public void onError(Throwable t) {
                        response.resume();
                        throw new RuntimeException(t);
                    }
                });
            }

        };

        HttpServer server = createWebServer(httpHandler);
        http2Addon.getConfiguration().setInitialWindowSize(maxWindowSize);

        try {
            Thread.sleep(5);
            server.start();

            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                int length = parseResult.get(30, SECONDS);
                assertEquals(writeCounter.get(), length);
                assertTrue(callbackInvoked.get());
            } finally {
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }

    }

    @Test
    public void testBlockingCharacterOutputSink() throws Exception {

        int bufferSize = 4096;
        int maxWindowSize = bufferSize * 3 / 4;
        int bytesToSend = bufferSize * 1024 * 4;

        FutureImpl<Integer> parseResult = SafeFutureImpl.create();
        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {

            private int bytesRead;

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(session -> {
                        sendRequest(ctx);
                        ctx.resume(ctx.getStopAction());
                    });
                    
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = 
                    HttpRequestPacket.builder()
                                     .method("GET")
                                     .uri("/path")
                                     .protocol(HTTP_1_1)
                                     .header("Host", "localhost:" + PORT)
                                     .build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                int remaining = b.remaining();

                if (b.hasRemaining()) {
                    try {
                        check(b.toStringContent(), bytesRead % bufferSize, remaining, bufferSize);
                    } catch (Exception e) {
                        parseResult.failure(e);
                    }

                    bytesRead += remaining;
                }

                if (message.isLast()) {
                    parseResult.result(bytesRead);
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        FilterChain clientChain = filterChainBuilder.build();
        setInitialHttp2WindowSize(clientChain, maxWindowSize);

        clientTransport.setProcessor(clientChain);

        AtomicInteger writeCounter = new AtomicInteger();
        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {
                response.setContentType("text/plain");
                NIOWriter out = response.getNIOWriter();

                int sent = 0;

                char[] b = new char[bufferSize];
                fill(b);
                while (sent < bytesToSend) {
                    out.write(b);
                    sent += bufferSize;
                    writeCounter.addAndGet(bufferSize);
                }
            }
        };

        HttpServer server = createWebServer(httpHandler);
        http2Addon.getConfiguration().setInitialWindowSize(maxWindowSize);

        try {
            Thread.sleep(5);
            server.start();

            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                int length = parseResult.get(60, SECONDS);
                assertEquals("Received " + length + " bytes", bytesToSend, length);
            } finally {
                LOGGER.log(Level.INFO, "Written {0}", writeCounter);
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testWriteExceptionPropagation() throws Exception {
        int size = 1024;

        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(new Http2State.ReadyListener() {
                        @Override
                        public void ready(Http2Session http2Session) {
                            sendRequest(ctx);
                            ctx.resume(ctx.getStopAction());
                        }
                    });
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/path").protocol(HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setProcessor(filterChainBuilder.build());

        FutureImpl<Boolean> parseResult = SafeFutureImpl.create();

        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {

                // clientTransport.pause();
                response.setContentType("text/plain");
                NIOWriter out = response.getNIOWriter();

                char[] c = new char[size];
                Arrays.fill(c, 'a');

                for (;;) {
                    try {
                        out.write(c);
                        out.flush();
                        Thread.yield();
                    } catch (IOException e) {
                        if (e instanceof CustomIOException || e.getCause() instanceof CustomIOException) {
                            parseResult.result(Boolean.TRUE);
                        } else {
                            System.out.println("NOT CUSTOM");
                            parseResult.failure(e);
                        }
                        break;
                    } catch (Exception e) {
                        System.out.println("NOT CUSTOM");
                        parseResult.failure(e);
                        break;
                    }
                }

            }

        };

        HttpServer server = createWebServer(httpHandler);

        NetworkListener listener = server.getListener("grizzly");
        listener.registerAddOn(new AddOn() {

            @Override
            public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
                int idx = builder.indexOfType(TransportFilter.class);
                builder.add(idx + 1, new BaseFilter() {
                    AtomicInteger counter = new AtomicInteger();

                    @Override
                    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
                        Buffer buffer = ctx.getMessage();
                        if (counter.addAndGet(buffer.remaining()) > size * 8) {
                            throw new CustomIOException();
                        }

                        return ctx.getInvokeAction();
                    }
                });
            }

        });

        try {
            Thread.sleep(5);
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                boolean exceptionThrown = parseResult.get(10, SECONDS);
                assertTrue("Unexpected Exception thrown.", exceptionThrown);
            } finally {
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testOutputBufferDirectWrite() throws Exception {
        int bufferSize = 65536;
        int maxWindowSize = bufferSize * 10;

        FutureImpl<String> parseResult = SafeFutureImpl.create();

        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {
            private StringBuilder sb = new StringBuilder();

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(new Http2State.ReadyListener() {
                        @Override
                        public void ready(Http2Session http2Session) {
                            sendRequest(ctx);
                            ctx.resume(ctx.getStopAction());
                        }
                    });
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/path").protocol(HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {

                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                if (b.hasRemaining()) {
                    sb.append(b.toStringContent());
                }

                if (message.isLast()) {
                    parseResult.result(sb.toString());
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        FilterChain clientChain = filterChainBuilder.build();
        setInitialHttp2WindowSize(clientChain, maxWindowSize);

        clientTransport.setProcessor(clientChain);

        AtomicInteger writeCounter = new AtomicInteger();
        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {

                clientTransport.pause();
                response.setContentType("text/plain");
                NIOOutputStream out = response.getNIOOutputStream();

                // in order to enable direct writes - set the buffer size less than byte[] length
                response.setBufferSize(bufferSize / 8);

                byte[] b = new byte[bufferSize];

                int i = 0;
                while (out.canWrite()) {
                    Arrays.fill(b, (byte) ('a' + i++ % ('z' - 'a')));
                    writeCounter.addAndGet(b.length);
                    out.write(b);
                    Thread.yield();
                }

                clientTransport.resume();
            }
        };

        HttpServer server = createWebServer(httpHandler);
        http2Addon.getConfiguration().setInitialWindowSize(maxWindowSize);

        try {
            Thread.sleep(50);
            server.start();

            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                String resultStr = parseResult.get(10, SECONDS);
                assertEquals(writeCounter.get(), resultStr.length());
                check1(resultStr, bufferSize);

            } finally {
                LOGGER.log(Level.INFO, "Written {0}", writeCounter);
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testWritePossibleReentrants() throws Exception {

        FutureImpl<HttpHeader> parseResult = SafeFutureImpl.create();
        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(new Http2State.ReadyListener() {
                        @Override
                        public void ready(Http2Session http2Session) {
                            sendRequest(ctx);
                            ctx.resume(ctx.getStopAction());
                        }
                    });
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/path").protocol(HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpPacket message = ctx.getMessage();
                HttpHeader header = message.isHeader() ? (HttpHeader) message : message.getHttpHeader();

                parseResult.result(header);

                return ctx.getStopAction();
            }
        });

        int maxAllowedReentrants = Writer.Reentrant.getMaxReentrants();
        AtomicInteger maxReentrantsNoticed = new AtomicInteger();

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setProcessor(filterChainBuilder.build());
        HttpHandler httpHandler = new HttpHandler() {

            int reentrants = maxAllowedReentrants * 3;
            ThreadLocal<Integer> reentrantsCounter = new ThreadLocal<Integer>() {

                @Override
                protected Integer initialValue() {
                    return -1;
                }
            };

            @Override
            public void service(Request request, Response response) throws Exception {
                response.suspend();

                // clientTransport.pause();
                NIOOutputStream outputStream = response.getNIOOutputStream();
                reentrantsCounter.set(0);

                try {
                    outputStream.notifyCanWrite(new WriteHandler() {

                        @Override
                        public void onWritePossible() throws Exception {
                            if (reentrants-- >= 0) {
                                int reentrantNum = reentrantsCounter.get() + 1;

                                try {
                                    reentrantsCounter.set(reentrantNum);

                                    if (reentrantNum > maxReentrantsNoticed.get()) {
                                        maxReentrantsNoticed.set(reentrantNum);
                                    }

                                    outputStream.notifyCanWrite(this);
                                } finally {
                                    reentrantsCounter.set(reentrantNum - 1);
                                }
                            } else {
                                finish(200);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            finish(500);
                        }

                        private void finish(int code) {
                            response.setStatus(code);
                            response.resume();
                        }
                    });
                } finally {
                    reentrantsCounter.remove();
                }
            }
        };

        HttpServer server = createWebServer(httpHandler);

        try {
            Thread.sleep(5);
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                HttpHeader header = parseResult.get(10, SECONDS);
                assertEquals(200, ((HttpResponsePacket) header).getStatus());

                assertTrue("maxReentrantNoticed=" + maxReentrantsNoticed + " maxAllowed=" + maxAllowedReentrants,
                        maxReentrantsNoticed.get() <= maxAllowedReentrants);
            } finally {
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void testWritePossibleNotification() throws Exception {
        int notificationsNum = 5;
        int size = 8192;

        FutureImpl<Integer> parseResult = SafeFutureImpl.create();
        FilterChainBuilder filterChainBuilder = createClientFilterChainAsBuilder(isSecure);
        filterChainBuilder.add(new BaseFilter() {
            private int bytesRead;

            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {

                Http2Session c = Http2Session.get(ctx.getConnection());
                if (c != null) { // we're going over TLS
                    c.getHttp2State().addReadyListener(new Http2State.ReadyListener() {
                        @Override
                        public void ready(Http2Session http2Session) {
                            sendRequest(ctx);
                            ctx.resume(ctx.getStopAction());
                        }
                    });
                    return ctx.getSuspendAction();
                } else {
                    sendRequest(ctx);
                    return ctx.getStopAction();
                }
            }

            private void sendRequest(FilterChainContext ctx) {
                // Build the HttpRequestPacket, which will be sent to a server
                // We construct HTTP request version 1.1 and specifying the URL of the
                // resource we want to download
                HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/path").protocol(HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();

                // Write the request asynchronously
                ctx.write(HttpContent.builder(httpRequest).content(EMPTY_BUFFER).last(true).build());
            }

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                Buffer b = message.getContent();
                int remaining = b.remaining();

                if (b.hasRemaining()) {
                    try {
                        check(b.toStringContent(), bytesRead % size, remaining, size);
                    } catch (Exception e) {
                        parseResult.failure(e);
                    }

                    bytesRead += remaining;
                }

                if (message.isLast()) {
                    parseResult.result(bytesRead);
                }
                return ctx.getStopAction();
            }
        });

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setProcessor(filterChainBuilder.build());

        AtomicInteger sentBytesCount = new AtomicInteger();
        AtomicInteger notificationsCount = new AtomicInteger();

        HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(Request request, Response response) throws Exception {
                response.suspend();

                NIOOutputStream outputStream = response.getNIOOutputStream();
                outputStream.notifyCanWrite(new WriteHandler() {

                    @Override
                    public void onWritePossible() throws Exception {
                        clientTransport.pause();

                        try {
                            while (outputStream.canWrite()) {
                                byte[] b = new byte[size];
                                fill(b);
                                outputStream.write(b);
                                sentBytesCount.addAndGet(size);
                                Thread.yield();
                            }

                            if (notificationsCount.incrementAndGet() < notificationsNum) {
                                outputStream.notifyCanWrite(this);
                            } else {
                                finish(200);
                            }
                        } finally {
                            clientTransport.resume();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        finish(500);
                    }

                    private void finish(int code) {
                        response.setStatus(code);
                        response.resume();
                    }
                });
            }
        };

        HttpServer server = createWebServer(httpHandler);

        try {
            Thread.sleep(5);
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, SECONDS);
                int responseContentLength = parseResult.get(10, SECONDS);

                assertEquals(notificationsNum, notificationsCount.get());
                assertEquals(sentBytesCount.get(), responseContentLength);
            } finally {
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }

    private static void fill(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) ('a' + i % ('z' - 'a'));
        }
    }

    private static void fill(char[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) ('a' + i % ('z' - 'a'));
        }
    }

//    private static void check(String s, int lastCameSize, int bufferSize) {
//        check(s, 0, lastCameSize, bufferSize);
//    }

    private static void check(String s, int offset, int lastCameSize, int bufferSize) {
        int start = s.length() - lastCameSize;

        for (int i = 0; i < lastCameSize; i++) {
            char c = s.charAt(start + i);
            char expect = (char) ('a' + (i + start + offset) % bufferSize % ('z' - 'a'));
            if (c != expect) {
                throw new IllegalStateException("Result at [" + (i + start) + "] don't match. Expected=" + expect + " got=" + c);
            }
        }
    }

    private void check1(String resultStr, int LENGTH) {
        for (int i = 0; i < resultStr.length() / LENGTH; i++) {
            char expect = (char) ('a' + i % ('z' - 'a'));
            for (int j = 0; j < LENGTH; j++) {
                char charAt = resultStr.charAt(i * LENGTH + j);
                if (charAt != expect) {
                    throw new IllegalStateException("Result at [" + (i * LENGTH + j) + "] don't match. Expected=" + expect + " got=" + charAt);
                }
            }
        }
    }

    private HttpServer createWebServer(HttpHandler httpHandler) {
        HttpServer httpServer = createServer(null, PORT, isSecure, HttpHandlerRegistration.of(httpHandler, "/path/*"));

        NetworkListener listener = httpServer.getListener("grizzly");
        listener.getKeepAlive().setIdleTimeoutInSeconds(-1);

        return httpServer;

    }

    private void setInitialHttp2WindowSize(FilterChain filterChain, int windowSize) {

        int http2FilterIdx = filterChain.indexOfType(Http2BaseFilter.class);
        Http2BaseFilter http2Filter = (Http2BaseFilter) filterChain.get(http2FilterIdx);
        http2Filter.getConfiguration().setInitialWindowSize(windowSize);
    }

    private static class CustomIOException extends IOException {
        private static long serialVersionUID = 1L;
    }
}
