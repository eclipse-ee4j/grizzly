/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.ajp;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.ajp.AjpAddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;

/**
 * Sample demonstrates how custom {@link HttpHandler}, rigistered on
 * Grizzly {@link HttpServer} may transparently process both HTTP
 * and AJP requests.
 *
 * (please check the readme.txt for apache configuration instructions).
 * 
 * @author Alexey Stashok
 */
public class AjpHelloWorld {
    private static final Logger LOGGER = Grizzly.logger(AjpHelloWorld.class);


    public static void main(String[] args) {

        // create a HTTP server instance
        final HttpServer server = new HttpServer();

        // Create plain HTTP listener, which will handle port 8080
        final NetworkListener httpNetworkListener =
                new NetworkListener("http-listener", "0.0.0.0", 8080);

        // Create AJP listener, which will handle port 8009
        final NetworkListener ajpNetworkListener =
                new NetworkListener("ajp-listener", "0.0.0.0", 8009);
        // Register AJP addon on HttpServer's listener
        ajpNetworkListener.registerAddOn(new AjpAddOn());

        server.addListener(httpNetworkListener);
        server.addListener(ajpNetworkListener);

        final ServerConfiguration config = server.getServerConfiguration();

        // Map the path, /grizzly, to the HelloWorldHandler
        config.addHttpHandler(new HelloWorldHandler(), "/grizzly");

        try {
            // start the server
            server.start();

            // So now we're listening on 2 ports:
            // 8080 : for HTTP requests
            // 8009 : for AJP
            // both ports redirect requests to HelloWorldHandler

            System.out.println("Press enter to stop...");
            System.in.read();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        } finally {
            server.shutdownNow();
        }
    }

    // Simple "Hello World" {@link HttpHandler}.
    public static class HelloWorldHandler extends HttpHandler {

        @Override
        public void service(final Request request, final Response response)
                throws Exception {
            // Here we don't care if it's AJP or HTTP originated request
            // everything is transparent
            final Writer writer = response.getWriter();
            writer.write("Hello world!");
        }

    }
}
