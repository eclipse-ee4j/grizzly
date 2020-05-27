/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.ssl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.samples.echo.EchoFilter;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.utils.StringFilter;

/**
 * Class initializes and starts the SSL echo server, based on Grizzly 2.0 We use the {@link EchoFilter} from echo
 * example.
 *
 * @see SSLFilter
 * @see SSLContextConfigurator
 * @see SSLEngineConfigurator
 *
 * @author Alexey Stashok
 */
public class SSLEchoServer {
    public static final String HOST = "localhost";
    public static final int PORT = 7777;

    public static void main(String[] args) throws IOException {
        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());

        // Initialize and add SSLFilter
        final SSLEngineConfigurator serverConfig = initializeSSL();
        final SSLEngineConfigurator clientConfig = serverConfig.copy().setClientMode(true);

        filterChainBuilder.add(new SSLFilter(serverConfig, clientConfig));

        // Add StringFilter, which will be responsible for Buffer <-> String transformation
        filterChainBuilder.add(new StringFilter(Charset.forName("UTF-8")));

        // Use the plain EchoFilter
        filterChainBuilder.add(new EchoFilter());

        // Create TCP transport
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

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

    /**
     * Initialize server side SSL configuration.
     *
     * @return server side {@link SSLEngineConfigurator}.
     */
    private static SSLEngineConfigurator initializeSSL() {
        // Initialize SSLContext configuration
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();

        // Set key store
        ClassLoader cl = SSLEchoServer.class.getClassLoader();
        URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        if (cacertsUrl != null) {
            sslContextConfig.setTrustStoreFile(cacertsUrl.getFile());
            sslContextConfig.setTrustStorePass("changeit");
        }

        // Set trust store
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        if (keystoreUrl != null) {
            sslContextConfig.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfig.setKeyStorePass("changeit");
        }

        // Create SSLEngine configurator
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(), false, false, false);
    }
}
