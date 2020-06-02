/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif;

import java.io.IOException;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.portunif.PUFilter;
import org.glassfish.grizzly.portunif.PUProtocol;
import org.glassfish.grizzly.portunif.ProtocolFinder;
import org.glassfish.grizzly.samples.portunif.addservice.AddProtocolFinder;
import org.glassfish.grizzly.samples.portunif.addservice.AddServerMessageFilter;
import org.glassfish.grizzly.samples.portunif.addservice.AddServiceFilter;
import org.glassfish.grizzly.samples.portunif.subservice.SubProtocolFinder;
import org.glassfish.grizzly.samples.portunif.subservice.SubServerMessageFilter;
import org.glassfish.grizzly.samples.portunif.subservice.SubServiceFilter;

/**
 * Port-unification sample, which hosts "add" and "sub" services on the same port. Sample creates a protocol tree:
 *
 * TransportFilter | PUFilter | ---------------------------- | | AddServerMessageFilter SubServerMessageFilter | |
 * AddServiceFilter SubServiceFilter
 *
 *
 * @author Alexey Stashok
 */
public class PUServer {
    static final int PORT = 17400;

    public static void main(String[] args) throws IOException {
        // Create PUFilter
        final PUFilter puFilter = new PUFilter();

        // Configure add-service PUProtocol
        final PUProtocol addProtocol = configureAddProtocol(puFilter);
        // Configure sub-service PUProtocol
        final PUProtocol subProtocol = configureSubProtocol(puFilter);

        // Register add-service pu protocol
        puFilter.register(addProtocol);
        // Register sub-service pu protocol
        puFilter.register(subProtocol);

        // Construct the main filter chain
        final FilterChainBuilder puFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter()).add(puFilter);

        // Build TCP transport
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(puFilterChainBuilder.build());

        try {
            // Bind to the server port
            transport.bind(PORT);
            // Start
            transport.start();

            Grizzly.logger(PUServer.class).info("Server is ready...\nPress enter to exit.");

            System.in.read();
        } finally {
            // Shutdown the TCP transport
            transport.shutdownNow();
        }
    }

    /**
     * Configure ADD-service {@link PUProtocol}.
     *
     * @param puFilter {@link PUFilter}
     * @return configured {@link PUProtocol}
     */
    static PUProtocol configureAddProtocol(final PUFilter puFilter) {
        // Create ADD-service ProtocolFinder
        final ProtocolFinder addProtocolFinder = new AddProtocolFinder();

        // Create ADD-service FilterChain
        final FilterChain addProtocolFilterChain = puFilter.getPUFilterChainBuilder()
                // Add ADD-service message parser/serializer
                .add(new AddServerMessageFilter())
                // Add ADD-service filter
                .add(new AddServiceFilter()).build();

        // Construct PUProtocol
        return new PUProtocol(addProtocolFinder, addProtocolFilterChain);
    }

    /**
     * Configure SUB-service {@link PUProtocol}.
     *
     * @param puFilter {@link PUFilter}.
     * @return configured {@link PUProtocol}
     */
    static PUProtocol configureSubProtocol(final PUFilter puFilter) {
        // Create SUB-service ProtocolFinder
        final ProtocolFinder subProtocolFinder = new SubProtocolFinder();

        // Create SUB-service FilterChain
        final FilterChain subProtocolFilterChain = puFilter.getPUFilterChainBuilder()
                // Add SUB-service message parser/serializer
                .add(new SubServerMessageFilter())
                // Add SUB-service filter
                .add(new SubServiceFilter()).build();

        // Construct PUProtocol
        return new PUProtocol(subProtocolFinder, subProtocolFilterChain);
    }

}
