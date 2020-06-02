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

package org.glassfish.grizzly.nio.transport;

import org.glassfish.grizzly.NIOTransportBuilder;
import org.glassfish.grizzly.nio.NIOTransport;

/**
 * {@link NIOTransportBuilder} implementation for <code>TCP</code>.
 *
 * @since 2.0
 */
@SuppressWarnings("ALL")
public class TCPNIOTransportBuilder extends NIOTransportBuilder<TCPNIOTransportBuilder> {

    protected boolean keepAlive = TCPNIOTransport.DEFAULT_KEEP_ALIVE;
    protected int linger = TCPNIOTransport.DEFAULT_LINGER;
    protected int serverConnectionBackLog = TCPNIOTransport.DEFAULT_SERVER_CONNECTION_BACKLOG;
    protected int serverSocketSoTimeout = TCPNIOTransport.DEFAULT_SERVER_SOCKET_SO_TIMEOUT;
    protected boolean tcpNoDelay = TCPNIOTransport.DEFAULT_TCP_NO_DELAY;

    // ------------------------------------------------------------ Constructors

    protected TCPNIOTransportBuilder(Class<? extends TCPNIOTransport> transportClass) {
        super(transportClass);
    }

    // ---------------------------------------------------------- Public Methods

    public static TCPNIOTransportBuilder newInstance() {
        return new TCPNIOTransportBuilder(TCPNIOTransport.class);
    }

    /**
     * @see TCPNIOTransport#isKeepAlive() ()
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * @see TCPNIOTransport#setKeepAlive(boolean)
     *
     * @return this <code>TCPNIOTransportBuilder</code>
     */
    public TCPNIOTransportBuilder setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return getThis();
    }

    /**
     * @see TCPNIOTransport#getLinger()
     */
    public int getLinger() {
        return linger;
    }

    /**
     * @see TCPNIOTransport#setLinger(int)
     *
     * @return this <code>TCPNIOTransportBuilder</code>
     */
    public TCPNIOTransportBuilder setLinger(int linger) {
        this.linger = linger;
        return getThis();
    }

    /**
     * @see TCPNIOTransport#getServerConnectionBackLog() ()
     */
    public int getServerConnectionBackLog() {
        return serverConnectionBackLog;
    }

    /**
     * @see TCPNIOTransport#setServerConnectionBackLog(int)
     *
     * @return this <code>TCPNIOTransportBuilder</code>
     */
    public TCPNIOTransportBuilder setServerConnectionBackLog(int serverConnectionBackLog) {
        this.serverConnectionBackLog = serverConnectionBackLog;
        return getThis();
    }

    /**
     * @see TCPNIOTransport#getServerSocketSoTimeout()
     */
    public int getServerSocketSoTimeout() {
        return serverSocketSoTimeout;
    }

    /**
     * @see TCPNIOTransport#setServerSocketSoTimeout(int)
     *
     * @return this <code>TCPNIOTransportBuilder</code>
     */
    public TCPNIOTransportBuilder setServerSocketSoTimeout(int serverSocketSoTimeout) {
        this.serverSocketSoTimeout = serverSocketSoTimeout;
        return getThis();
    }

    /**
     * @see TCPNIOTransport#isTcpNoDelay()
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * @see TCPNIOTransport#setTcpNoDelay(boolean)
     *
     * @return this <code>TCPNIOTransportBuilder</code>
     */
    public TCPNIOTransportBuilder setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return getThis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TCPNIOTransport build() {
        TCPNIOTransport transport = (TCPNIOTransport) super.build();
        transport.setKeepAlive(keepAlive);
        transport.setLinger(linger);
        transport.setServerConnectionBackLog(serverConnectionBackLog);
        transport.setTcpNoDelay(tcpNoDelay);
        transport.setServerSocketSoTimeout(serverSocketSoTimeout);
        return transport;
    }

    // ------------------------------------------------------- Protected Methods

    @Override
    protected TCPNIOTransportBuilder getThis() {
        return this;
    }

    @Override
    protected NIOTransport create(final String name) {
        return new TCPNIOTransport(name);
    }
}
