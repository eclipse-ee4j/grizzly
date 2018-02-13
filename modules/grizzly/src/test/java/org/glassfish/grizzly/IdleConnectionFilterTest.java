/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;

/**
 * Test {@link IdleTimeoutFilter}
 * 
 * @author Alexey Stashok
 */
public class IdleConnectionFilterTest extends GrizzlyTestCase {
    public static final int PORT = 7782;

    public void testAcceptedConnectionIdleTimeout() throws Exception {
        Connection connection = null;

        final CountDownLatch latch = new CountDownLatch(1);
        final DelayedExecutor timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor();
        timeoutExecutor.start();
        IdleTimeoutFilter idleTimeoutFilter =
                new IdleTimeoutFilter(timeoutExecutor, 2, TimeUnit.SECONDS);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(idleTimeoutFilter);
        filterChainBuilder.add(new BaseFilter() {
                private volatile Connection acceptedConnection;
                @Override
                public NextAction handleAccept(FilterChainContext ctx)
                        throws IOException {
                    acceptedConnection = ctx.getConnection();
                    return ctx.getInvokeAction();
                }

                @Override
                public NextAction handleClose(FilterChainContext ctx)
                        throws IOException {
                    if (ctx.getConnection().equals(acceptedConnection)) {
                        latch.countDown();
                    }

                    return ctx.getInvokeAction();
                }

            });

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());
        
        try {
            transport.bind(PORT);
            transport.start();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }
            timeoutExecutor.stop();
            timeoutExecutor.destroy();
            transport.shutdownNow();
        }
    }

    public void testConnectedConnectionIdleTimeout() throws Exception {
        Connection connection = null;
        final CountDownLatch latch = new CountDownLatch(1);

        final DelayedExecutor timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor();
        timeoutExecutor.start();
        IdleTimeoutFilter idleTimeoutFilter =
                new IdleTimeoutFilter(timeoutExecutor, 2, TimeUnit.SECONDS);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(idleTimeoutFilter);
        filterChainBuilder.add(new BaseFilter() {
            private volatile Connection connectedConnection;

            @Override
            public NextAction handleConnect(FilterChainContext ctx)
                    throws IOException {
                connectedConnection = ctx.getConnection();
                return ctx.getInvokeAction();
            }

            @Override
            public NextAction handleClose(FilterChainContext ctx)
                    throws IOException {
                if (ctx.getConnection().equals(connectedConnection)) {
                    latch.countDown();
                }

                return ctx.getInvokeAction();
            }
        });
        
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());
        
        try {
            transport.bind(PORT);

            transport.start();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }
            timeoutExecutor.stop();
            timeoutExecutor.destroy();
            transport.shutdownNow();
        }
    }
    
    public void testInfiniteIdleTimeout() throws Exception {
        Connection connection = null;

        final CountDownLatch latch = new CountDownLatch(1);
        final DelayedExecutor timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor();
        timeoutExecutor.start();
        IdleTimeoutFilter idleTimeoutFilter =
                new IdleTimeoutFilter(timeoutExecutor, -1, TimeUnit.SECONDS);

        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(idleTimeoutFilter);
        filterChainBuilder.add(new BaseFilter() {
                private volatile Connection acceptedConnection;
                @Override
                public NextAction handleAccept(FilterChainContext ctx)
                        throws IOException {
                    acceptedConnection = ctx.getConnection();
                    return ctx.getInvokeAction();
                }

                @Override
                public NextAction handleClose(FilterChainContext ctx)
                        throws IOException {
                    if (ctx.getConnection().equals(acceptedConnection)) {
                        latch.countDown();
                    }

                    return ctx.getInvokeAction();
                }

            });

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());
        
        try {
            transport.bind(PORT);
            transport.start();

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            assertFalse(latch.await(2, TimeUnit.SECONDS));
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }
            timeoutExecutor.stop();
            timeoutExecutor.destroy();
            transport.shutdownNow();
        }
    }
}
