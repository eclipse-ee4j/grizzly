/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.io.NIOInputStream;
import org.glassfish.grizzly.http.server.util.AggregatorAddOn;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.utils.DelayFilter;
import org.glassfish.grizzly.utils.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Test AggregatorAddOn
 * 
 * @author Alexey Stashok
 */
@RunWith(Parameterized.class)
public class AggregatorAddOnTest {
    private static final int PORT = 18904;
    private static final Logger LOGGER = Grizzly.logger(AggregatorAddOnTest.class);

    @Parameterized.Parameters
    public static Collection<Object[]> getMode() {
        return Arrays.asList(new Object[][]{
                    {Boolean.FALSE},
                    {Boolean.TRUE},
                });
    }

    private HttpServer httpServer;
    
    final boolean isSslEnabled;
    
    public AggregatorAddOnTest(boolean isSslEnabled) {
        this.isSslEnabled = isSslEnabled;
    }

    @Before
    public void before() throws Exception {
        httpServer = createServer(isSslEnabled);
        httpServer.start();
    }

    @After
    public void after() throws Exception {
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGet() throws Exception {
        final FutureImpl<HttpContent> result = Futures.createSafeFuture();

        TCPNIOTransport client = createClient(result, isSslEnabled);
        
        try {
            client.start();
            Connection c = client.connect("localhost", PORT).get(10, TimeUnit.SECONDS);
            
            HttpRequestPacket request =
                    HttpRequestPacket.builder().uri("/")
                        .method(Method.GET)
                        .protocol(Protocol.HTTP_1_1)
                        .header("Host", "localhost:" + PORT).build();
            c.write(request);
            
            final HttpContent responseContent = result.get(10, TimeUnit.SECONDS);
            final HttpResponsePacket response = (HttpResponsePacket) responseContent.getHttpHeader();
            
            assertEquals(200, response.getStatus());
            assertEquals(0, Integer.parseInt(response.getHeader("In-Length")));
            c.close();
        } finally {
            client.shutdownNow();
        }        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testPostContentLength() throws Exception {
        final FutureImpl<HttpContent> result = Futures.createSafeFuture();

        TCPNIOTransport client = createClient(result, isSslEnabled);
        
        try {
            client.start();
            Connection c = client.connect("localhost", PORT).get(10, TimeUnit.SECONDS);
            
            HttpRequestPacket request =
                    HttpRequestPacket.builder().uri("/")
                        .method(Method.POST)
                        .protocol(Protocol.HTTP_1_1)
                        .header("Host", "localhost:" + PORT)
                        .contentLength(15)
                        .build();
            c.write(request);
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).build());
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).build());
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).build());
            
            final HttpContent responseContent = result.get(10, TimeUnit.SECONDS);
            final HttpResponsePacket response = (HttpResponsePacket) responseContent.getHttpHeader();
            
            assertEquals(200, response.getStatus());
            assertEquals(15, Integer.parseInt(response.getHeader("In-Length")));
            c.close();
        } finally {
            client.shutdownNow();
        }        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testPostChunked() throws Exception {
        final FutureImpl<HttpContent> result = Futures.createSafeFuture();

        TCPNIOTransport client = createClient(result, isSslEnabled);
        
        try {
            client.start();
            Connection c = client.connect("localhost", PORT).get(10, TimeUnit.SECONDS);
            
            HttpRequestPacket request =
                    HttpRequestPacket.builder().uri("/")
                        .method(Method.POST)
                        .protocol(Protocol.HTTP_1_1)
                        .header("Host", "localhost:" + PORT)
                        .chunked(true)
                        .build();
            c.write(request);
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).build());
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).build());
            Thread.sleep(500);
            c.write(HttpContent.builder(request).content(Buffers.wrap(null, new byte[5])).last(true).build());
            
            final HttpContent responseContent = result.get(10, TimeUnit.SECONDS);
            final HttpResponsePacket response = (HttpResponsePacket) responseContent.getHttpHeader();
            
            assertEquals(200, response.getStatus());
            assertEquals(15, Integer.parseInt(response.getHeader("In-Length")));
            c.close();
        } finally {
            client.shutdownNow();
        }        
    }
    
    private static TCPNIOTransport createClient(final FutureImpl<HttpContent> result,
            final boolean isSslEnabled) throws Exception {
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        FilterChainBuilder builder = FilterChainBuilder.stateless();
        builder.add(new TransportFilter());
        
        // simulate slow read
        builder.add(new DelayFilter(5, 0));
        
        if (isSslEnabled) {
            final SSLFilter sslFilter = new SSLFilter(createSSLConfig(true),
                    createSSLConfig(false));
            builder.add(sslFilter);
        }
        
        builder.add(new HttpClientFilter());
        builder.add(new BaseFilter() {
            int total;
            FileChannel out;
            File f;
            long start;
            long stop;

            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent content = ctx.getMessage();
                final int remaining = content.getContent().remaining();
                total += remaining;
                out.write(content.getContent().toByteBuffer());
                if (content.isLast()) {
                    total = 0;
                    stop = System.currentTimeMillis();
                    
                    try {
                        out.close();
                        LOGGER.log(Level.INFO, "Client received file ({0} bytes) in {1}ms.",
                                new Object[]{f.length(), stop - start});
                        // result.result(f) should be the last operation in handleRead
                        // otherwise NPE may occur in handleWrite asynchronously
                        result.result(content);
                    } catch (Throwable e) {
                        result.failure(e);
                    }
                }
                return ctx.getStopAction();
            }

            @Override
            public NextAction handleWrite(FilterChainContext ctx) throws IOException {
                try {
                    if (f != null) {
                        if (!f.delete()) {
                            LOGGER.log(Level.WARNING, "Unable to explicitly delete file: {0}", f.getAbsolutePath());
                        }
                        f = null;
                    }
                    f = File.createTempFile("grizzly-test-", ".tmp");
                    f.deleteOnExit();
                    out = new FileOutputStream(f).getChannel();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                start = System.currentTimeMillis();
                return super.handleWrite(ctx);
            }
        });
        
        transport.setProcessor(builder.build());
        return transport;
    }
    
    private static HttpServer createServer(boolean isSslEnabled) throws Exception {
        final HttpServer server = new HttpServer();
        final NetworkListener listener = 
                new NetworkListener("test", 
                                    NetworkListener.DEFAULT_NETWORK_HOST, 
                                    PORT);
        
        if (isSslEnabled) {
            listener.setSecure(true);
            listener.setSSLEngineConfig(createSSLConfig(true));
        }
        
        listener.registerAddOn(new AggregatorAddOn());
        
        server.addListener(listener);
        
        server.getServerConfiguration().addHttpHandler(new HttpHandler("AggregatorAddOn-Test") {
            @Override
            public void service(final Request request, final Response response)
                    throws Exception {
                byte[] inArray = new byte[4096];
                final NIOInputStream in = request.getNIOInputStream();
                
                final int inSize = in.available();
                int remaining = inSize;
                
                while (remaining > 0) {
                    final int readNow = in.read(inArray);
                    if (readNow <= 0) {
                        throw new Exception("Enexpected number of bytes read: " + readNow);
                    }
                    
                    remaining -= readNow;
                }
                
                if (!request.getNIOInputStream().isFinished()) {
                    throw new Exception("InputStream supposed to be finished");
                }
                
                response.setHeader("In-Length", String.valueOf(inSize));
            }
        });
        
        return server;
    }
    
    private static SSLEngineConfigurator createSSLConfig(boolean isServer) throws Exception {
        final SSLContextConfigurator sslContextConfigurator =
                new SSLContextConfigurator();
        final ClassLoader cl = SuspendTest.class.getClassLoader();
        // override system properties
        final URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        if (cacertsUrl != null) {
            sslContextConfigurator.setTrustStoreFile(cacertsUrl.getFile());
            sslContextConfigurator.setTrustStorePass("changeit");
        }

        // override system properties
        final URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        if (keystoreUrl != null) {
            sslContextConfigurator.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfigurator.setKeyStorePass("changeit");
        }

        return new SSLEngineConfigurator(sslContextConfigurator.createSSLContext(),
                !isServer, false, false);
    }    
}
