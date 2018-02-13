/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.http.download;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Simple HTTP (Web) server, which listens on a specific TCP port and shares
 * static resources (files), located in a passed folder.
 * 
 * @author Alexey Stashok
 */
public class Server {
    private static final Logger logger = Grizzly.logger(Server.class);

    // TCP Host
    public static final String HOST = "localhost";
    // TCP port
    public static final int PORT = 7777;

    public static void main(String[] args) throws IOException {

        // lifecycle of the executor used by the IdleTimeoutFilter must be explicitly
        // managed
        final DelayedExecutor timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor();
        timeoutExecutor.start();

        // Construct filter chain
        FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless();
        // Add transport filter
        serverFilterChainBuilder.add(new TransportFilter());
        // Add IdleTimeoutFilter, which will close connetions, which stay
        // idle longer than 10 seconds.
        serverFilterChainBuilder.add(new IdleTimeoutFilter(timeoutExecutor, 10, TimeUnit.SECONDS));
        // Add HttpServerFilter, which transforms Buffer <-> HttpContent
        serverFilterChainBuilder.add(new HttpServerFilter());
        // Simple server implementation, which locates a resource in a local file system
        // and transfers it via HTTP
        serverFilterChainBuilder.add(new WebServerFilter("."));

        // Initialize Transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        // Set filterchain as a Transport Processor
        transport.setProcessor(serverFilterChainBuilder.build());

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
            timeoutExecutor.stop();
            timeoutExecutor.destroy();
            logger.info("Stopped transport...");
        }
    }
}
