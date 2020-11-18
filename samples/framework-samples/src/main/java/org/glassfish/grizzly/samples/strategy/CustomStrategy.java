/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.strategy;

import java.io.IOException;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.samples.echo.EchoFilter;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;

/**
 * Sample shows how easy custom {@link org.glassfish.grizzly.IOStrategy} could be applied for a
 * {@link org.glassfish.grizzly.Transport}. In this example we use
 * {@link org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy} for processing all I/O events occurring on
 * {@link org.glassfish.grizzly.Connection}.
 *
 * To test this echo server you can use {@link org.glassfish.grizzly.samples.echo.EchoClient}.
 *
 * @see org.glassfish.grizzly.IOStrategy
 * @see org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy
 * @see org.glassfish.grizzly.strategies.SameThreadIOStrategy
 * @see org.glassfish.grizzly.strategies.WorkerThreadIOStrategy
 * @see org.glassfish.grizzly.strategies.SimpleDynamicNIOStrategy
 *
 * @author Alexey Stashok
 */
public class CustomStrategy {
    public static final String HOST = "localhost";
    public static final int PORT = 7777;

    public static void main(String[] args) throws IOException {
        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new EchoFilter());

        // Create TCP transport
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        // Set the LeaderFollowerIOStrategy (any strategy could be applied this way)
        transport.setIOStrategy(LeaderFollowerNIOStrategy.getInstance());

        try {
            // binding transport to start listen on certain host and port
            transport.bind(HOST, PORT);

            // start the transport
            transport.start();

            System.out.println("Press any key to stop the server...");
            System.in.read();
        } finally {
            System.out.println("Stopping transport...");
            // stop the transport
            transport.shutdownNow();

            System.out.println("Stopped transport...");
        }
    }
}
