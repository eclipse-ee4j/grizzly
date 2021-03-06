/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.connectionpool;

import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@link MultiEndpointPool} tests.
 * 
 * @author Alexey Stashok
 */
public class MultiEndPointPoolTest {
    private static int PORT = PORT();
    private static int NUMBER_OF_PORTS_TO_BIND = 3;

    static int PORT() {
        try {
            int port = 18334 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<Connection<?>> serverSideConnections = newSetFromMap(new ConcurrentHashMap<>());

    private TCPNIOTransport transport;

    @Before
    public void init() throws Exception {
        FilterChain filterChain = 
            FilterChainBuilder.stateless()
                              .add(new TransportFilter())
                              .add(new BaseFilter() {

            @Override
            public NextAction handleAccept(FilterChainContext ctx) throws IOException {
                serverSideConnections.add(ctx.getConnection());
                return ctx.getStopAction();
            }

            @Override
            public NextAction handleClose(FilterChainContext ctx) throws IOException {
                serverSideConnections.remove(ctx.getConnection());
                return ctx.getStopAction();
            }
        }).build();

        transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChain);

        for (int i = 0; i < NUMBER_OF_PORTS_TO_BIND; i++) {
            Thread.sleep(10);
            transport.bind(PORT + i);
        }

        Thread.sleep(10);
        transport.start();
    }

    @After
    public void tearDown() throws IOException {
        serverSideConnections.clear();

        if (transport != null) {
            transport.shutdownNow();
        }
    }

    @Test
    public void testLocalAddress() throws Exception {
        InetSocketAddress localAddress = new InetSocketAddress("localhost", 60000);
        MultiEndpointPool<SocketAddress> pool =
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(3).maxConnectionsTotal(15)
                             .keepAliveTimeout(-1, SECONDS)
                             .build();
        
        Endpoint<SocketAddress> key1 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), localAddress, transport);
        try {
            Connection<?> c1 = pool.take(key1).get();
            assertEquals(localAddress, c1.getLocalAddress());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testBasicPollRelease() throws Exception {
        MultiEndpointPool<SocketAddress> pool = 
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(3)
                             .maxConnectionsTotal(15)
                             .keepAliveTimeout(-1, SECONDS).build();

        try {

            Endpoint<SocketAddress> key1 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), transport);
            Endpoint<SocketAddress> key2 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT + 1), transport);

            Connection<?> c11 = pool.take(key1).get();
            assertNotNull(c11);
            assertEquals(1, pool.size());
            
            Connection<?> c12 = pool.take(key1).get();
            assertNotNull(c12);
            assertEquals(2, pool.size());

            Connection<?> c21 = pool.take(key2).get();
            assertNotNull(c21);
            assertEquals(3, pool.size());
            
            Connection<?> c22 = pool.take(key2).get();
            assertNotNull(c22);
            assertEquals(4, pool.size());

            assertTrue(pool.release(c11));
            assertEquals(4, pool.size());

            assertTrue(pool.release(c21));
            assertEquals(4, pool.size());

            c11 = pool.take(key1).get();
            assertNotNull(c11);
            assertEquals(4, pool.size());

            assertTrue(pool.detach(c11));
            assertEquals(3, pool.size());

            assertTrue(pool.attach(key1, c11));
            assertEquals(4, pool.size());

            assertTrue(pool.release(c11));

            assertEquals(4, pool.size());

            c11 = pool.take(key1).get();
            assertNotNull(c11);

            c21 = pool.take(key2).get();
            assertNotNull(c21);

            c11.close().get(10, SECONDS);
            assertEquals(3, pool.size());

            c12.close().get(10, SECONDS);
            assertEquals(2, pool.size());

            c21.close().get(10, SECONDS);
            assertEquals(1, pool.size());

            c22.close().get(10, SECONDS);
            assertEquals(0, pool.size());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testTotalPoolSizeLimit() throws Exception {
        MultiEndpointPool<SocketAddress> pool =
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(2)
                             .maxConnectionsTotal(2)
                             .keepAliveTimeout(-1, SECONDS)
                             .build();

        try {
            Endpoint<SocketAddress> key1 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), transport);
            Endpoint<SocketAddress> key2 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT + 1), transport);

            Connection<?> c11 = pool.take(key1).get();
            assertNotNull(c11);
            assertEquals(1, pool.size());
            
            Connection<?> c12 = pool.take(key1).get();
            assertNotNull(c12);
            assertEquals(2, pool.size());

            @SuppressWarnings("rawtypes")
            GrizzlyFuture<Connection> c21Future = pool.take(key2);
            try {
                c21Future.get(2, SECONDS);
                fail("TimeoutException had to be thrown");
            } catch (TimeoutException e) {
            }

            assertTrue(c21Future.cancel(false));
            assertEquals(2, pool.size());

            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }

                    c12.closeSilently();
                }
            };
            t.start();

            Connection<?> c21 = pool.take(key2).get(10, SECONDS);
            assertNotNull(c12);
            assertEquals(2, pool.size());

            c11.close().get(10, SECONDS);
            assertEquals(1, pool.size());

            c21.close().get(10, SECONDS);
            assertEquals(0, pool.size());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testSingleEndpointClose() throws Exception {
        int maxConnectionsPerEndpoint = 4;

        MultiEndpointPool<SocketAddress> pool = 
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(maxConnectionsPerEndpoint)
                             .maxConnectionsTotal(maxConnectionsPerEndpoint * 2)
                             .keepAliveTimeout(-1, SECONDS)
                             .build();

        try {
            Endpoint<SocketAddress> key1 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), transport);
            Endpoint<SocketAddress> key2 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT + 1), transport);

            Connection<?>[] e1Connections = new Connection[maxConnectionsPerEndpoint];
            Connection<?>[] e2Connections = new Connection[maxConnectionsPerEndpoint];

            for (int i = 0; i < maxConnectionsPerEndpoint; i++) {
                e1Connections[i] = pool.take(key1).get();
                assertNotNull(e1Connections[i]);
                assertEquals((i * 2) + 1, pool.size());

                e2Connections[i] = pool.take(key2).get();
                assertNotNull(e2Connections[i]);
                assertEquals((i * 2) + 2, pool.size());
            }

            int numberOfReleasedConnections = maxConnectionsPerEndpoint / 2;
            for (int i = 0; i < numberOfReleasedConnections; i++) {
                pool.release(e1Connections[i]);
                assertNotNull(e1Connections[i]);
            }

            pool.close(key1);
            assertEquals(maxConnectionsPerEndpoint, pool.size());

            for (int i = 0; i < numberOfReleasedConnections; i++) {
                assertFalse(e1Connections[i].isOpen());
            }

            for (int i = numberOfReleasedConnections; i < maxConnectionsPerEndpoint; i++) {
                assertTrue(e1Connections[i].isOpen());
            }

            for (int i = numberOfReleasedConnections; i < maxConnectionsPerEndpoint; i++) {
                pool.release(e1Connections[i]);
            }

            for (int i = numberOfReleasedConnections; i < maxConnectionsPerEndpoint; i++) {
                assertFalse(e1Connections[i].isOpen());
            }
        } finally {
            pool.close();
        }
    }

    @Test
    public void testEmbeddedPollTimeout() throws Exception {
        int maxConnectionsPerEndpoint = 2;

        MultiEndpointPool<SocketAddress> pool =
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(maxConnectionsPerEndpoint)
                             .maxConnectionsTotal(maxConnectionsPerEndpoint * 2)
                             .keepAliveTimeout(-1, SECONDS)
                             .asyncPollTimeout(2, SECONDS)
                             .build();

        try {
            Endpoint<SocketAddress> key = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), transport);

            Connection<?> c1 = pool.take(key).get();
            assertNotNull(c1);
            assertEquals(1, pool.size());

            Connection<?> c2 = pool.take(key).get();
            assertNotNull(c2);
            assertEquals(2, pool.size());

            @SuppressWarnings("rawtypes")
            GrizzlyFuture<Connection> c3Future = pool.take(key);
            try {
                c3Future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                assertTrue("Unexpected exception " + cause, cause instanceof TimeoutException);
            } catch (Throwable e) {
                fail("Unexpected exception " + e);
            }

            assertFalse(c3Future.cancel(false));

            assertEquals(2, pool.size());

            pool.release(c2);

            Connection<?> c3 = pool.take(key).get(2, SECONDS);
            assertNotNull(c3);
            assertEquals(2, pool.size());
        } finally {
            pool.close();
        }
    }

    @Test
    public void testEndpointPoolCustomizer() throws Exception {
        Endpoint<SocketAddress> key1 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT), transport);
        Endpoint<SocketAddress> key2 = Endpoint.Factory.create(new InetSocketAddress("localhost", PORT + 1), transport);

        int maxConnectionsPerEndpoint = 2;

        MultiEndpointPool<SocketAddress> pool = 
            MultiEndpointPool.builder(SocketAddress.class)
                             .maxConnectionsPerEndpoint(maxConnectionsPerEndpoint)
                             .maxConnectionsTotal(maxConnectionsPerEndpoint * 2)
                             .keepAliveTimeout(-1, SECONDS)
                             .endpointPoolCustomizer(new MultiEndpointPool.EndpointPoolCustomizer<SocketAddress>() {

                                @Override
                                public void customize(Endpoint<SocketAddress> endpoint, MultiEndpointPool.EndpointPoolBuilder<SocketAddress> builder) {
                                    if (endpoint.equals(key1)) {
                                        builder.keepAliveTimeout(0, SECONDS); // no pooling
                                    }
                                }
                             }).build();

        try {
            Connection<?> c1 = pool.take(key1).get();
            assertNotNull(c1);
            assertEquals(1, pool.size());

            Connection<?> c2 = pool.take(key2).get();
            assertNotNull(c2);
            assertEquals(2, pool.size());

            pool.release(c2);
            assertEquals(2, pool.size());
            assertEquals(2, pool.getOpenConnectionsCount());

            // c1 should be closed immediately
            pool.release(c1);
            assertEquals(1, pool.size());
            assertEquals(1, pool.getOpenConnectionsCount());
        } finally {
            pool.close();
        }
    }
}
