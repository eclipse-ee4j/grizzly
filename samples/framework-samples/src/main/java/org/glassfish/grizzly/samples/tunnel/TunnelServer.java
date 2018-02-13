/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.tunnel;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Simple tunneling server
 *
 * @author Alexey Stashok
 */
public class TunnelServer {
    private static final Logger logger = Logger.getLogger(TunnelServer.class.getName());

    public static final String HOST = "localhost";
    public static final int PORT = 7777;

    public static final String REDIRECT_HOST = "localhost";
    public static final int REDIRECT_PORT = 5001;

    public static void main(String[] args) throws IOException {
        // Create TCP transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();

        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new TunnelFilter(
                TCPNIOConnectorHandler.builder(transport).build(),
                REDIRECT_HOST, REDIRECT_PORT));
        
        transport.setProcessor(filterChainBuilder.build());
        
        // Set async write queue size limit
        transport.getAsyncQueueIO().getWriter().setMaxPendingBytesPerConnection(256 * 1024);

        try {
            // binding transport to start listen on certain host and port
            transport.bind(HOST, PORT);

            // start the transport
            transport.start();

            logger.info("Press any key to stop the server...");
            System.in.read();
        } finally {
            logger.info("Stopping transport...");
            // stop the transport
            transport.shutdownNow();

            logger.info("Stopped transport...");
        }
    }
}
