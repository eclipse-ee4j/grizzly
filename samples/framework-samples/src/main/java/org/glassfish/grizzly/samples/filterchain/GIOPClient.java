/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.filterchain;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Simple GIOP client
 * 
 * @author Alexey Stashok
 */
public class GIOPClient {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Connection connection = null;

        final FutureImpl<GIOPMessage> resultMessageFuture = SafeFutureImpl.create();

        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new GIOPFilter());
        filterChainBuilder.add(new CustomClientFilter(resultMessageFuture));

        // Create TCP NIO transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            // start transport
            transport.start();

            // Connect client to the GIOP server
            Future<Connection> future = transport.connect(GIOPServer.HOST,
                    GIOPServer.PORT);

            connection = future.get(10, TimeUnit.SECONDS);

            // Initialize sample GIOP message
            byte[] testMessage = "GIOP test".getBytes(Charsets.ASCII_CHARSET);
            GIOPMessage sentMessage = new GIOPMessage((byte) 1, (byte) 2,
                    (byte) 0x0F, (byte) 0, testMessage);

            connection.write(sentMessage);

            final GIOPMessage rcvMessage = resultMessageFuture.get(10, TimeUnit.SECONDS);

            // Check if echo returned message equal to original one
            if (sentMessage.equals(rcvMessage)) {
                System.out.println("DONE!");
            } else {
                System.out.println("Messages are not equal!");
            }

        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    public static final class CustomClientFilter extends BaseFilter {
        private final FutureImpl<GIOPMessage> resultFuture;

        public CustomClientFilter(FutureImpl<GIOPMessage> resultFuture) {
            this.resultFuture = resultFuture;
}

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            final GIOPMessage message = ctx.getMessage();
            resultFuture.result(message);

            return ctx.getStopAction();
        }
    }
}
