/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.httpmultipart;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

/**
 * HTTP upload server, which instantiates two Grizzly {@link org.glassfish.grizzly.http.server.HttpHandler}s:
 * {@link FormHttpHandler} on URL http://localhost:18080/,
 * {@link UploaderHttpHandler} on URIL http://localhost:18080/upload.
 * First one is responsible to serve simple HTML upload form and the second one
 * takes care of actual file/data uploading.
 *
 * @author Alexey Stashok
 */
public class UploadServer {
    private static final Logger LOGGER = Grizzly.logger(UploadServer.class);

    private static final int PORT = 18080;

    public static void main(String[] args) {
        // create a HttpServer
        final HttpServer server = new HttpServer();        
        final ServerConfiguration config = server.getServerConfiguration();

        // Map the path / to the FormHttpHandler
        config.addHttpHandler(new FormHttpHandler(), "/");
        // Map the path /upload to the UploaderHttpHandler
        config.addHttpHandler(new UploaderHttpHandler(), "/upload");

        // Create HTTP network listener on host "0.0.0.0" and port 18080.
        final NetworkListener listener = new NetworkListener("Grizzly",
                NetworkListener.DEFAULT_NETWORK_HOST, PORT);

        server.addListener(listener);

        try {
            // Start the server
            server.start();

            LOGGER.log(Level.INFO, "Server listens on port {0}", PORT);
            LOGGER.log(Level.INFO, "Press enter to exit");
            System.in.read();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        } finally {
            // Stop the server
            server.shutdownNow();
        }
    }
}
