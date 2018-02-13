/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.nio.transport;

import org.glassfish.grizzly.AbstractBindingHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.utils.Exceptions;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.locks.Lock;

/**
 * This class may be used to apply a custom {@link org.glassfish.grizzly.Processor} and/or {@link org.glassfish.grizzly.ProcessorSelector}
 * atomically within a bind operation - not something that can normally be done using the {@link UDPNIOTransport} alone.
 *
 * Example usage:
 * <pre>
 *     UDPNIOBindingHandler handler = UDPNIOBindingHandler.builder(transport).setProcessor(custom).build();
 *     handler.bind(socketAddress);
 * </pre>
 *
 * @since 2.2.19
 */
public class UDPNIOBindingHandler extends AbstractBindingHandler {

    private final UDPNIOTransport udpTransport;

    // ------------------------------------------------------------ Constructors


    public UDPNIOBindingHandler(UDPNIOTransport udpTransport) {
        super(udpTransport);
        this.udpTransport = udpTransport;
    }


    // ------------------------------- Methods from AbstractBindingHandler


    @Override
    public UDPNIOServerConnection bind(SocketAddress socketAddress) throws IOException {
        return bind(socketAddress, -1);
    }

    @Override
    public UDPNIOServerConnection bind(SocketAddress socketAddress, int backlog) throws IOException {
        return bindToChannel(
                udpTransport.getSelectorProvider().openDatagramChannel(),
                socketAddress);
    }

    @Override
    public UDPNIOServerConnection bindToInherited() throws IOException {
        return bindToChannel(
                this.<DatagramChannel>getSystemInheritedChannel(DatagramChannel.class),
                null);
    }

    @Override
    public void unbind(Connection connection) {
        udpTransport.unbind(connection);
    }

    public static Builder builder(final UDPNIOTransport transport) {
        return new UDPNIOBindingHandler.Builder().transport(transport);
    }


    // --------------------------------------------------------- Private Methods


    private UDPNIOServerConnection bindToChannel(final DatagramChannel serverDatagramChannel,
                                                 final SocketAddress socketAddress)
    throws IOException {
        UDPNIOServerConnection serverConnection = null;

        final Lock lock = udpTransport.getState().getStateLocker().writeLock();
        lock.lock();
        try {
            udpTransport.getChannelConfigurator().preConfigure(transport,
                    serverDatagramChannel);

            if (socketAddress != null) {
                final DatagramSocket socket = serverDatagramChannel.socket();
                socket.bind(socketAddress);
            }

            udpTransport.getChannelConfigurator().postConfigure(transport,
                    serverDatagramChannel);

            serverConnection = udpTransport.obtainServerNIOConnection(serverDatagramChannel);
            serverConnection.setProcessor(getProcessor());
            serverConnection.setProcessorSelector(getProcessorSelector());
            udpTransport.serverConnections.add(serverConnection);

            if (!udpTransport.isStopped()) {
                serverConnection.register();
            }

            return serverConnection;
        } catch (Exception e) {
            if (serverConnection != null) {
                udpTransport.serverConnections.remove(serverConnection);

                serverConnection.closeSilently();
            } else {
                try {
                    serverDatagramChannel.close();
                } catch (IOException ignored) {
                }
            }

            throw Exceptions.makeIOException(e);
        } finally {
            lock.unlock();
        }
    }


    // ----------------------------------------------------------- Inner Classes


    public static class Builder extends AbstractBindingHandler.Builder<Builder> {

        private UDPNIOTransport transport;


        public UDPNIOBindingHandler build() {
            return (UDPNIOBindingHandler) super.build();
        }

        public Builder transport(UDPNIOTransport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        protected AbstractBindingHandler create() {
            if (transport == null) {
                throw new IllegalStateException(
                        "Unable to create TCPNIOBindingHandler - transport is null");
            }
            return new UDPNIOBindingHandler(transport);
        }

    } // END UDPNIOSocketBindingHandlerBuilder
}
