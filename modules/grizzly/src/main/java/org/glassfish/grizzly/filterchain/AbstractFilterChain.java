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

package org.glassfish.grizzly.filterchain;

import java.util.EnumSet;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Context;
import org.glassfish.grizzly.IOEvent;

/**
 * Abstract {@link FilterChain} implementation,
 * which redirects {@link org.glassfish.grizzly.Processor#process(org.glassfish.grizzly.Context)}
 * call to the {@link AbstractFilterChain#execute(org.glassfish.grizzly.filterchain.FilterChainContext)}
 *
 * @see FilterChain
 * 
 * @author Alexey Stashok
 */
public abstract class AbstractFilterChain implements FilterChain {
    // By default interested in all client connection related events
    protected final EnumSet<IOEvent> interestedIoEventsMask = EnumSet.allOf(IOEvent.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOfType(final Class<? extends Filter> filterType) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            final Filter filter = get(i);
            if (filterType.isAssignableFrom(filter.getClass())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterested(final IOEvent ioEvent) {
        return interestedIoEventsMask.contains(ioEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInterested(final IOEvent ioEvent, final boolean isInterested) {
        if (isInterested) {
            interestedIoEventsMask.add(ioEvent);
        } else {
            interestedIoEventsMask.remove(ioEvent);
        }
    }

    @Override
    public final FilterChainContext obtainFilterChainContext(
            final Connection connection) {

        final FilterChainContext context = FilterChainContext.create(connection);
        context.internalContext.setProcessor(this);
        return context;
    }

    @Override
    public FilterChainContext obtainFilterChainContext(
            final Connection connection,
            final Closeable closeable) {
        final FilterChainContext context = FilterChainContext.create(
                connection, closeable);
        context.internalContext.setProcessor(this);
        return context;
    }

    @Override
    public FilterChainContext obtainFilterChainContext(
            final Connection connection, final int startIdx, final int endIdx,
            final int currentIdx) {
        final FilterChainContext ctx = obtainFilterChainContext(connection);

        ctx.setStartIdx(startIdx);
        ctx.setEndIdx(endIdx);
        ctx.setFilterIdx(currentIdx);

        return ctx;
    }

    @Override
    public FilterChainContext obtainFilterChainContext(
            final Connection connection,
            final Closeable closeable,
            final int startIdx, final int endIdx, final int currentIdx) {
        
        final FilterChainContext ctx =
                obtainFilterChainContext(connection, closeable);

        ctx.setStartIdx(startIdx);
        ctx.setEndIdx(endIdx);
        ctx.setFilterIdx(currentIdx);

        return ctx;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append(getClass().getSimpleName())
                .append('@')
                .append(Integer.toHexString(hashCode()))
                .append(" {");
        
        final int size = size();
        
        if (size > 0) {
            sb.append(get(0).toString());
            for (int i = 1; i < size; i++) {
                sb.append(" <-> ");
                sb.append(get(i).toString());
            }
        }
        
        sb.append('}');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Context obtainContext(final Connection connection) {
        return obtainFilterChainContext(connection).internalContext;
    }

    @Override
    protected void finalize() throws Throwable {
        clear();
        super.finalize();
    }
}
