/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;
import static org.glassfish.grizzly.IOEvent.READ;
import static org.glassfish.grizzly.IOEvent.SERVER_ACCEPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.nio.AbstractNIOConnectionDistributor;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.RegisterChannelResult;
import org.glassfish.grizzly.nio.SelectorRunner;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.streams.StreamReader;
import org.glassfish.grizzly.streams.StreamWriter;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.ClientCheckFilter;
import org.glassfish.grizzly.utils.EchoFilter;
import org.glassfish.grizzly.utils.Futures;
import org.glassfish.grizzly.utils.ParallelWriteFilter;
import org.glassfish.grizzly.utils.RandomDelayOnWriteFilter;
import org.glassfish.grizzly.utils.StringFilter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link TCPNIOTransport}
 *
 * @author Alexey Stashok
 */
public class TCPNIOTransportTest {

    public static final int PORT = 9981;

    private static final Logger logger = Grizzly.logger(TCPNIOTransportTest.class);

    @Before
    public void setUp() throws Exception {
        ByteBufferWrapper.DEBUG_MODE = true;
    }

    @Test
    public void testBindUnbind() throws Exception {
        logger.info("Starting test");
        
        Connection<?> connection = null;
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        try {
            bindToPort(transport);

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);
            connection.closeSilently();

            transport.unbindAll();

            future = transport.connect("localhost", PORT);
            try {
                future.get(10, SECONDS);
                fail("Server connection should be closed!");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof IOException);
            }

            logger.info("Binding to port " + PORT);
            transport.bind(PORT);

            future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    @Test
    public void testMultiBind() throws Exception {
        logger.info("Starting test");
        
        Connection<?> connection = null;
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        try {
            logger.info("Binding to port " + PORT);

            final Connection serverConnection1 = transport.bind(PORT);

            logger.info("Binding to port " + (PORT + 1));
            final Connection serverConnection2 = transport.bind(PORT + 1);

            transport.start();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);
            connection.closeSilently();

            future = transport.connect("localhost", PORT + 1);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);
            connection.closeSilently();

            transport.unbind(serverConnection1);

            future = transport.connect("localhost", PORT);
            try {
                connection = future.get(10, SECONDS);
                fail("Server connection should be closed!");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof IOException);
            }

            transport.unbind(serverConnection2);
            future = transport.connect("localhost", PORT + 1);
            try {
                connection = future.get(10, SECONDS);
                fail("Server connection should be closed!");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof IOException);
            }

        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    @Test
    public void testClose() throws Exception {
        logger.info("Starting test");
        
        BlockingQueue<Connection<?>> acceptedQueue = new LinkedTransferQueue<>();

        Connection<?> connectedConnection = null;
        Connection<?> acceptedConnection = null;

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

        try {
            FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
            filterChainBuilder.add(new TransportFilter());
            filterChainBuilder.add(new BaseFilter() {

                @Override
                public NextAction handleAccept(final FilterChainContext ctx) throws IOException {
                    acceptedQueue.offer(ctx.getConnection());
                    return ctx.getInvokeAction();
                }
            });

            transport.setProcessor(filterChainBuilder.build());
            
            bindToPort(transport);

            Future<Connection> connectFuture = transport.connect(new InetSocketAddress("localhost", PORT));

            connectedConnection = connectFuture.get(10, SECONDS);
            acceptedConnection = acceptedQueue.poll(10, SECONDS);

            FutureImpl<Boolean> connectedCloseFuture = new SafeFutureImpl<Boolean>();
            FutureImpl<Boolean> acceptedCloseFuture = new SafeFutureImpl<Boolean>();

            // noinspection deprecation
            connectedConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    connectedCloseFuture.result(type == CloseType.LOCALLY);
                }
            });

            // noinspection deprecation
            acceptedConnection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(Closeable closeable, CloseType type) throws IOException {
                    acceptedCloseFuture.result(type == CloseType.REMOTELY);
                }
            });

            connectedConnection.closeSilently();

            assertTrue(connectedCloseFuture.get(10, SECONDS));
            assertTrue(acceptedCloseFuture.get(10, SECONDS));
        } finally {
            if (acceptedConnection != null) {
                acceptedConnection.closeSilently();
            }

            if (connectedConnection != null) {
                connectedConnection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    @Test
    public void testSelectorSwitch() throws Exception {
        logger.info("Starting test");
        
        Connection<?> connection = null;
        StreamReader reader;
        StreamWriter writer;

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

        CustomChannelDistributor distributor = new CustomChannelDistributor(transport);
        transport.setNIOChannelDistributor(distributor);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new BaseFilter() {

            @Override
            public NextAction handleAccept(FilterChainContext ctx) throws IOException {
                NIOConnection connection = (NIOConnection) ctx.getConnection();

                connection.attachToSelectorRunner(distributor.getSelectorRunner());
                connection.enableIOEvent(READ);

                return ctx.getInvokeAction();
            }
        });
        filterChainBuilder.add(new EchoFilter());

        transport.setProcessor(filterChainBuilder.build());

        transport.setSelectorRunnersCount(4);

        try {
            bindToPort(transport);
            

            final FutureImpl<Connection> connectFuture = Futures.createSafeFuture();
            transport.connect(new InetSocketAddress("localhost", PORT), Futures.toCompletionHandler(connectFuture, new EmptyCompletionHandler<Connection>() {

                @Override
                public void completed(Connection connection) {
                    synchronized (this) {
                        // noinspection deprecation
                        connection.configureStandalone(true);
                    }
                }
            }));
            connection = connectFuture.get(10, SECONDS);
            assertTrue(connection != null);

            connection.configureBlocking(true);

            byte[] originalMessage = "Hello".getBytes();
            writer = StandaloneProcessor.INSTANCE.getStreamWriter(connection);
            writer.writeByteArray(originalMessage);
            Future<Integer> writeFuture = writer.flush();

            assertTrue("Write timeout", writeFuture.isDone());
            assertEquals(originalMessage.length, (int) writeFuture.get());

            reader = StandaloneProcessor.INSTANCE.getStreamReader(connection);
            Future readFuture = reader.notifyAvailable(originalMessage.length);
            assertTrue("Read timeout", readFuture.get(10, SECONDS) != null);

            byte[] echoMessage = new byte[originalMessage.length];
            reader.readByteArray(echoMessage);
            assertTrue(Arrays.equals(echoMessage, originalMessage));
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    @Test
    public void testConnectFutureCancel() throws Exception {
        logger.info("Starting test");
        
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

        AtomicInteger serverConnectCounter = new AtomicInteger();
        AtomicInteger serverCloseCounter = new AtomicInteger();
        
        AtomicInteger clientConnectCounter = new AtomicInteger();
        AtomicInteger clientCloseCounter = new AtomicInteger();

        FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter()).add(new BaseFilter() {
            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {
                serverConnectCounter.incrementAndGet();
                logger.info("Connecting server: " + serverConnectCounter);
                return ctx.getInvokeAction();
            }

            @Override
            public NextAction handleClose(FilterChainContext ctx) throws IOException {
                serverCloseCounter.incrementAndGet();
                logger.info("Closing server: " + serverCloseCounter);
                return ctx.getInvokeAction();
            }
        });

        FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter()).add(new BaseFilter() {
            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {
                clientConnectCounter.incrementAndGet();
                logger.info("Connecting client: " + clientConnectCounter);
                return ctx.getInvokeAction();
            }

            @Override
            public NextAction handleClose(FilterChainContext ctx) throws IOException {
                clientCloseCounter.incrementAndGet();
                logger.info("Closing client: " + clientCloseCounter);
                return ctx.getInvokeAction();
            }
        });

        transport.setProcessor(serverFilterChainBuilder.build());

        SocketConnectorHandler connectorHandler = 
                TCPNIOConnectorHandler.builder(transport)
                                      .processor(clientFilterChainBuilder.build())
                                      .build();

        try {
            bindToPort(transport);

            int connectionsNum = 100;

            for (int i = 0; i < connectionsNum; i++) {
                Future<Connection> connectFuture = connectorHandler.connect(new InetSocketAddress("localhost", PORT));
                
                Thread.sleep(50);
                
                if (!connectFuture.cancel(false)) {
                    assertTrue("Future is not done", connectFuture.isDone());
                    
                    Connection connection = connectFuture.get();
                    assertNotNull("Connection is null?", connection);
                    assertTrue("Connection is not connected", connection.isOpen());
                    
                    connection.closeSilently();
                }
            }

            Thread.sleep(500);

            assertEquals("Number of connected and closed connections doesn't match", clientConnectCounter.get(), clientCloseCounter.get());
        } finally {
            transport.shutdownNow();
        }
    }

    @Test
    public void testParallelWritesBlockingMode() throws Exception {
        logger.info("Starting test");
        doTestParallelWrites(100, 100000, true);
    }

    @Test
    public void testThreadInterruptionDuringAcceptDoesNotMakeServerDeaf() throws Exception {
        logger.info("Starting test");
        
        Field interruptField = TCPNIOServerConnection.class.getDeclaredField("DISABLE_INTERRUPT_CLEAR");
        interruptField.setAccessible(true);
        interruptField.setBoolean(null, true);

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setSelectorRunnersCount(1);
        transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig().setCorePoolSize(1).setMaxPoolSize(1));
        transport.setIOStrategy(new SameThreadIOStrategyInterruptWrapper(true));
        
        bindToPort(transport);

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setIOStrategy(SameThreadIOStrategy.getInstance());

        try {
            clientTransport.start();
            SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(clientTransport)
                    .processor(FilterChainBuilder.stateless().add(new TransportFilter()).build()).build();
            try {
                Future<Connection> futureConnection = connectorHandler.connect("localhost", PORT);
                Connection connection = futureConnection.get(5, SECONDS);
                Thread.sleep(500); // Give a little time for the remote RST to be acknowledged.
            } catch (Exception e1) {
                System.out.println(e1.toString() + ".  This is expected.");
            }

            int successfulAttempts = 0;

            for (int i = 0; i < 10; i++) {
                try {
                    Future f2 = connectorHandler.connect("localhost", PORT);
                    f2.get(5, SECONDS);
                    System.out.println("Successful connection in " + ++successfulAttempts + " attempts.");
                    break;
                } catch (Exception e2) {
                    System.out.println(e2.toString() + ": not recovered yet...");
                }
            }

        } catch (Exception e) {
            fail("Unexpected Error: " + e.toString());
            e.printStackTrace();
        } finally {
            interruptField.setBoolean(null, false);
            clientTransport.shutdownNow();
            transport.shutdownNow();
        }
    }

    @Test
    public void testThreadInterruptionElsewhereDoesNotMakeServerDeaf() throws Exception {
        logger.info("Starting test");
        
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setSelectorRunnersCount(1);
        transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig().setCorePoolSize(1).setMaxPoolSize(1));
        transport.setIOStrategy(new SameThreadIOStrategyInterruptWrapper(false));
        
        bindToPort(transport);

        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setIOStrategy(SameThreadIOStrategy.getInstance());

        try {

            clientTransport.start();
            SocketConnectorHandler connectorHandler = 
                    TCPNIOConnectorHandler.builder(clientTransport)
                                          .processor(FilterChainBuilder.stateless().add(new TransportFilter()).build())
                                          .build();

            int successfulAttempts = 0;

            for (int i = 0; i < 10; i++) {
                try {
                    Future<Connection> futureConnection = connectorHandler.connect("localhost", PORT);
                    futureConnection.get(5, SECONDS);
                    
                    System.out.println("Successful connection (" + ++successfulAttempts + ").");
                } catch (Exception e2) {
                    e2.printStackTrace();
                    fail();
                }
            }

        } catch (Exception e) {
            fail("Unexpected Error: " + e.toString());
            e.printStackTrace();
        } finally {
            clientTransport.shutdownNow();
            transport.shutdownNow();
        }
    }

    // --------------------------------------------------------- Private Methods

    protected void doTestParallelWrites(int packetsNumber, int size, boolean blocking) throws Exception {
        Connection<?> connection = null;

        ExecutorService executorService = Executors.newCachedThreadPool();

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RandomDelayOnWriteFilter());
        filterChainBuilder.add(new StringFilter());
        filterChainBuilder.add(new ParallelWriteFilter(executorService, packetsNumber, size));

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());
        transport.configureBlocking(blocking);

        try {
            bindToPort(transport);

            FutureImpl<Boolean> clientFuture = SafeFutureImpl.create();
            FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter());
            clientFilterChainBuilder.add(new StringFilter());

            ClientCheckFilter clientTestFilter = new ClientCheckFilter(clientFuture, packetsNumber, size);

            clientFilterChainBuilder.add(clientTestFilter);

            SocketConnectorHandler connectorHandler = 
                    TCPNIOConnectorHandler.builder(transport)
                                          .processor(clientFilterChainBuilder.build())
                                          .build();

            Future<Connection> future = connectorHandler.connect("localhost", PORT);
            connection = future.get(10, SECONDS);
            assertTrue(connection != null);

            try {
                connection.write("start");
            } catch (Exception e) {
                logger.log(WARNING, "Error occurred when sending start command");
                throw e;
            }

            Boolean isDone = clientFuture.get(10, SECONDS);
            assertEquals(TRUE, isDone);
        } finally {
            try {
                executorService.shutdownNow();
            } catch (Exception ignored) {
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }

            try {
                transport.shutdownNow();
            } catch (Exception ignored) {
            }

        }
    }
    
    private static void bindToPort(TCPNIOTransport transport) throws Exception {
        logger.info("Binding to port " + PORT);
        try {
            transport.bind(PORT);
            transport.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
            throw e;
        }
        logger.info("Bound to port " + PORT);
    }

    // ---------------------------------------------------------- Nested Classes

    static class SameThreadIOStrategyInterruptWrapper implements IOStrategy {
        private final IOStrategy delegate = SameThreadIOStrategy.getInstance();
        private volatile boolean interruptedOnce = false;
        private final boolean interruptBefore;

        SameThreadIOStrategyInterruptWrapper(boolean interruptBefore) {
            this.interruptBefore = interruptBefore;
        }

        @Override
        public boolean executeIoEvent(Connection connection, IOEvent ioEvent) throws IOException {
            if (interruptBefore) {
                if (!interruptedOnce && ioEvent.equals(SERVER_ACCEPT)) {
                    Thread.currentThread().interrupt();
                    interruptedOnce = true;
                }
                
                return delegate.executeIoEvent(connection, ioEvent);
            } 
            
            boolean result = delegate.executeIoEvent(connection, ioEvent);
            if (ioEvent.equals(SERVER_ACCEPT)) {
                Thread.currentThread().interrupt();
            }
            
            return result;
        }

        @Override
        public boolean executeIoEvent(Connection connection, IOEvent ioEvent, boolean isIoEventEnabled) throws IOException {
            return delegate.executeIoEvent(connection, ioEvent, isIoEventEnabled);
        }

        @Override
        public Executor getThreadPoolFor(Connection connection, IOEvent ioEvent) {
            return delegate.getThreadPoolFor(connection, ioEvent);
        }

        @Override
        public ThreadPoolConfig createDefaultWorkerPoolConfig(Transport transport) {
            return delegate.createDefaultWorkerPoolConfig(transport);
        }
    }

    public static class CustomChannelDistributor extends AbstractNIOConnectionDistributor {

        private final AtomicInteger counter;

        public CustomChannelDistributor(final NIOTransport transport) {
            super(transport);
            counter = new AtomicInteger();
        }

        @Override
        public void registerChannel(SelectableChannel channel, int interestOps, Object attachment) throws IOException {
            transport.getSelectorHandler().registerChannel(getSelectorRunner(), channel, interestOps, attachment);
        }

        @Override
        public void registerChannelAsync(SelectableChannel channel, int interestOps, Object attachment, CompletionHandler<RegisterChannelResult> completionHandler) {
            transport.getSelectorHandler().registerChannelAsync(getSelectorRunner(), channel, interestOps, attachment, completionHandler);
        }

        @Override
        public void registerServiceChannelAsync(SelectableChannel channel, int interestOps, Object attachment, CompletionHandler<RegisterChannelResult> completionHandler) {
            transport.getSelectorHandler().registerChannelAsync(getSelectorRunner(), channel, interestOps, attachment, completionHandler);
        }

        private SelectorRunner getSelectorRunner() {
            SelectorRunner[] runners = getTransportSelectorRunners();
            int index = counter.getAndIncrement() % runners.length;

            return runners[index];
        }
    }
}
