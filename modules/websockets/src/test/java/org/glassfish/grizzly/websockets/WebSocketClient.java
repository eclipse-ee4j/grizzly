/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.Futures;

public class WebSocketClient extends SimpleWebSocket {
    private static final Logger logger = Logger.getLogger(Constants.WEBSOCKET);
    private final Version version;
    private final URI address;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    protected TCPNIOTransport transport;

    public WebSocketClient(String uri, WebSocketListener... listeners) {
        this(uri, WebSocketEngine.DEFAULT_VERSION, listeners);
    }

    public WebSocketClient(String uri, Version version, WebSocketListener... listeners) {
        super(version.createHandler(true), listeners);
        this.version = version;
        try {
            address = new URI(uri);
        } catch (URISyntaxException e) {
            throw new WebSocketException(e.getMessage(), e);
        }
        add(new WebSocketCloseAdapter());
    }

    public URI getAddress() {
        return address;
    }

    public void execute(Runnable runnable) {
        executorService.submit(runnable);
    }

    /**
     * @return this on successful connection
     */
    public WebSocket connect() {
        return connect(WebSocketEngine.DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * @param timeout number of seconds to timeout trying to connect
     * @param unit time unit to use
     *
     * @return this on successful connection
     */
    public WebSocket connect(long timeout, TimeUnit unit) {
        try {
            buildTransport();
            transport.start();
            final TCPNIOConnectorHandler connectorHandler = new TCPNIOConnectorHandler(transport) {
                @Override
                protected void preConfigure(Connection conn) {
                    super.preConfigure(conn);
//                    final ProtocolHandler handler = version.createHandler(true);
                    /*
                     * holder.handshake = handshake;
                     */
                    protocolHandler.setConnection(conn);
                    final WebSocketHolder holder = WebSocketHolder.set(conn, protocolHandler, WebSocketClient.this);
                    holder.handshake = protocolHandler.createClientHandShake(address);
                }
            };
            final FutureImpl<Boolean> completeFuture = Futures.createSafeFuture();
            add(new WebSocketAdapter() {
                @Override
                public void onConnect(final WebSocket socket) {
                    super.onConnect(socket);
                    completeFuture.result(Boolean.TRUE);
                }
            });

            connectorHandler.setProcessor(createFilterChain(completeFuture));
            // start connect
            connectorHandler.connect(new InetSocketAddress(address.getHost(), address.getPort()), new EmptyCompletionHandler<Connection>() {

                @Override
                public void failed(Throwable throwable) {
                    completeFuture.failure(throwable);
                }

                @Override
                public void cancelled() {
                    completeFuture.failure(new CancellationException());
                }

            });

            completeFuture.get(timeout, unit);
            return this;
        } catch (Throwable e) {
            if (e instanceof ExecutionException) {
                e = e.getCause();
            }

            if (e instanceof HandshakeException) {
                throw (HandshakeException) e;
            }

            throw new HandshakeException(e.getMessage());
        }
    }

    protected void buildTransport() {
        transport = TCPNIOTransportBuilder.newInstance().build();
    }

    private static Processor createFilterChain(final FutureImpl<Boolean> completeFuture) {

        FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
        clientFilterChainBuilder.add(new TransportFilter());
        clientFilterChainBuilder.add(new HttpClientFilter());
        clientFilterChainBuilder.add(new WebSocketClientFilter() {

            @Override
            protected void onHandshakeFailure(Connection connection, HandshakeException e) {
                completeFuture.failure(e);
            }
        });

        return clientFilterChainBuilder.build();
    }

    private class WebSocketCloseAdapter extends WebSocketAdapter {
        @Override
        public void onClose(WebSocket socket, DataFrame frame) {
            super.onClose(socket, frame);
            if (transport != null) {
                try {
                    transport.shutdownNow();
                } catch (IOException e) {
                    logger.log(Level.INFO, e.getMessage(), e);
                }
            }
        }
    }
}
