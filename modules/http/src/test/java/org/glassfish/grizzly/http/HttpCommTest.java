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

package org.glassfish.grizzly.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.ChunkingFilter;

import junit.framework.TestCase;

/**
 * Test HTTP communication
 *
 * @author Alexey Stashok
 */
public class HttpCommTest extends TestCase {

    private static final Logger logger = Grizzly.logger(HttpCommTest.class);

    public static final int PORT = 19002;

    @SuppressWarnings("unchecked")
    public void testSinglePacket() throws Exception {
        final FilterChain serverFilterChain = FilterChainBuilder.stateless().add(new TransportFilter()).add(new ChunkingFilter(2)).add(new HttpServerFilter())
                .add(new DummyServerFilter()).build();

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(serverFilterChain);

        Connection connection = null;
        try {
            transport.bind(PORT);
            transport.start();

            final BlockingQueue<HttpPacket> resultQueue = new LinkedTransferQueue<>();

            final FilterChain clientFilterChain = FilterChainBuilder.stateless().add(new TransportFilter()).add(new ChunkingFilter(2))
                    .add(new HttpClientFilter()).add(new BaseFilter() {
                        @Override
                        public NextAction handleRead(FilterChainContext ctx) throws IOException {
                            resultQueue.add((HttpPacket) ctx.getMessage());
                            return ctx.getStopAction();
                        }
                    }).build();

            final SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(transport).processor(clientFilterChain).build();

            Future<Connection> future = connectorHandler.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            int clientPort = ((InetSocketAddress) connection.getLocalAddress()).getPort();
            assertNotNull(connection);

            HttpRequestPacket httpRequest = HttpRequestPacket.builder().method("GET").uri("/dummyURL").query("p1=v1&p2=v2").protocol(Protocol.HTTP_1_0)
                    .header("client-port", Integer.toString(clientPort)).header("Host", "localhost").build();

            Future<WriteResult> writeResultFuture = connection.write(httpRequest);
            writeResultFuture.get(10, TimeUnit.SECONDS);

            HttpContent response = (HttpContent) resultQueue.poll(10, TimeUnit.SECONDS);
            HttpResponsePacket responseHeader = (HttpResponsePacket) response.getHttpHeader();

            assertEquals(httpRequest.getRequestURI(), responseHeader.getHeader("Found"));

        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public static class DummyServerFilter extends BaseFilter {

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {

            final HttpContent httpContent = ctx.getMessage();
            final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();

            logger.log(Level.FINE, "Got the request: {0}", request);

            assertEquals(PORT, request.getLocalPort());
            assertTrue(isLocalAddress(request.getLocalAddress()));
            assertTrue(isLocalAddress(request.getRemoteHost()));
            assertTrue(isLocalAddress(request.getRemoteAddress()));
            assertEquals(request.getHeader("client-port"), Integer.toString(request.getRemotePort()));

            HttpResponsePacket response = request.getResponse();
            HttpStatus.OK_200.setValues(response);

            response.addHeader("Content-Length", "0");

            // Set header using headers collection (just for testing reasons)
            final String junk = "---junk---";
            final Buffer foundBuffer = Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, junk + "Found");
            response.getHeaders().addValue(foundBuffer, junk.length(), foundBuffer.remaining() - junk.length()).setString(request.getRequestURI());
//            response.addHeader("Found", request.getRequestURI());

            ctx.write(response);

            return ctx.getStopAction();
        }
    }

    private static boolean isLocalAddress(String address) throws IOException {
        final InetAddress inetAddr = InetAddress.getByName(address);

        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface ni = e.nextElement();
            Enumeration<InetAddress> inetAddrs = ni.getInetAddresses();
            while (inetAddrs.hasMoreElements()) {
                InetAddress addr = inetAddrs.nextElement();
                if (addr.equals(inetAddr)) {
                    return true;
                }
            }
        }

        return false;
    }
}
