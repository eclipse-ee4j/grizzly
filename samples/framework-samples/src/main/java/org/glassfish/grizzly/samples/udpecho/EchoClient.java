/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.udpecho;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import org.glassfish.grizzly.utils.StringFilter;

/**
 * The simple client, which sends a message to the echo server
 * and waits for response
 * @author Alexey Stashok
 */
public class EchoClient {
    private static final Logger logger = Logger.getLogger(EchoClient.class.getName());

    public static void main(String[] args) throws IOException,
            ExecutionException, InterruptedException, TimeoutException {

        final FutureImpl<Boolean> future = SafeFutureImpl.create();

        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which will be responsible for reading and
        // writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        // Add string filter, which will transform Buffer <-> String
        filterChainBuilder.add(new StringFilter(Charset.forName("UTF-8")));
        // Add the client filter, responsible for the client logic
        filterChainBuilder.add(new ClientFilter("Echo test", future));

        // Create the UDP transport
        final UDPNIOTransport transport =
                UDPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            // start the transport
            transport.start();

            // perform async. connect to the server
            transport.connect(EchoServer.HOST,
                    EchoServer.PORT);
            // wait for connect operation to complete

            // check the result
            final boolean isEqual = future.get(10, TimeUnit.SECONDS);
            assert isEqual;
            logger.info("Echo came successfully");
        } finally {
            // stop the transport
            transport.shutdownNow();
        }
    }

    /**
     * ClientFilter, which sends a message, when UDP connection gets bound to the target address,
     * and checks the server echo.
     */
    static class ClientFilter extends BaseFilter {
        // initial message to be sent to the server
        private final String message;
        // the resulting future
        private final FutureImpl<Boolean> future;

        private ClientFilter(String message, FutureImpl<Boolean> future) {
            this.message = message;
            this.future = future;
        }

        /**
         * Method is called, when UDP connection is getting bound to the server address.
         * 
         * @param ctx the {@link FilterChainContext}.
         * @return
         * @throws IOException
         */
        @Override
        public NextAction handleConnect(final FilterChainContext ctx) throws IOException {
            // We have StringFilter down on the filterchain - so we can write String directly
            ctx.write(message);
            return ctx.getInvokeAction();
        }

        /**
         * Method is called, when UDP message came from the server.
         *
         * @param ctx the {@link FilterChainContext}.
         * @return
         * @throws IOException
         */
        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            // We have StringFilter down on the filterchain - so we can get String directly
            final String messageFromServer = ctx.getMessage();

            // check the echo
            future.result(message.equals(messageFromServer));
            return ctx.getInvokeAction();
        }

    }
}
