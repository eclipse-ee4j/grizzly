/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif;

import static org.glassfish.grizzly.samples.portunif.PUServer.PORT;
import static org.glassfish.grizzly.samples.portunif.PUServer.configureAddProtocol;
import static org.glassfish.grizzly.samples.portunif.PUServer.configureSubProtocol;

import java.io.IOException;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.portunif.PUFilter;
import org.glassfish.grizzly.portunif.PUProtocol;

/**
 * Port-unification sample, which hosts "HTTP", "add" and "sub" services on the same port.
 *
 * Sample creates a protocol tree:
 *
 * TransportFilter | PUFilter | ------------------------------------------------------- | | | AddServerMessageFilter
 * SubServerMessageFilter HttpCodecFilter | | | AddServiceFilter SubServiceFilter HttpServerFilter
 *
 *
 * @author Alexey Stashok
 */
public class HttpPUServer {
    public static void main(String[] args) throws IOException {
        // Create regular HttpServer
        final HttpServer httpServer = new HttpServer();

        final NetworkListener networkListener = new NetworkListener("pu-http-server", "0.0.0.0", PORT);

        // Register port unification AddOn
        networkListener.registerAddOn(new PortUnificationAddOn());

        // Finish the server initialization
        httpServer.addListener(networkListener);

        httpServer.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                response.getWriter().write("Hello world from HTTP!");
            }
        });

        httpServer.start();

        try {
            Grizzly.logger(HttpPUServer.class)
                    .info("Server is ready...\n" + "You can test it using AddClient, SubClient applications or web browser\n" + "Press enter to exit.");

            System.in.read();
        } finally {
            httpServer.shutdownNow();
        }
    }

    public static class PortUnificationAddOn implements AddOn {

        @Override
        public void setup(final NetworkListener networkListener, final FilterChainBuilder builder) {
            // Create PUFilter.
            // We will try to filter off "add" and "sub" protocols, if they
            // are not recognize - assume it's HTTP.
            // So we don't need to register HTTP ProtocolFinder, but have to
            // pass <tt>false</tt> to the constructor to not let "unrecognized"
            // connections to be closed (because they might be HTTP).
            final PUFilter puFilter = new PUFilter(false);

            // Configure add-service PUProtocol
            final PUProtocol addProtocol = configureAddProtocol(puFilter);
            // Configure sub-service PUProtocol
            final PUProtocol subProtocol = configureSubProtocol(puFilter);

            // Register add-service pu protocol
            puFilter.register(addProtocol);
            // Register sub-service pu protocol
            puFilter.register(subProtocol);

            // now find the place to insert PUFilter in the HTTP FilterChainBuilder.
            // we'll insert the PUFilter right next to the TransportFilter
            final int transportFilterIdx = builder.indexOfType(TransportFilter.class);

            assert transportFilterIdx != -1;

            builder.add(transportFilterIdx + 1, puFilter);
        }

    }
}
