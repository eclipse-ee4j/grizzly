/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.echo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.StringFilter;

/**
 * Class initializes and starts the echo server, based on Grizzly 2.0
 *
 * @author Alexey Stashok
 */
public class EchoServer {
    private static final Logger logger = Logger.getLogger(EchoServer.class.getName());

    public static final String HOST = "localhost";
    public static final int PORT = 7777;

    public static void main(String[] args) throws IOException {
        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();

        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());

        // StringFilter is responsible for Buffer <-> String conversion
        filterChainBuilder.add(new StringFilter(Charset.forName("UTF-8")));

        // EchoFilter is responsible for echoing received messages
        filterChainBuilder.add(new EchoFilter());

        // Create TCP transport
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

        transport.setProcessor(filterChainBuilder.build());

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
