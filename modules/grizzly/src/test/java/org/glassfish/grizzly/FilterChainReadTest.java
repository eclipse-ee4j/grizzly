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

package org.glassfish.grizzly;

import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.EchoFilter;
import org.glassfish.grizzly.utils.StringEncoder;
import org.glassfish.grizzly.utils.StringFilter;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;

/**
 * Test {@link FilterChain} blocking read.
 * 
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class FilterChainReadTest extends TestCase {
    public static final int PORT = PORT();
    
    static int PORT() {
        try {
            int port = 7785 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger logger = Grizzly.logger(FilterChainReadTest.class);

    public void testBlockingRead() throws Exception {
        final String[] clientMsgs = {"Hello", "from", "client"};
        
        Connection connection = null;
        int messageNum = 3;

        final BlockingQueue<String> intermResultQueue = new LinkedTransferQueue<>();
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(new BaseFilter() {
            @Override
            public NextAction handleRead(FilterChainContext ctx)
                    throws IOException {

                String message = ctx.getMessage();

                logger.log(Level.INFO, "First chunk come: {0}", message);
                intermResultQueue.add(message);

                Connection connection = ctx.getConnection();
                connection.setReadTimeout(10, TimeUnit.SECONDS);

                for (int i = 0; i < clientMsgs.length - 1; i++) {
                    final ReadResult rr = ctx.read();
                    final String blckMsg = (String) rr.getMessage();

                    rr.recycle();
                    logger.log(Level.INFO, "Blocking chunk come: {0}", blckMsg);
                    intermResultQueue.add(blckMsg);
                    message += blckMsg;
                }

                ctx.setMessage(message);

                return ctx.getInvokeAction();
            }
        });
        filterChainBuilder.add(new EchoFilter());


        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            Thread.sleep(400);
            transport.bind(PORT);
            Thread.sleep(400);
            transport.start();

            final BlockingQueue<String> resultQueue = new LinkedTransferQueue<>();

            FilterChainBuilder clientFilterChainBuilder =
                    FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            clientFilterChainBuilder.add(new BaseFilter() {

                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException {
                    resultQueue.add((String) ctx.getMessage());
                    return ctx.getStopAction();
                }

            });
            final FilterChain clientFilterChain = clientFilterChainBuilder.build();

            SocketConnectorHandler connectorHandler =
                    TCPNIOConnectorHandler.builder(transport)
                    .processor(clientFilterChain)
                    .build();

            
            Future<Connection> future = connectorHandler.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            for (int i = 0; i < messageNum; i++) {
                String clientMessage = "";

                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    Future<WriteResult> writeFuture = connection.write(msg);

                    assertTrue("Write timeout loop: " + i,
                            writeFuture.get(10, TimeUnit.SECONDS) != null);

                    final String srvInterm = intermResultQueue.poll(10, TimeUnit.SECONDS);

                    assertEquals("Unexpected interm. response (" + i + ", " + j + ")", msg, srvInterm);

                    clientMessage += msg;
                }


                final String message = resultQueue.poll(10, TimeUnit.SECONDS);

                assertEquals("Unexpected response (" + i + ")",
                        clientMessage, message);
            }
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public void testBlockingReadWithRemainder() throws Exception {
        final String[] clientMsgs = {"Hello", "from", "client"};

        Connection connection = null;
        int messageNum = 3;

        final BlockingQueue<String> intermResultQueue = new LinkedTransferQueue<>();
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(new BaseFilter() {
            @Override
            public NextAction handleRead(FilterChainContext ctx)
                    throws IOException {

                String message = ctx.getMessage();

                logger.log(Level.INFO, "First chunk come: {0}", message);
                intermResultQueue.add(message);

                Connection connection = ctx.getConnection();
                connection.setReadTimeout(10, TimeUnit.SECONDS);

                for (int i = 0; i < clientMsgs.length - 1; i++) {
                    final ReadResult rr = ctx.read();
                    final String blckMsg = (String) rr.getMessage();

                    rr.recycle();
                    logger.log(Level.INFO, "Blocking chunk come: {0}", blckMsg);
                    intermResultQueue.add(blckMsg);
                    message += blckMsg;
                }

                ctx.setMessage(message);

                return ctx.getInvokeAction();
            }
        });
        filterChainBuilder.add(new EchoFilter());


        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            transport.bind(PORT);
            transport.start();

            final BlockingQueue<String> resultQueue = new LinkedTransferQueue<>();

            FilterChainBuilder clientFilterChainBuilder =
                    FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            clientFilterChainBuilder.add(new BaseFilter() {

                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException {
                    resultQueue.add((String) ctx.getMessage());
                    return ctx.getStopAction();
                }

            });
            final FilterChain clientFilterChain = clientFilterChainBuilder.build();

            SocketConnectorHandler connectorHandler =
                    TCPNIOConnectorHandler.builder(transport)
                    .processor(clientFilterChain)
                    .build();
            
            Future<Connection> future = connectorHandler.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            for (int i = 0; i < messageNum; i++) {
                String clientMessage = "";

                CompositeBuffer bb = CompositeBuffer.newBuffer(transport.getMemoryManager());
                
                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    clientMessage += msg;
                    StringEncoder stringEncoder = new StringEncoder();
                    TransformationResult<String, Buffer> result =
                            stringEncoder.transform(connection, msg);
                    Buffer buffer = result.getMessage();
                    bb.append(buffer);
                }


                Future<WriteResult<WritableMessage, SocketAddress>> writeFuture =
                        transport.getAsyncQueueIO().getWriter().write(connection, bb);

                assertTrue("Write timeout loop: " + i,
                        writeFuture.get(10, TimeUnit.SECONDS) != null);

                for (int j = 0; j < clientMsgs.length; j++) {
                    String msg = clientMsgs[j] + "-" + i;
                    final String srvInterm = intermResultQueue.poll(10, TimeUnit.SECONDS);

                    assertEquals("Unexpected interm. response (" + i + ", " + j + ")", msg, srvInterm);
                }


                final String message = resultQueue.poll(10, TimeUnit.SECONDS);

                assertEquals("Unexpected response (" + i + ")",
                        clientMessage, message);
            }
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public void testBlockingReadError() throws Exception {
        Connection connection = null;

        final BlockingQueue<Object> intermResultQueue = new LinkedTransferQueue<>();
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(new BaseFilter() {
            @Override
            public NextAction handleRead(FilterChainContext ctx)
                    throws IOException {

                String message = ctx.getMessage();

                logger.log(Level.INFO, "First chunk come: {0}", message);
                intermResultQueue.add(message);

                Connection connection = ctx.getConnection();
                connection.setReadTimeout(10, TimeUnit.SECONDS);

                try {
                    final ReadResult rr = ctx.read();
                    intermResultQueue.add(rr);
                } catch (Exception e) {
                    intermResultQueue.add(e);
                }

                return ctx.getStopAction();
            }
        });


        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            transport.bind(PORT);
            transport.start();

            FilterChainBuilder clientFilterChainBuilder =
                    FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());
            final FilterChain clientFilterChain = clientFilterChainBuilder.build();

            SocketConnectorHandler connectorHandler =
                    TCPNIOConnectorHandler.builder(transport)
                    .processor(clientFilterChain)
                    .build();
            
            Future<Connection> future = connectorHandler.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            String msg = "Hello";
            Future<WriteResult> writeFuture = connection.write(msg);

            assertTrue("Write timeout",
                    writeFuture.get(10, TimeUnit.SECONDS) != null);

            final String srvInterm = (String) intermResultQueue.poll(10, TimeUnit.SECONDS);

            assertEquals("Unexpected interm. response", msg, srvInterm);

            connection.closeSilently();
            connection = null;
            
            final Exception e = (Exception) intermResultQueue.poll(10, TimeUnit.SECONDS);

            assertTrue("Unexpected response. Exception: " + e.getClass() + ": " + e.getMessage(),
                    e instanceof EOFException);
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }
}
