/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.portunif;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.INFO;
import static org.glassfish.grizzly.portunif.ProtocolFinder.Result.FOUND;
import static org.glassfish.grizzly.portunif.ProtocolFinder.Result.NOT_FOUND;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.EchoFilter;
import org.glassfish.grizzly.utils.StringEncoder;
import org.glassfish.grizzly.utils.StringFilter;

import junit.framework.TestCase;

/**
 * Test {@link FilterChain} blocking read.
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class FilterChainReadTest extends TestCase {
    public static int PORT = PORT();

    private static Logger logger = Grizzly.logger(FilterChainReadTest.class);
    
    protected static int PORT() {
        try {
            int port = 7785 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void testBlockingRead() throws Exception {
        String[] clientMsgs = { "XXXXX", "Hello", "from", "client" };

        Connection<WriteResult> connection = null;
        int messageNum = 3;

        BlockingQueue<String> intermResultQueue = new LinkedTransferQueue<>();

        PUFilter puFilter = new PUFilter();
        FilterChain subProtocolChain = 
            puFilter.getPUFilterChainBuilder()
                    .add(new MergeFilter(clientMsgs.length, intermResultQueue))
                    .add(new EchoFilter())
                    .build();

        puFilter.register(new SimpleProtocolFinder(clientMsgs[0]), subProtocolChain);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(puFilter);

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            Thread.sleep(10);
            transport.bind(PORT);
            transport.start();

            BlockingQueue<String> resultQueue = new LinkedTransferQueue<>();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);

            FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            clientFilterChainBuilder.add(new BaseFilter() {

                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException {
                    resultQueue.add((String) ctx.getMessage());
                    return ctx.getStopAction();
                }

            });
            FilterChain clientFilterChain = clientFilterChainBuilder.build();

            connection.setProcessor(clientFilterChain);

            for (int i = 0; i < messageNum; i++) {
                String clientMessage = "";

                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    Future<WriteResult<String, WriteResult>> writeFuture = connection.write(msg);

                    assertTrue("Write timeout loop: " + i, writeFuture.get(10, SECONDS) != null);

                    String srvInterm = intermResultQueue.poll(10, SECONDS);

                    assertEquals("Unexpected interm. response (" + i + ", " + j + ")", msg, srvInterm);

                    clientMessage += msg;
                }

                String message = resultQueue.poll(10, SECONDS);

                assertEquals("Unexpected response (" + i + ")", clientMessage, message);
            }
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public void testBlockingReadWithRemainder() throws Exception {
        String[] clientMsgs = { "YYYYY", "Hello", "from", "client" };

        Connection connection = null;
        int messageNum = 3;

        BlockingQueue<String> intermResultQueue = new LinkedTransferQueue<>();

        PUFilter puFilter = new PUFilter();
        FilterChain subProtocolChain = puFilter.getPUFilterChainBuilder().add(new MergeFilter(clientMsgs.length, intermResultQueue)).add(new EchoFilter())
                .build();

        puFilter.register(new SimpleProtocolFinder(clientMsgs[0]), subProtocolChain);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(puFilter);

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            Thread.sleep(10);
            transport.bind(PORT);
            transport.start();

            BlockingQueue<String> resultQueue = new LinkedTransferQueue<>();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);

            FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            clientFilterChainBuilder.add(new BaseFilter() {

                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException {
                    resultQueue.add((String) ctx.getMessage());
                    return ctx.getStopAction();
                }

            });
            FilterChain clientFilterChain = clientFilterChainBuilder.build();

            connection.setProcessor(clientFilterChain);

            for (int i = 0; i < messageNum; i++) {
                String clientMessage = "";

                CompositeBuffer bb = CompositeBuffer.newBuffer(transport.getMemoryManager());

                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    clientMessage += msg;
                    StringEncoder stringEncoder = new StringEncoder();
                    TransformationResult<String, Buffer> result = stringEncoder.transform(connection, msg);
                    Buffer buffer = result.getMessage();
                    bb.append(buffer);
                }

                Future<WriteResult<WritableMessage, SocketAddress>> writeFuture = transport.getAsyncQueueIO().getWriter().write(connection, bb);

                assertTrue("Write timeout loop: " + i, writeFuture.get(10, SECONDS) != null);

                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    String srvInterm = intermResultQueue.poll(10, SECONDS);

                    assertEquals("Unexpected interm. response (" + i + ", " + j + ")", msg, srvInterm);
                }

                String message = resultQueue.poll(10, SECONDS);

                assertEquals("Unexpected response (" + i + ")", clientMessage, message);
            }
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public void testBlockingReadError() throws Exception {
        String[] clientMsgs = { "ZZZZZ", "Hello", "from", "client" };

        Connection connection = null;

        BlockingQueue intermResultQueue = new LinkedTransferQueue();

        PUFilter puFilter = new PUFilter();
        FilterChain subProtocolChain = puFilter.getPUFilterChainBuilder().add(new MergeFilter(clientMsgs.length, intermResultQueue)).add(new EchoFilter())
                .build();

        puFilter.register(new SimpleProtocolFinder(clientMsgs[0]), subProtocolChain);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(puFilter);

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            Thread.sleep(10);
            transport.bind(PORT);
            transport.start();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);

            FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            FilterChain clientFilterChain = clientFilterChainBuilder.build();

            connection.setProcessor(clientFilterChain);

            String msg = clientMsgs[0];
            Future<WriteResult> writeFuture = connection.write(msg);

            assertTrue("Write timeout", writeFuture.get(10, SECONDS) != null);

            String srvInterm = (String) intermResultQueue.poll(10, SECONDS);

            assertEquals("Unexpected interm. response", msg, srvInterm);

            connection.closeSilently();
            connection = null;

            Exception e = (Exception) intermResultQueue.poll(10, SECONDS);

            assertTrue("Unexpected response. Exception: " + e.getClass() + ": " + e.getMessage(), e instanceof EOFException);
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    private static class SimpleProtocolFinder implements ProtocolFinder {
        public String name;

        public SimpleProtocolFinder(String name) {
            this.name = name;
        }

        @Override
        public Result find(PUContext puContext, FilterChainContext ctx) {
            String requestedProtocolName = ctx.getMessage();

            return requestedProtocolName.startsWith(name) ? FOUND : NOT_FOUND;
        }
    }

    private static class MergeFilter extends BaseFilter {

        private int clientMsgs;
        private BlockingQueue intermResultQueue;

        public MergeFilter(int clientMsgs, BlockingQueue intermResultQueue) {
            this.clientMsgs = clientMsgs;
            this.intermResultQueue = intermResultQueue;
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {

            String message = ctx.getMessage();

            logger.log(INFO, "First chunk come: {0}", message);
            intermResultQueue.add(message);

            Connection connection = ctx.getConnection();
            connection.setReadTimeout(10, SECONDS);

            try {
                for (int i = 0; i < clientMsgs - 1; i++) {
                    ReadResult rr = ctx.read();
                    String blckMsg = (String) rr.getMessage();

                    rr.recycle();
                    logger.log(INFO, "Blocking chunk come: {0}", blckMsg);
                    intermResultQueue.add(blckMsg);
                    message += blckMsg;
                }
            } catch (Exception e) {
                intermResultQueue.add(e);
                return ctx.getStopAction();
            }

            ctx.setMessage(message);

            return ctx.getInvokeAction();
        }
    }
}
