/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.lifecycle;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Sample {@link org.glassfish.grizzly.filterchain.Filter}, which tracks the connections lifecycle. The new connections
 * could be either accepted if we have server, or connected, if we establish client connection.
 *
 * @author Alexey Stashok
 */
public class LifeCycleFilter extends BaseFilter {
    private final Attribute<Integer> connectionIdAttribute = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("connection-id");

    private final AtomicInteger totalConnectionNumber;
    private final Map<Connection, Integer> activeConnectionsMap;

    public LifeCycleFilter() {
        totalConnectionNumber = new AtomicInteger();
        activeConnectionsMap = new ConcurrentHashMap<>();
    }

    /**
     * Method is called, when new {@link Connection} was accepted by a {@link org.glassfish.grizzly.Transport}
     *
     * @param ctx the filter chain context
     * @return the next action to be executed by chain
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException {
        newConnection(ctx.getConnection());

        return ctx.getInvokeAction();
    }

    /**
     * Method is called, when new client {@link Connection} was connected to some endpoint
     *
     * @param ctx the filter chain context
     * @return the next action to be executed by chain
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        newConnection(ctx.getConnection());

        return ctx.getInvokeAction();
    }

    /**
     * Method is called, when the {@link Connection} is getting closed
     *
     * @param ctx the filter chain context
     * @return the next action to be executed by chain
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        activeConnectionsMap.remove(ctx.getConnection());
        return super.handleClose(ctx);
    }

    /**
     * Add connection to the {@link Map>
     *
     * @param connection new {@link Connection}
     */
    private void newConnection(Connection connection) {
        final Integer id = totalConnectionNumber.incrementAndGet();
        connectionIdAttribute.set(connection, id);
        activeConnectionsMap.put(connection, id);
    }

    /**
     * Returns the total number of connections ever created by the {@link org.glassfish.grizzly.Transport}
     *
     * @return the total number of connections ever created by the {@link org.glassfish.grizzly.Transport}
     */
    public int getTotalConnections() {
        return totalConnectionNumber.get();
    }

    /**
     * Returns the {@link Set} of currently active {@link Connection}s.
     *
     * @return the {@link Set} of currently active {@link Connection}s
     */
    public Set<Connection> getActiveConnections() {
        return activeConnectionsMap.keySet();
    }
}
