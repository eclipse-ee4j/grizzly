/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.nio.transport.UDPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link UDPNIOTransport}
 *
 * @author Alexey Stashok
 */
public class UDPNIOTransportTest {
    public static final int PORT = 7777;

    @Before
    public void setUp() throws Exception {
        ByteBufferWrapper.DEBUG_MODE = true;
    }

    @Test
    public void testConnectFutureCancel() throws Exception {
        UDPNIOTransport transport = UDPNIOTransportBuilder.newInstance().build();

        final AtomicInteger connectCounter = new AtomicInteger();
        final AtomicInteger closeCounter = new AtomicInteger();

        FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter());

        FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter()).add(new BaseFilter() {
            @Override
            public NextAction handleConnect(FilterChainContext ctx) throws IOException {
                connectCounter.incrementAndGet();
                return ctx.getInvokeAction();
            }

            @Override
            public NextAction handleClose(FilterChainContext ctx) throws IOException {
                closeCounter.incrementAndGet();
                return ctx.getInvokeAction();
            }
        });

        transport.setProcessor(serverFilterChainBuilder.build());

        SocketConnectorHandler connectorHandler = UDPNIOConnectorHandler.builder(transport).processor(clientFilterChainBuilder.build()).build();

        try {
            transport.bind(PORT);
            transport.start();

            final int connectionsNum = 100;

            for (int i = 0; i < connectionsNum; i++) {
                final Future<Connection> connectFuture = connectorHandler.connect(new InetSocketAddress("localhost", PORT));
                if (!connectFuture.cancel(false)) {
                    assertTrue("Future is not done", connectFuture.isDone());
                    final Connection c = connectFuture.get();
                    assertNotNull("Connection is null?", c);
                    assertTrue("Connection is not connected", c.isOpen());
                    c.closeSilently();
                }
            }

            Thread.sleep(50);

            assertEquals("Number of connected and closed connections doesn't match", connectCounter.get(), closeCounter.get());
        } finally {
            transport.shutdownNow();
        }
    }
}
