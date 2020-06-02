/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectorHandler;
import org.glassfish.grizzly.GrizzlyFuture;

/**
 * Simple {@link Endpoint} implementation.
 *
 * The <tt>EndpointKey</tt> contains the endpoint address, that will be used by a {@link ConnectorHandler} passed to
 * {@link MultiEndpointPool} to establish a new client-side {@link org.glassfish.grizzly.Connection}. Additionally, the
 * <tt>EndpointKey</tt> contains an internal key object ({@link #getInternalKey()}) that is used in the
 * {@link #equals(java.lang.Object)} and {@link #hashCode()} methods.
 *
 * @param <E>
 * @author Alexey Stashok
 */
public class EndpointKey<E> extends Endpoint<E> {
    private final Object internalKey;
    private final E targetEndpointAddress;
    private final E localEndpointAddress;

    private final ConnectorHandler<E> connectorHandler;

    /**
     * Construct <tt>EndpointKey</tt> based on the given internalKey and endpoint.
     *
     * @param internalKey the internal key to be used in {@link #equals(java.lang.Object)} and {@link #hashCode()} methods
     * @param endpointAddress the endpoint address, that will be used by a {@link ConnectorHandler} passed to
     * {@link MultiEndpointPool} to establish new client-side {@link org.glassfish.grizzly.Connection}
     */
    public EndpointKey(final Object internalKey, final E endpointAddress) {
        this(internalKey, endpointAddress, null, null);
    }

    /**
     * Construct <tt>EndpointKey</tt> based on the given internalKey, endpoint, and local endpoint.
     *
     * @param internalKey the internal key to be used in {@link #equals(java.lang.Object)} and {@link #hashCode()} methods
     * @param endpointAddress the endpoint address, that will be used by a {@link ConnectorHandler} passed to
     * {@link MultiEndpointPool} to establish new client-side {@link org.glassfish.grizzly.Connection}
     * @param localEndpointAddress the local address that will be used by the {@link ConnectorHandler} to bind the local
     * side of the outgoing connection.
     */
    public EndpointKey(final Object internalKey, final E endpointAddress, final E localEndpointAddress) {
        this(internalKey, endpointAddress, localEndpointAddress, null);
    }

    /**
     * Construct <tt>EndpointKey</tt> based on the given internalKey, endpoint, and {@link ConnectorHandler}.
     *
     * @param internalKey the internal key to be used in {@link #equals(java.lang.Object)} and {@link #hashCode()} methods
     * @param endpointAddress the endpoint address, that will be used by a {@link ConnectorHandler} passed to
     * {@link MultiEndpointPool} to establish new client-side {@link org.glassfish.grizzly.Connection}
     * @param connectorHandler customized {@link ConnectorHandler} for this endpoint
     */
    public EndpointKey(final Object internalKey, final E endpointAddress, final ConnectorHandler<E> connectorHandler) {
        this(internalKey, endpointAddress, null, connectorHandler);
    }

    /**
     *
     * @param internalKey the internal key to be used in {@link #equals(java.lang.Object)} and {@link #hashCode()} methods
     * @param endpointAddress the endpoint address, that will be used by a {@link ConnectorHandler} passed to
     * {@link MultiEndpointPool} to establish new client-side {@link org.glassfish.grizzly.Connection}
     * @param localEndpointAddress the local address that will be used by the {@link ConnectorHandler} to bind the local
     * side of the outgoing connection.
     * @param connectorHandler customized {@link ConnectorHandler} for this endpoint
     */
    public EndpointKey(final Object internalKey, final E endpointAddress, final E localEndpointAddress, final ConnectorHandler<E> connectorHandler) {
        if (internalKey == null) {
            throw new NullPointerException("internal key can't be null");
        }

        if (endpointAddress == null) {
            throw new NullPointerException("target endpoint address can't be null");
        }

        this.internalKey = internalKey;
        this.targetEndpointAddress = endpointAddress;
        this.localEndpointAddress = localEndpointAddress;
        this.connectorHandler = connectorHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GrizzlyFuture<Connection> connect() {
        return (GrizzlyFuture) connectorHandler.connect(targetEndpointAddress, localEndpointAddress);
    }

    @Override
    public Object getId() {
        return getInternalKey();
    }

    /**
     * @return the internal key used in {@link #equals(java.lang.Object)} and {@link #hashCode()} methods
     */
    public Object getInternalKey() {
        return internalKey;
    }

    /**
     * @return the endpoint address, used by a {@link ConnectorHandler} passed to {@link MultiEndpointPool} to establish new
     * client-side {@link org.glassfish.grizzly.Connection}
     */
    public E getEndpoint() {
        return targetEndpointAddress;
    }

    /**
     * @return the local endpoint address that be bound to when making the outgoing connection.
     */
    public E getLocalEndpoint() {
        return localEndpointAddress;
    }

    /**
     * @return a customized {@link ConnectorHandler}, which will be used to create {@link org.glassfish.grizzly.Connection}s
     * to this endpoint.
     */
    public ConnectorHandler<E> getConnectorHandler() {
        return connectorHandler;
    }

    @Override
    public String toString() {
        return "EndpointKey{" + "internalKey=" + internalKey + ", targetEndpointAddress=" + targetEndpointAddress + ", localEndpointAddress="
                + localEndpointAddress + ", connectorHandler=" + connectorHandler + "} " + super.toString();
    }
}
