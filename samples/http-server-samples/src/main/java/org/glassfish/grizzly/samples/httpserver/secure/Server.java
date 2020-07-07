/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.httpserver.secure;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * Secured standalone Java HTTP server.
 */
public class Server {
    private static final Logger LOGGER = Grizzly.logger(Server.class);

    public static void main(String[] args) {
        final HttpServer server = new HttpServer();
        final ServerConfiguration config = server.getServerConfiguration();

        // Register simple HttpHandler
        config.addHttpHandler(new SimpleHttpHandler(), "/");

        // create a network listener that listens on port 8080.
        final NetworkListener networkListener = new NetworkListener("secured-listener", NetworkListener.DEFAULT_NETWORK_HOST,
                NetworkListener.DEFAULT_NETWORK_PORT);

        // Enable SSL on the listener
        networkListener.setSecure(true);
        networkListener.setSSLEngineConfig(createSslConfiguration());

        server.addListener(networkListener);
        try {
            // Start the server
            server.start();
            System.out.println("The secured server is running.\nhttps://localhost:" + NetworkListener.DEFAULT_NETWORK_PORT + "\nPress enter to stop...");
            System.in.read();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        } finally {
            server.shutdownNow();
        }
    }

    /**
     * Initialize server side SSL configuration.
     *
     * @return server side {@link SSLEngineConfigurator}.
     */
    private static SSLEngineConfigurator createSslConfiguration() {
        // Initialize SSLContext configuration
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();

        ClassLoader cl = Server.class.getClassLoader();
        // Set key store
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        if (keystoreUrl != null) {
            sslContextConfig.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfig.setKeyStorePass("changeit");
        }

        // Create SSLEngine configurator
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(), false, false, false);
    }
}
