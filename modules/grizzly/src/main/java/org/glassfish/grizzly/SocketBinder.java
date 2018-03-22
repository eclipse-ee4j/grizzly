/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Common API for {@link java.net.Socket} based {@link Transport}s, which are able
 * to bind server {@link java.net.Socket} to specific address and listen for incoming
 * data.
 *
 * @author Alexey Stashok
 */
public interface SocketBinder {
    /**
     * Binds Transport to the specific port on localhost.
     *
     * @param port
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(int port) throws IOException;

    /**
     * Binds Transport to the specific host and port.
     *
     * @param host the local host the server will bind to
     * @param port
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(String host, int port) throws IOException;

    /**
     * Binds Transport to the specific host and port.
     * @param host the local host the server will bind to
     * @param port
     * @param backlog the maximum length of the queue
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(String host, int port, int backlog) throws IOException;

    /**
     * Binds Transport to the specific host, and port within a {@link PortRange}.
     *
     * @param host the local host the server will bind to
     * @param portRange {@link PortRange}.
     * @param backlog the maximum length of the queue
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(String host, PortRange portRange, int backlog) throws IOException;

    /**
     * Binds Transport to the specific host, and port within a {@link PortRange}.
     *
     * @param host the local host the server will bind to
     * @param portRange {@link PortRange}.
     * @param randomStartPort if true, a random port in the range will be used as the initial port.
     * @param backlog the maximum length of the queue
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(String host, PortRange portRange, boolean randomStartPort, int backlog) throws IOException;

    /**
     * Binds Transport to the specific SocketAddress.
     *
     * @param socketAddress the local address the server will bind to
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(SocketAddress socketAddress) throws IOException;

    /**
     * Binds Transport to the specific SocketAddress.
     *
     * @param socketAddress the local address the server will bind to
     * @param backlog the maximum length of the queue
     * @return bound {@link Connection}
     *
     * @throws java.io.IOException
     */
    Connection bind(SocketAddress socketAddress, int backlog) throws IOException;

    /**
     * Binds the Transport to the channel inherited from the entity that
     * created this Java virtual machine.
     * 
     * @return bound {@link Connection}
     * 
     * @throws IOException 
     */
    Connection bindToInherited() throws IOException;
    
    /**
     * Unbinds bound {@link Transport} connection.
     * @param connection {@link Connection}
     *
     * @throws java.io.IOException
     */
    void unbind(Connection connection);

    /**
     * Unbinds all bound {@link Transport} connections.
     *
     * @throws java.io.IOException
     */
    void unbindAll();

}
