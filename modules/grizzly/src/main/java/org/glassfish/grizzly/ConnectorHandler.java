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

import java.util.concurrent.Future;

/**
 * Client side connector handler API.
 * <tt>ConnectorHandler</tt> is responsible for creating and initializing
 * {@link Connection}, and optionally connect it to a specific local/remote
 * address.
 *
 * @author Alexey Stashok
 */
public interface ConnectorHandler<E> {

    /**
     * Creates, initializes and establishes {@link Connection} to the specific
     * <code>remoteAddress</code>.
     *
     * @param remoteAddress remote address to connect to
     * @return {@link Future} of connect operation, which could be used to get
     * resulting {@link Connection}
     */
    Future<Connection> connect(E remoteAddress);

    /**
     * Creates, initializes and establishes {@link Connection} to the specific
     * <code>remoteAddress</code>.
     *
     * @param remoteAddress remote address to connect to
     * @param completionHandler {@link CompletionHandler}
     */
    void connect(E remoteAddress,
                 CompletionHandler<Connection> completionHandler);

    /**
     * Creates, initializes {@link Connection}, binds it to the specific local
     * and remote <code>remoteAddress</code>.
     *
     * @param remoteAddress remote address to connect to
     * @param localAddress local address to bind a {@link Connection} to
     * @return {@link Future} of connect operation, which could be used to get
     * resulting {@link Connection}
     */
    Future<Connection> connect(E remoteAddress, E localAddress);

    /**
     * Creates, initializes {@link Connection}, binds it to the specific local
     * and remote <code>remoteAddress</code>.
     *
     * @param remoteAddress remote address to connect to
     * @param localAddress local address to bind a {@link Connection} to
     * @param completionHandler {@link CompletionHandler}
     * resulting {@link Connection}
     */
    Future<Connection> connect(E remoteAddress,
                 E localAddress,
                 CompletionHandler<Connection> completionHandler);
}
