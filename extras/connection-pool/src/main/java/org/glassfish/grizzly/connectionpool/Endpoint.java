/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.connectionpool;

import java.net.SocketAddress;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectorHandler;
import org.glassfish.grizzly.GrizzlyFuture;

/**
 * The abstract class, which describes a pool endpoint and has a method, which creates new {@link Connection} to the
 * endpoint.
 *
 * @param <E> the address type, for example for TCP transport it's {@link SocketAddress}
 *
 * @author Alexey Stashok
 */
public abstract class Endpoint<E> {
    public abstract Object getId();

    public abstract GrizzlyFuture<Connection> connect();

    /**
     * The method is called, once new {@link Connection} related to the <tt>Endpoint</tt> is established.
     *
     * @param connection the {@link Connection}
     * @param pool the pool, to which the {@link Connection} is bound
     */
    protected void onConnect(Connection connection, SingleEndpointPool<E> pool) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Endpoint)) {
            return false;
        }

        return getId().equals(((Endpoint) o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Convenient <tt>Endpoint</tt> factory for cases, when user has a {@link ConnectorHandler} and endpoint address.
     */
    public static final class Factory {

        public static <E> Endpoint<E> create(final E targetAddress, final ConnectorHandler<E> connectorHandler) {
            return create(targetAddress, null, connectorHandler);
        }

        public static <E> Endpoint<E> create(final E targetAddress, final E localAddress, final ConnectorHandler<E> connectorHandler) {
            return create(targetAddress.toString() + (localAddress != null ? localAddress.toString() : ""), targetAddress, localAddress, connectorHandler);
        }

        public static <E> Endpoint<E> create(final Object id, final E targetAddress, final E localAddress, final ConnectorHandler<E> connectorHandler) {
            return new Endpoint<E>() {

                @Override
                public Object getId() {
                    return id;
                }

                @Override
                public GrizzlyFuture<Connection> connect() {
                    return (GrizzlyFuture<Connection>) connectorHandler.connect(targetAddress, localAddress);
                }
            };
        }
    }
}
