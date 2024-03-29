/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.udpmulticast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.UDPNIOConnection;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import org.glassfish.grizzly.utils.StringFilter;

/**
 * Simple chat application based on UDP multicast.
 *
 * Requires JDK 1.7+
 *
 * When you run MulticastChat application, the first thing you might want to know is network interfaces' names available
 * on your system (command #6), so type:
 *
 * <pre>
 * '$ 6'
 * </pre>
 * 
 * and press enter to see the interfaces and check if they support multicasting.
 *
 * On my system I see:
 * 
 * <pre>
 * "
 * $6
 * wlan0 support-multicast=true
 * eth0 support-multicast=true
 * lo support-multicast=false
 * "
 * </pre>
 * 
 * which means I can use wlan0 and eth0 interfaces to test multicasting.
 *
 * So now I have to join the multicasting group
 * 
 * <pre>
 * "
 * $2 228.5.6.7 eth0
 * "
 * </pre>
 *
 * The output would be like:
 * 
 * <pre>
 * "
 * Tue Sep 11 16:09:42 CEST 2012 /10.163.25.1:8888: joined the group /228.5.6.7
 * "
 * </pre>
 *
 * Run MulticastChat application on different machine (it's better to use local network, cause multicasting might be
 * blocked by routers), run the same/similar commands, so both clients join the same multicast group.
 *
 * To send message to the multicast group use the command like:
 * 
 * <pre>
 * "
 * $1 228.5.6.7 hello
 * "
 * </pre>
 *
 * @see UDPNIOConnection
 *
 * @author Alexey Stashok
 */
public class MulticastChat {
    private static final Logger logger = Logger.getLogger(MulticastChat.class.getName());

    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        new MulticastChat().run();
    }

    private void run() throws Exception {

        // Build FilterChain to parse incoming UDP packets and print to System.out
        final FilterChain filterChain = FilterChainBuilder.stateless()
                // Add TransportFilter, which will be responsible for reading and
                // writing data to the connection
                .add(new TransportFilter())
                // Add String codec (responsible for byte[] <-> String conversion)
                .add(new StringFilter(Charset.forName("UTF-8")))
                // Add PrintFilter, which is responsible for printing incoming messages
                .add(new PrintFilter()).build();

        // Create UDP transport
        final UDPNIOTransport transport = UDPNIOTransportBuilder.newInstance().setProcessor(filterChain).build();

        UDPNIOConnection connection = null;

        try {
            // start the transport
            transport.start();

            // Create non-connected UDP connection and bind it to the local PORT
            final Future<Connection> connectFuture = transport.connect(null, new InetSocketAddress(PORT));

            connection = (UDPNIOConnection) connectFuture.get(10, TimeUnit.SECONDS);

            // Prepare the console reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
            printCommands();

            do {
                System.out.print("\n$");
                try {
                    // Parse the chat command
                    final ChatCommand command = ChatCommand.parse(reader.readLine());
                    if (command == null) {
                        // If chat command is not recognized - print menu
                        printCommands();
                    } else {
                        // Run the command
                        command.run(connection);

                        // Exit if needed (only exit command returns true)
                        if (command.isExit()) {
                            return;
                        }
                    }
                } catch (Throwable t) {
                    System.out.println(t.getClass().getName() + ": " + t.getMessage());
                }
            } while (true);

        } finally {
            // Close connection is it's not null
            if (connection != null) {
                connection.close();
            }

            logger.fine("Stopping transport...");
            // stop the transport
            transport.shutdownNow();

            logger.fine("Stopped transport...");
        }

    }

    /**
     * Prints the menu of the available commands.
     */
    private static void printCommands() {
        System.out.println("Please make your choice (type command number and command parameters separated by space:");
        System.out.println("1: send message. Parameters: group_addr message. Example: '$1 228.5.6.7 hello'");
        System.out.println("2: join the group. Parameters: group_addr network_interface [source]. Example: '$2 228.5.6.7 eth0'");
        System.out.println("3: leave the group. Parameters: group_addr network_interface [source]. Example: '$3 228.5.6.7 eth0'");
        System.out.println("4: block the source. Parameters: group_addr network_interface source. Example: '$4 228.5.6.7 eth0 192.168.0.10'");
        System.out.println("5: unblock the source. Parameters: group_addr network_interface source. Example: '$5 228.5.6.7 eth0 192.168.0.10'");
        System.out.println("6: list network interfaces. No parameters");
        System.out.println("7: exit");
    }
}
