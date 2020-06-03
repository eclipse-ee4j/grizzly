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

package org.glassfish.grizzly.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Transport;

/**
 * RoundRobin NIOConnectionDistributor implementation, which allocates one SelectorRunner for OP_ACCEPT events and other
 * event will be assign to a next SelectorRunner from the array.
 *
 * @author Alexey Stashok
 */
public final class RoundRobinConnectionDistributor extends AbstractNIOConnectionDistributor {
    private final Iterator it;

    public RoundRobinConnectionDistributor(final NIOTransport transport) {
        this(transport, false, false);
    }

    public RoundRobinConnectionDistributor(final NIOTransport transport, final boolean useDedicatedAcceptor) {
        this(transport, useDedicatedAcceptor, false);
    }

    /**
     * Constructs RoundRobinConnectionDistributor with the given configuration.
     *
     * @param transport
     * @param useDedicatedAcceptor depending on this flag server {@link Connection}s, responsible for accepting client
     * connections, will or will not use dedicated {@link SelectorRunner}
     * @param isServerOnly <tt>true</tt> means this {@link NIOChannelDistributor} will be used by a {@link Transport}, which
     * operates as a server only(the Transport will never initiate a client-side {@link Connection}). In this case we're
     * able to use optimized (thread unsafe) distribution algorithm.
     */
    public RoundRobinConnectionDistributor(final NIOTransport transport, final boolean useDedicatedAcceptor, final boolean isServerOnly) {
        super(transport);
        this.it = useDedicatedAcceptor ? isServerOnly ? new ServDedicatedIterator() : new DedicatedIterator()
                : isServerOnly ? new ServSharedIterator() : new SharedIterator();
    }

    @Override
    public void registerChannel(final SelectableChannel channel, final int interestOps, final Object attachment) throws IOException {
        transport.getSelectorHandler().registerChannel(it.next(), channel, interestOps, attachment);
    }

    @Override
    public void registerChannelAsync(final SelectableChannel channel, final int interestOps, final Object attachment,
            final CompletionHandler<RegisterChannelResult> completionHandler) {
        transport.getSelectorHandler().registerChannelAsync(it.next(), channel, interestOps, attachment, completionHandler);
    }

    @Override
    public void registerServiceChannelAsync(final SelectableChannel channel, final int interestOps, final Object attachment,
            final CompletionHandler<RegisterChannelResult> completionHandler) {

        transport.getSelectorHandler().registerChannelAsync(it.nextService(), channel, interestOps, attachment, completionHandler);
    }

    private interface Iterator {
        SelectorRunner next();

        SelectorRunner nextService();
    }

    private final class DedicatedIterator implements Iterator {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public SelectorRunner next() {
            final SelectorRunner[] runners = getTransportSelectorRunners();
            if (runners.length == 1) {
                return runners[0];
            }

            return runners[(counter.getAndIncrement() & 0x7fffffff) % (runners.length - 1) + 1];
        }

        @Override
        public SelectorRunner nextService() {
            return getTransportSelectorRunners()[0];
        }
    }

    private final class SharedIterator implements Iterator {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public SelectorRunner next() {
            final SelectorRunner[] runners = getTransportSelectorRunners();
            if (runners.length == 1) {
                return runners[0];
            }

            return runners[(counter.getAndIncrement() & 0x7fffffff) % runners.length];
        }

        @Override
        public SelectorRunner nextService() {
            return next();
        }
    }

    private final class ServDedicatedIterator implements Iterator {
        private int counter;

        @Override
        public SelectorRunner next() {
            final SelectorRunner[] runners = getTransportSelectorRunners();
            if (runners.length == 1) {
                return runners[0];
            }

            return runners[(counter++ & 0x7fffffff) % (runners.length - 1) + 1];
        }

        @Override
        public SelectorRunner nextService() {
            return getTransportSelectorRunners()[0];
        }
    }

    private final class ServSharedIterator implements Iterator {
        private int counter;

        @Override
        public SelectorRunner next() {
            final SelectorRunner[] runners = getTransportSelectorRunners();
            if (runners.length == 1) {
                return runners[0];
            }

            return runners[(counter++ & 0x7fffffff) % runners.length];
        }

        @Override
        public SelectorRunner nextService() {
            return next();
        }
    }
}
