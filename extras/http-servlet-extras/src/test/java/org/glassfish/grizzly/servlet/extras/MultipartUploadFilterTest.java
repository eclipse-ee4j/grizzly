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

package org.glassfish.grizzly.servlet.extras;

import junit.framework.Assert;
import junit.framework.TestCase;
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
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.servlet.extras.util.MultipartEntryPacket;
import org.glassfish.grizzly.servlet.extras.util.MultipartPacketBuilder;
import org.glassfish.grizzly.utils.ChunkingFilter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.utils.Futures;

public class MultipartUploadFilterTest extends TestCase {

    private static final int PORT = 9977;

    public void testBasicMultipartUploadFilter001() throws Exception {

        HttpServer httpServer = HttpServer.createSimpleServer(".", 9977);
        WebappContext ctx = new WebappContext("Upload Test");
        final String fileContent = "One Ring to rule them all, One Ring to find them,\n" +
                        "One Ring to bring them all and in the darkness bind them.";
        FilterRegistration filterRegistration =
                ctx.addFilter("UploadFilter", MultipartUploadFilter.class.getName());
        filterRegistration.addMappingForUrlPatterns(null, "/upload");
        final AtomicReference<File> uploadedFile = new AtomicReference<File>();
        final ServletRegistration servletRegistration =
                ctx.addServlet("UploadValidationServlet", new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req,
                                  HttpServletResponse resp) throws ServletException, IOException {

                final Object value = req.getAttribute(MultipartUploadFilter.UPLOADED_FILES);
                Assert.assertNotNull(value);
                Assert.assertTrue(value instanceof File[]);
                final File[] uploadedFiles = (File[]) value;
                Assert.assertEquals(1, uploadedFiles.length);
                final File f = uploadedFiles[0];
                Reader r = new InputStreamReader(new FileInputStream(f));
                char[] buf = new char[512];
                int read = r.read(buf);
                r.close();
                Assert.assertEquals(fileContent, new String(buf, 0, read));
                Assert.assertTrue(f.exists());
                Assert.assertTrue(f.canRead());
                uploadedFile.set(f);
            }
        });
        servletRegistration.addMapping("/upload");

        try {
            ctx.deploy(httpServer);
            httpServer.start();

            final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
            clientTransport.start();
            HttpClient client = new HttpClient(clientTransport);
            Future conn = client.connect("localhost", PORT);
            conn.get(10, TimeUnit.SECONDS);
            Future<HttpPacket> future = client.get(createMultipartPacket(fileContent));
            HttpPacket packet = future.get(10, TimeUnit.SECONDS);
            HttpResponsePacket response = (HttpResponsePacket) packet.getHttpHeader();
            Assert.assertEquals(200, response.getStatus());
            File f = uploadedFile.get();
            Assert.assertNotNull(f);
            Assert.assertFalse(f.exists());
        } finally {
            httpServer.shutdownNow();
        }
    }

    public void testBasicMultipartUploadFilter002() throws Exception {

        HttpServer httpServer = HttpServer.createSimpleServer(".", 9977);
        WebappContext ctx = new WebappContext("Upload Test");
        final String fileContent = "One Ring to rule them all, One Ring to find them,\n" +
                        "One Ring to bring them all and in the darkness bind them.";
        FilterRegistration filterRegistration =
                ctx.addFilter("UploadFilter", MultipartUploadFilter.class.getName());
        filterRegistration.setInitParameter(MultipartUploadFilter.DELETE_ON_REQUEST_END, "false");
        filterRegistration.addMappingForUrlPatterns(null, "/upload");
        final AtomicReference<File> uploadedFile = new AtomicReference<File>();
        final ServletRegistration servletRegistration =
                ctx.addServlet("UploadValidationServlet", new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req,
                                  HttpServletResponse resp) throws ServletException, IOException {

                final Object value = req.getAttribute(MultipartUploadFilter.UPLOADED_FILES);
                Assert.assertNotNull(value);
                Assert.assertTrue(value instanceof File[]);
                final File[] uploadedFiles = (File[]) value;
                Assert.assertEquals(1, uploadedFiles.length);
                final File f = uploadedFiles[0];
                Reader r = new InputStreamReader(new FileInputStream(f));
                char[] buf = new char[512];
                int read = r.read(buf);
                Assert.assertEquals(fileContent, new String(buf, 0, read));
                Assert.assertTrue(f.exists());
                Assert.assertTrue(f.canRead());
                uploadedFile.set(f);
            }
        });
        servletRegistration.addMapping("/upload");

        try {
            ctx.deploy(httpServer);
            httpServer.start();

            final TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
            clientTransport.start();
            HttpClient client = new HttpClient(clientTransport);
            Future conn = client.connect("localhost", PORT);
            conn.get(10, TimeUnit.SECONDS);
            Future<HttpPacket> future = client.get(createMultipartPacket(fileContent));
            HttpPacket packet = future.get(10, TimeUnit.SECONDS);
            HttpResponsePacket response = (HttpResponsePacket) packet.getHttpHeader();
            Assert.assertEquals(200, response.getStatus());
            File f = uploadedFile.get();
            Assert.assertNotNull(f);
            Assert.assertTrue(f.exists());
            f.deleteOnExit();
        } finally {
            httpServer.shutdownNow();
        }
    }


    // --------------------------------------------------------- Private Methods


    private HttpPacket createMultipartPacket(final String content) {
        String boundary = "---------------------------103832778631715";
        MultipartPacketBuilder mpb = MultipartPacketBuilder.builder(boundary);
        mpb.preamble("preamble").epilogue("epilogue");

        mpb.addMultipartEntry(MultipartEntryPacket.builder()
                .contentDisposition("form-data; name=\"test.txt\"; filename=\"test.txt\"")
                .content(content)
                .build());


        final Buffer bodyBuffer = mpb.build();

        final HttpRequestPacket requestHeader = HttpRequestPacket.builder()
                .method(Method.POST)
                .uri("/upload")
                .protocol(Protocol.HTTP_1_1)
                .header("host", "localhost")
                .contentType("multipart/form-data; boundary=" + boundary)
                .contentLength(bodyBuffer.remaining())
                .build();

        return HttpContent.builder(requestHeader)
                .content(bodyBuffer)
                .build();
    }


    // ---------------------------------------------------------- Nested Classes


    private static class HttpClient {
        private final TCPNIOTransport transport;
        private final int chunkSize;

        private volatile Connection connection;
        private volatile FutureImpl<HttpPacket> asyncFuture;

        public HttpClient(TCPNIOTransport transport) {
            this(transport, -1);
        }

        public HttpClient(TCPNIOTransport transport, int chunkSize) {
            this.transport = transport;
            this.chunkSize = chunkSize;
        }

        public Future<Connection> connect(String host, int port) throws IOException {
            FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
            filterChainBuilder.add(new TransportFilter());

            if (chunkSize > 0) {
                filterChainBuilder.add(new ChunkingFilter(chunkSize));
            }

            filterChainBuilder.add(new HttpClientFilter());
            filterChainBuilder.add(new HttpResponseFilter());

            final SocketConnectorHandler connector =
                    TCPNIOConnectorHandler.builder(transport)
                    .processor(filterChainBuilder.build())
                    .build();

            final FutureImpl<Connection> future =
                    Futures.createSafeFuture();
            
            connector.connect(new InetSocketAddress(host, port),
                    Futures.toCompletionHandler(future, 
                    new EmptyCompletionHandler<Connection>() {
                @Override
                public void completed(Connection result) {
                    connection = result;
                }
            }));
            
            return future;
        }

        @SuppressWarnings("unchecked")
        public Future<HttpPacket> get(HttpPacket request) throws IOException {
            final FutureImpl<HttpPacket> localFuture = SafeFutureImpl.create();
            asyncFuture = localFuture;
            connection.write(request, new EmptyCompletionHandler() {

                @Override
                public void failed(Throwable throwable) {
                    localFuture.failure(throwable);
                }
            });

            connection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type)
                        throws IOException {
                    localFuture.failure(new IOException());
                }
            });
            return localFuture;
        }

        public void close() throws IOException {
            if (connection != null) {
                connection.close();
            }
        }

        private class HttpResponseFilter extends BaseFilter {
            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                HttpContent message = ctx.getMessage();
                if (message.isLast()) {
                    final FutureImpl<HttpPacket> localFuture = asyncFuture;
                    asyncFuture = null;
                    localFuture.result(message);

                    return ctx.getStopAction();
                }

                return ctx.getStopAction(message);
            }
        } // END HttpResponseFilter

    } // END HttpClient

}
