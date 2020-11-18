/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.net.SocketAddress;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

public class WebSocketServer {
    private static final Logger logger = Grizzly.logger(WebSocketServer.class);
    private static final Object SYNC = new Object();
    private HttpServer httpServer;

    /**
     * Empty constructor, which doesn't do any network initialization.
     */
    protected WebSocketServer() {

    }

    /**
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>port</code>.
     *
     * @deprecated please use {@link #createServer(int)}.
     */
    @Deprecated
    public WebSocketServer(final int port) {
        this(NetworkListener.DEFAULT_NETWORK_HOST, new PortRange(port));
    }

    /**
     * @param host the network port to which this listener will bind.
     * @param portRange port range to attempt to bind to.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * <code>host</code>:<code>[port-range]</code>.
     */
    protected WebSocketServer(final String host, final PortRange portRange) {
        final NetworkListener networkListener = new NetworkListener("WebSocket NetworkListener", host, portRange);
        networkListener.setMaxPendingBytes(-1);
        networkListener.registerAddOn(new WebSocketAddOn());

        httpServer = new HttpServer();
        final ServerConfiguration config = httpServer.getServerConfiguration();
        config.addHttpHandler(new StaticHttpHandler("."), "/");

        config.setHttpServerName("WebSocket Server");
        config.setName("WebSocket Server");

        httpServer.addListener(networkListener);
    }

    /**
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>port</code>.
     *
     * @deprecated please use {@link #createServer(int)}.
     */
    @Deprecated
    public static WebSocketServer createSimpleServer(final int port) {
        return createServer(port);
    }

    /**
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>port</code>.
     */
    public static WebSocketServer createServer(final int port) {
        return createServer(NetworkListener.DEFAULT_NETWORK_HOST, new PortRange(port));
    }

    /**
     * @param range port range to attempt to bind to.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>[port-range]</code>.
     */
    public static WebSocketServer createServer(final PortRange range) {

        return createServer(NetworkListener.DEFAULT_NETWORK_HOST, range);

    }

    /**
     * @param socketAddress the endpoint address to which this listener will bind.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on <code>socketAddress</code>.
     */
    public static WebSocketServer createServer(final SocketAddress socketAddress) {

        final InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
        return createServer(inetAddr.getHostName(), inetAddr.getPort());
    }

    /**
     * @param host the network port to which this listener will bind.
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on <code>host</code>:<code>port</code>.
     */
    public static WebSocketServer createServer(final String host, final int port) {

        return createServer(host, new PortRange(port));

    }

    /**
     * @param host the network port to which this listener will bind.
     * @param range port range to attempt to bind to.
     *
     * @return a <code>WebSocketServer</code> configured to listen to requests on
     * <code>host</code>:<code>[port-range]</code>.
     */
    public static WebSocketServer createServer(final String host, final PortRange range) {

        return new WebSocketServer(host, range);
    }

    public void start() throws IOException {
        synchronized (SYNC) {
            httpServer.start();
        }
    }

    public void stop() {
        synchronized (SYNC) {
            httpServer.shutdownNow();
            WebSocketEngine.getEngine().unregisterAll();
        }
    }

    public void register(String contextPath, String urlPattern, WebSocketApplication application) {
        WebSocketEngine.getEngine().register(contextPath, urlPattern, application);
    }
}
