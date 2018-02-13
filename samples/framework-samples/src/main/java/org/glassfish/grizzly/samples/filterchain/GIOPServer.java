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

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.samples.echo.EchoFilter;

/**
 * Simple GIOP echo server
 * 
 * @author Alexey Stashok
 */
public class GIOPServer {
    public static final String HOST = "localhost";
    public static final int PORT = 9098;
    
    public static void main(String[] args) throws Exception {
        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add filters to the chain
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new GIOPFilter());
        filterChainBuilder.add(new EchoFilter());


        // Create TCP NIO transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            // Bind server socket and start transport
            transport.bind(PORT);
            transport.start();

            System.out.println("Press <enter> to exit...");
            System.in.read();
        } finally {
            transport.shutdownNow();
        }
    }
}
