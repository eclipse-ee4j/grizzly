/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.connectionpool;

import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.grizzly.utils.EchoFilter;
import org.glassfish.grizzly.utils.StringFilter;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * The simple echo server implementation,
 * which is used to test client-side connection pool.
 */
public class EchoServer {
    // the address to bind the server to
    private final SocketAddress endpointAddress;
    
    // internal transport
    private Transport transport;
    // true, if the server is running, or false otherwise
    private boolean isRunning;

    public EchoServer(SocketAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    /**
     * Returns the {@link SocketAddress} the server is bound to.
     */
    public SocketAddress getEndpointAddress() {
        return endpointAddress;
    }
    
    /**
     * Starts the server.
     */
    public void start() throws IOException {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        final FilterChain filterChain = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(new StringFilter(Charsets.UTF8_CHARSET))
                .add(new EchoFilter())
                .build();
        
        final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder.newInstance()
                .setProcessor(filterChain)
                .build();

        transport = tcpTransport;
        
        tcpTransport.bind(endpointAddress);
        tcpTransport.start();
    }
    
    /**
     * Stops the server.
     */
    public void stop() throws IOException {
        if (!isRunning) {
            return;
        }
        
        final Transport localTransport = transport;
        transport = null;

        localTransport.shutdownNow();
    }
}
