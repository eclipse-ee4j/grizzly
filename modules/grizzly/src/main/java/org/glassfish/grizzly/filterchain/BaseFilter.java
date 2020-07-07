/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.filterchain;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext.Operation;

/**
 * Provides empty implementation for {@link Filter} processing methods.
 *
 * @see Filter
 *
 * @author Alexey Stashok
 */
public class BaseFilter implements Filter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAdded(FilterChain filterChain) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFilterChainChanged(FilterChain filterChain) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoved(FilterChain filterChain) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleEvent(final FilterChainContext ctx, final FilterChainEvent event) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        return ctx.getInvokeAction();
    }

    /**
     * Notification about exception, occurred on the {@link FilterChain}
     *
     * @param ctx event processing {@link FilterChainContext}
     * @param error error, which occurred during <tt>FilterChain</tt> execution
     */
    @Override
    public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
    }

    public FilterChainContext createContext(final Connection connection, final Operation operation) {
        FilterChain filterChain = (FilterChain) connection.getProcessor();
        final FilterChainContext ctx = filterChain.obtainFilterChainContext(connection);
        final int idx = filterChain.indexOf(this);
        ctx.setOperation(operation);
        ctx.setFilterIdx(idx);
        ctx.setStartIdx(idx);

        return ctx;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }
}
