/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.Future;

/**
 * Socket based client side connector.
 * <tt>SocketConnectorHandler</tt> is responsible for creating and initializing
 * {@link Connection}, and optionally connect is to a specific local/remote
 * address.
 * 
 * @author Alexey Stashok
 */
public interface SocketConnectorHandler extends ConnectorHandler<SocketAddress> {


    int DEFAULT_CONNECTION_TIMEOUT = 30000;

    /**
     * Creates, initializes and connects socket to the specific remote host
     * and port and returns {@link Connection}, representing socket.
     * 
     * @param host remote host to connect to.
     * @param port remote port to connect to.
     * @return {@link Future} of connect operation, which could be used to get
     * resulting {@link Connection}.
     * 
     * @throws java.io.IOException
     */
    Future<Connection> connect(String host, int port) throws IOException;
}
