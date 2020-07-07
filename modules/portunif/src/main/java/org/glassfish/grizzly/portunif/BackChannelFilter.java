/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.portunif;

import java.io.IOException;

import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainContext.TransportContext;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * The {@link org.glassfish.grizzly.filterchain.Filter}, which is responsible to connect custom protocol
 * {@link FilterChain} with main {@link FilterChain}. Usually this {@link org.glassfish.grizzly.filterchain.Filter} is
 * getting added to the custom protocol {@link FilterChain} as first {@link org.glassfish.grizzly.filterchain.Filter}.
 *
 * @author Alexey Stashok
 */
public class BackChannelFilter extends BaseFilter {
    private final PUFilter puFilter;

    BackChannelFilter(final PUFilter puFilter) {
        this.puFilter = puFilter;
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // If this method is called as part of natural PU filterchain processing -
        // just pass process to the next filter
        if (!isFilterChainRead(ctx)) {
            return ctx.getInvokeAction();
        }

        // if this is filterchain read - delegate read to the underlying filterchain
        final FilterChainContext suspendedParentContext = puFilter.suspendedContextAttribute.get(ctx);

        assert suspendedParentContext != null;

        final ReadResult readResult = suspendedParentContext.read();

        ctx.setMessage(readResult.getMessage());
        ctx.setAddressHolder(readResult.getSrcAddressHolder());

        readResult.recycle();

        return ctx.getInvokeAction();
    }

    /**
     * Methods returns <tt>true</tt>, if {@link #handleRead(org.glassfish.grizzly.filterchain.FilterChainContext)} is called
     * because user explicitly initiated FilterChain by calling {@link FilterChainContext#read()} or
     * {@link FilterChain#read(org.glassfish.grizzly.filterchain.FilterChainContext)}; otherwise <tt>false</tt> is returned.
     */
    private boolean isFilterChainRead(final FilterChainContext ctx) {
        return ctx.getMessage() == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        final FilterChainContext suspendedParentContext = puFilter.suspendedContextAttribute.get(ctx);

        assert suspendedParentContext != null;

        final TransportContext transportContext = ctx.getTransportContext();

        suspendedParentContext.write(ctx.getAddress(), ctx.getMessage(), transportContext.getCompletionHandler(), transportContext.getPushBackHandler(),
                transportContext.getMessageCloner(), transportContext.isBlocking());

        return ctx.getStopAction();
    }

    @Override
    public NextAction handleEvent(final FilterChainContext ctx, final FilterChainEvent event) throws IOException {

        // if downstream event - pass it to the puFilter
        if (isDownstream(ctx)) {
            final FilterChainContext suspendedParentContext = puFilter.suspendedContextAttribute.get(ctx);

            assert suspendedParentContext != null;

            suspendedParentContext.notifyDownstream(event);
        }

        return ctx.getInvokeAction();
    }

    @Override
    public void exceptionOccurred(final FilterChainContext ctx, final Throwable error) {

        // if downstream event - pass it to the puFilter
        if (isDownstream(ctx)) {
            final FilterChainContext suspendedParentContext = puFilter.suspendedContextAttribute.get(ctx);

            assert suspendedParentContext != null;

            suspendedParentContext.fail(error);
        }
    }

    private static boolean isDownstream(final FilterChainContext context) {
        return context.getStartIdx() > context.getEndIdx();
    }
}
