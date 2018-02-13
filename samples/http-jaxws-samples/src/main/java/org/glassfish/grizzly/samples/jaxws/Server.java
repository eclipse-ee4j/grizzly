/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.jaxws;

import org.glassfish.grizzly.samples.jaxws.service.AddService;
import java.io.IOException;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.jaxws.JaxwsHandler;

/**
 * Basic Grizzly JAX-WS {@link HttpHandler} sample.
 * 
 * @author Alexey Stashok
 */
public class Server {
    public static final String WEB_SERVICE_CONTEXT_ROOT = "/add";
    private static final int PORT = 19881;
    
    public static void main(String[] args) throws IOException {
        
        // Create jax-ws HttpHandler and passing web service instance.
        final HttpHandler jaxwsHandler = new JaxwsHandler(new AddService());
        
        // Standard Grizzly HttpServer initialization
        final HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("jaxws-listener", "0.0.0.0", PORT);
        
        httpServer.getServerConfiguration().addHttpHandler(jaxwsHandler, WEB_SERVICE_CONTEXT_ROOT);
        httpServer.addListener(networkListener);
        
        // Start server
        httpServer.start();
        
        try {
            System.out.println("WSDL is available at http://localhost:" + PORT + WEB_SERVICE_CONTEXT_ROOT + "?wsdl");
            System.out.println("Press enter to stop the server...");
            System.in.read();
        } finally {
            httpServer.shutdownNow();
        }
        
    }
}
