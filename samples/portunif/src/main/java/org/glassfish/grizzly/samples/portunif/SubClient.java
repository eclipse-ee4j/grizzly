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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.samples.portunif.subservice.SubClientMessageFilter;
import org.glassfish.grizzly.samples.portunif.subservice.SubRequestMessage;
import org.glassfish.grizzly.samples.portunif.subservice.SubResponseMessage;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Client app, which tests deployed SUB-service.
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class SubClient {
    private static final Logger LOGGER = Grizzly.logger(PUServer.class);

    public static void main(String[] args) throws Exception {
        Connection connection = null;

        // Construct the client filter chain
        final FilterChainBuilder puFilterChainBuilder = FilterChainBuilder.stateless()
                // Add TransportFilter
                .add(new TransportFilter())
                // Add SUB-service message parser/serializer
                .add(new SubClientMessageFilter())
                // Add Result reporter Filter
                .add(new ResultFilter());

        // Construct TCPNIOTransport
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(puFilterChainBuilder.build());

        try {
            // Start
            transport.start();

            // Create the client connection
            final Future<Connection> connectFuture = transport.connect("localhost", PUServer.PORT);
            connection = connectFuture.get(10, TimeUnit.SECONDS);

            LOGGER.info("Enter 2 numbers separated by space (<value1> <value2>) end press <enter>.");
            LOGGER.info("Type q and enter to exit.");

            // Read user input and communicate the SUB-service
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charsets.ASCII_CHARSET));
            while ((line = reader.readLine()) != null) {
                if ("q".equals(line)) {
                    break;
                }

                // Parse user input
                final int value1;
                final int value2;
                try {
                    final String[] values = line.split(" ");

                    value1 = Integer.parseInt(values[0].trim());
                    value2 = Integer.parseInt(values[1].trim());
                } catch (Exception e) {
                    LOGGER.warning("Bad format, repeat pls");
                    continue;
                }

                // send the request to SUB-service
                final GrizzlyFuture<WriteResult> writeFuture = connection.write(new SubRequestMessage(value1, value2));

                final WriteResult result = writeFuture.get(10, TimeUnit.SECONDS);
                assert result != null;
            }

        } finally {
            // Close the client connection
            if (connection != null) {
                connection.closeSilently();
            }

            // Shutdown the transport
            transport.shutdownNow();
        }
    }

    // Simple reporting Filter
    private static final class ResultFilter extends BaseFilter {

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            // Take SUB-service response
            final SubResponseMessage subResponseMessage = ctx.getMessage();

            // do output
            LOGGER.log(Level.INFO, "Result={0}", subResponseMessage.getResult());

            return ctx.getStopAction();
        }
    }
}
