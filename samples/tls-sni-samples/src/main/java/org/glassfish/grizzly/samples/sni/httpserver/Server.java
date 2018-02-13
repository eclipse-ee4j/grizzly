/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.sni.httpserver;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.sni.SNIServerConfigResolver;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * SNI-aware standalone Java HTTP server.
 * The server could be reached via SNI hosts: *.foo.com and *.bar.com.
 * 
 * One of the ways to test the server is redirecting foo.com and bar.com
 * host names to IP address 127.0.0.1 (localhost) in /etc/hosts file:
 * 
 * <pre>
 *      127.0.0.1       foo.com, bar.com
 * </pre>
 * 
 * Then type https://foo.com:8080 or https://bar.com:8080 in the web browser
 * and make sure the server uses different certificates for these hosts.
 * 
 * If you try to reach the server using different host name (like localhost) -
 * the connection will be immediately terminated.
 */
public class Server {
    private static final Logger LOGGER = Grizzly.logger(Server.class);
    
    public static void main(String[] args) {
        final HttpServer server = new HttpServer();
        final ServerConfiguration config = server.getServerConfiguration();

        // Register simple HttpHandler
        config.addHttpHandler(new SimpleHttpHandler(), "/");
        
        // create a network listener that listens on port 8080.
        final NetworkListener networkListener = new NetworkListener(
                "secured-listener",
                NetworkListener.DEFAULT_NETWORK_HOST,
                NetworkListener.DEFAULT_NETWORK_PORT);
        
        // Enable SSL on the listener
        networkListener.setSecure(true);

        // Create an SNI resolver for two hosts: foo.com and bar.com
        final SNIServerConfigResolver resolver = new FooBarSNIResolver(
                createSslConfiguration("foo.jks"),
                createSslConfiguration("bar.jks"));
        
        // Register SNI AddOn to replace standard SSLBaseFilter with an SNIFilter
        networkListener.registerAddOn(new SNIAddOn(resolver));
        
        server.addListener(networkListener);
        try {
            // Start the server
            server.start();
            System.out.println("The SNI-aware server is running on port " + NetworkListener.DEFAULT_NETWORK_PORT + "\nPress enter to stop...");
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
    private static SSLEngineConfigurator createSslConfiguration(final String keyStoreName) {
        // Initialize SSLContext configuration
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();

        ClassLoader cl = Server.class.getClassLoader();
        // Set key store
        URL keystoreUrl = cl.getResource(keyStoreName);
        if (keystoreUrl != null) {
            sslContextConfig.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfig.setKeyStorePass("changeit");
        }


        // Create SSLEngine configurator
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(),
                false, false, false);
    }}
