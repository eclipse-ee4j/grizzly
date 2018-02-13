/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.lifecycle;

import java.io.IOException;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.samples.echo.EchoFilter;

/**
 * An example, how connections lifecycle could be controlled using Grizzly 2.0
 *
 * @author Alexey Stashok
 */
public class LifeCycleExample {
    public static final String HOST = "localhost";
    public static final int PORT = 7777;

    public static void main(String[] args) throws IOException {
        LifeCycleFilter lifeCycleFilter = new LifeCycleFilter();

        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        // Add lifecycle filter to track the connections
        filterChainBuilder.add(lifeCycleFilter);
        // Add echo filter
        filterChainBuilder.add(new EchoFilter());

        // Create TCP transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            // binding transport to start listen on certain host and port
            transport.bind(HOST, PORT);

            // start the transport
            transport.start();
            System.out.println("Press 'q and ENTER' to exit, or just ENTER to see statistics...");

            do {
                printStats(lifeCycleFilter);
            } while (System.in.read() != 'q');
        } finally {
            // stop the transport
            transport.shutdownNow();
        }
    }

    /**
     * Print the lifecycle statistics
     *
     * @param lifeCycleFilter the {@link LifeCycleFilter}
     */
    private static void printStats(LifeCycleFilter lifeCycleFilter) {
        System.out.println("The total number of connections ever connected: " +
                lifeCycleFilter.getTotalConnections());
        System.out.println("The number of active connections: " +
                lifeCycleFilter.getActiveConnections().size());
    }
}
