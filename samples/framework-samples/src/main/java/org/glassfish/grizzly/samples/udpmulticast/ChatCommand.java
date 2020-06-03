/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.udpmulticast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.nio.transport.UDPNIOConnection;
import org.glassfish.grizzly.utils.Futures;

/**
 * Class represent chat command abstraction.
 *
 * @author Alexey Stashok
 */
public abstract class ChatCommand {

    /**
     * Method parses chat command from the input String.
     */
    public static ChatCommand parse(final String command) throws Exception {
        if (command == null) {
            return null;
        }

        // Split input string, using space (0x20) as a delimiter
        String[] splitString = command.trim().split(" ");

        if (splitString.length == 0) {
            return null;
        }

        final int id;
        try {
            // parse the command id
            id = Integer.valueOf(splitString[0]);
        } catch (NumberFormatException e) {
            System.out.println("Bad command: can't parse command id");
            return null;
        }

        switch (id) {
        case 1:
            return new SendMsgCommand(splitString);
        case 2:
            return new JoinGroupCommand(splitString);
        case 3:
            return new LeaveGroupCommand(splitString);
        case 4:
            return new BlockSourceCommand(splitString);
        case 5:
            return new UnblockSourceCommand(splitString);
        case 6:
            return new ListNetworkInterfacesCommand();
        case 7:
            return new ExitCommand();
        }

        return null;
    }

    public boolean isExit() {
        return false;
    }

    public abstract void run(UDPNIOConnection connection) throws Exception;

    /**
     * Send message command
     */
    private static class SendMsgCommand extends ChatCommand {
        /**
         * multicast group address
         */
        private final InetAddress groupAddr;
        /**
         * Message to send
         */
        private final String msg;

        public SendMsgCommand(String[] params) throws Exception {
            if (params.length < 3) {
                throw new IllegalArgumentException("Send message command expects 2 parameters: group_addr message");
            }

            // get the multicast group address
            groupAddr = InetAddress.getByName(params[1]);

            // from the rest split parameters build a message to send
            final StringBuilder messageBuilder = new StringBuilder();

            for (int i = 2; i < params.length; i++) {
                if (messageBuilder.length() > 0) {
                    messageBuilder.append(' ');
                }

                messageBuilder.append(params[i].trim());
            }

            msg = messageBuilder.toString();
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            // construct destination multicast address to send the message to
            final InetSocketAddress peerAddr = new InetSocketAddress(groupAddr, ((InetSocketAddress) connection.getLocalAddress()).getPort());

            // Create Future to be able to block until the message is sent
            final FutureImpl<WriteResult<String, SocketAddress>> writeFuture = Futures.createSafeFuture();

            // Send the message
            connection.write(peerAddr, msg, Futures.toCompletionHandler(writeFuture));

            // Block until the message is sent
            writeFuture.get(10, TimeUnit.SECONDS);
        }
    }

    /**
     * Join multicast group
     */
    private static class JoinGroupCommand extends ChatCommand {
        /**
         * multicast group address
         */
        private final InetAddress groupAddr;
        /**
         * Network Interface (like eth0, wlan0) to join the multicast group on
         */
        private final NetworkInterface ni;
        /**
         * Source address (optional parameter) to listen multicast messages from.
         */
        private final InetAddress source;

        public JoinGroupCommand(String[] params) throws Exception {
            if (params.length != 3 && params.length != 4) {
                throw new IllegalArgumentException("Join group command expects 3 parameters (1 optional): group_addr network_interface [source]");
            }

            // get the multicast group address
            groupAddr = InetAddress.getByName(params[1]);

            // parse Network Interface by name or inet-address
            try {
                ni = NetworkInterface.getByName(params[2]) != null ? NetworkInterface.getByName(params[2])
                        : NetworkInterface.getByInetAddress(InetAddress.getByName(params[2]));
            } catch (Exception e) {
                throw new IllegalArgumentException("Passed network interface can't be resolved");
            }

            // parse the source address to listen multicast messages from (optional)
            source = params.length == 4 ? InetAddress.getByName(params[3]) : null;
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            // Join the multicast group
            connection.join(groupAddr, ni, source);

            // construct destination multicast address to send the message to
            final InetSocketAddress peerAddr = new InetSocketAddress(groupAddr, ((InetSocketAddress) connection.getLocalAddress()).getPort());

            // Create Future to be able to block until the message is sent
            final FutureImpl<WriteResult<String, SocketAddress>> writeFuture = Futures.createSafeFuture();

            // Send the greeting message to group
            connection.write(peerAddr, "joined the group " + groupAddr, Futures.toCompletionHandler(writeFuture));

            // Block until the message is sent
            writeFuture.get(10, TimeUnit.SECONDS);
        }
    }

    /**
     * Drop multicast group membership
     */
    private static class LeaveGroupCommand extends ChatCommand {
        /**
         * multicast group address
         */
        private final InetAddress groupAddr;
        /**
         * Network Interface (like eth0, wlan0) to join the multicast group on
         */
        private final NetworkInterface ni;
        private final InetAddress source;

        public LeaveGroupCommand(String[] params) throws Exception {
            if (params.length != 3 && params.length != 4) {
                throw new IllegalArgumentException("Leave group command expects 3 parameters (1 optional): group_addr network_interface [source]");
            }

            // get the multicast group address
            groupAddr = InetAddress.getByName(params[1]);

            // parse Network Interface by name or inet-address
            try {
                ni = NetworkInterface.getByName(params[2]) != null ? NetworkInterface.getByName(params[2])
                        : NetworkInterface.getByInetAddress(InetAddress.getByName(params[2]));
            } catch (Exception e) {
                throw new IllegalArgumentException("Passed network interface can't be resolved");
            }

            // parse the source address to listen multicast messages from (optional)
            source = params.length == 4 ? InetAddress.getByName(params[3]) : null;
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            // Drop the multicast group membership
            connection.drop(groupAddr, ni, source);

            try {
                // construct destination multicast address to send the message to
                final InetSocketAddress peerAddr = new InetSocketAddress(groupAddr, ((InetSocketAddress) connection.getLocalAddress()).getPort());

                // Create Future to be able to block until the message is sent
                final FutureImpl<WriteResult<String, SocketAddress>> writeFuture = Futures.createSafeFuture();

                // Send the leave message to the group
                connection.write(peerAddr, "left the group " + groupAddr, Futures.toCompletionHandler(writeFuture));

                // Block until the message is sent
                writeFuture.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Block specific peer in the multicast group
     */
    private static class BlockSourceCommand extends ChatCommand {
        /**
         * multicast group address
         */
        private final InetAddress groupAddr;
        /**
         * Network Interface (like eth0, wlan0) to join the multicast group on
         */
        private final NetworkInterface ni;
        /**
         * peer address we want to block
         */
        private final InetAddress source;

        public BlockSourceCommand(String[] params) throws Exception {
            if (params.length != 4) {
                throw new IllegalArgumentException("Block source command expects 3 parameters: group_addr network_interface source");
            }

            // get the multicast group address
            groupAddr = InetAddress.getByName(params[1]);

            // parse Network Interface by name or inet-address
            try {
                ni = NetworkInterface.getByName(params[2]) != null ? NetworkInterface.getByName(params[2])
                        : NetworkInterface.getByInetAddress(InetAddress.getByName(params[2]));
            } catch (Exception e) {
                throw new IllegalArgumentException("Passed network interface can't be resolved");
            }

            // the peer address we want to block
            source = InetAddress.getByName(params[3]);
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            // block the peer address (stop receiving multicast messages from the peer)
            connection.block(groupAddr, ni, source);
        }
    }

    /**
     * Unblock specific peer in the multicast group
     */
    private static class UnblockSourceCommand extends ChatCommand {
        /**
         * multicast group address
         */
        private final InetAddress groupAddr;
        /**
         * Network Interface (like eth0, wlan0) to join the multicast group on
         */
        private final NetworkInterface ni;
        /**
         * peer address we want to unblock
         */
        private final InetAddress source;

        public UnblockSourceCommand(String[] params) throws Exception {
            if (params.length != 4) {
                throw new IllegalArgumentException("Unblock source command expects 3 parameters: group_addr network_interface source");
            }

            // get the multicast group address
            groupAddr = InetAddress.getByName(params[1]);

            // parse Network Interface by name or inet-address
            try {
                ni = NetworkInterface.getByName(params[2]) != null ? NetworkInterface.getByName(params[2])
                        : NetworkInterface.getByInetAddress(InetAddress.getByName(params[2]));
            } catch (Exception e) {
                throw new IllegalArgumentException("Passed network interface can't be resolved");
            }

            // the peer address we want to unblock
            source = InetAddress.getByName(params[3]);
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            // unblock the peer address (allow receiving multicast messages from the peer)
            connection.unblock(groupAddr, ni, source);
        }
    }

    /**
     * List available network interfaces
     */
    private static class ListNetworkInterfacesCommand extends ChatCommand {
        public ListNetworkInterfacesCommand() throws Exception {
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
            final Enumeration<NetworkInterface> niEnumeration = NetworkInterface.getNetworkInterfaces();

            while (niEnumeration.hasMoreElements()) {
                final NetworkInterface ni = niEnumeration.nextElement();
                System.out.println(ni.getName() + " support-multicast=" + ni.supportsMulticast());
            }
        }
    }

    /**
     * Exit chat app
     */
    private static class ExitCommand extends ChatCommand {
        public ExitCommand() throws Exception {
        }

        @Override
        public void run(final UDPNIOConnection connection) throws Exception {
        }

        @Override
        public boolean isExit() {
            return true;
        }
    }
}
