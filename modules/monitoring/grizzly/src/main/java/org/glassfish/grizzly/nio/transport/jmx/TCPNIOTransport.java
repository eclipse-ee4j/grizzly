/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio.transport.jmx;

import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * TCP NIO Transport JMX object.
 * 
 * @author Alexey Stashok
 */
@ManagedObject
@Description("Grizzly TCP NIO Transport")
public class TCPNIOTransport extends NIOTransport {
    public TCPNIOTransport(org.glassfish.grizzly.nio.transport.TCPNIOTransport transport) {
        super(transport);
    }

    @ManagedAttribute(id="server-socket-so-timeout")
    public int getServerSocketSoTimeout() {
        return transport.getServerSocketSoTimeout();
    }

    @ManagedAttribute(id="client-socket-so-timeout")
    public int getClientSocketSoTimeout() {
        return transport.getClientSocketSoTimeout();
    }

    @ManagedAttribute(id="socket-tcp-no-delay")
    public boolean getTcpNoDelay() {
        return ((org.glassfish.grizzly.nio.transport.TCPNIOTransport) transport).isTcpNoDelay();
    }

    @ManagedAttribute(id="socket-reuse-address")
    public boolean getReuseAddress() {
        return transport.isReuseAddress();
    }

    @ManagedAttribute(id="socket-linger")
    public int getLinger() {
        return ((org.glassfish.grizzly.nio.transport.TCPNIOTransport) transport).getLinger();
    }

    @ManagedAttribute(id="socket-keep-alive")
    public boolean getKeepAlive() {
        return ((org.glassfish.grizzly.nio.transport.TCPNIOTransport) transport).isKeepAlive();
    }

    @ManagedAttribute(id="client-connect-timeout-millis")
    public int getConnectTimeout() {
        return transport.getConnectionTimeout();
    }
}
