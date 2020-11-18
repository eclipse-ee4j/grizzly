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

import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

/**
 *
 * @author Alexey Stashok
 */
public abstract class AbstractNIOConnectionDistributor implements NIOChannelDistributor {

    protected final NIOTransport transport;

    public AbstractNIOConnectionDistributor(final NIOTransport transport) {
        this.transport = transport;
    }

    @Override
    public final void registerChannel(final SelectableChannel channel) throws IOException {
        registerChannel(channel, 0, null);
    }

    @Override
    public final void registerChannel(final SelectableChannel channel, final int interestOps) throws IOException {
        registerChannel(channel, interestOps, null);
    }

    @Override
    public final GrizzlyFuture<RegisterChannelResult> registerChannelAsync(final SelectableChannel channel) {
        return registerChannelAsync(channel, 0, null);
    }

    @Override
    public final GrizzlyFuture<RegisterChannelResult> registerChannelAsync(final SelectableChannel channel, final int interestOps) {
        return registerChannelAsync(channel, interestOps, null);
    }

    @Override
    public final GrizzlyFuture<RegisterChannelResult> registerChannelAsync(final SelectableChannel channel, final int interestOps, final Object attachment) {

        final FutureImpl<RegisterChannelResult> future = Futures.createSafeFuture();

        registerChannelAsync(channel, interestOps, attachment, Futures.toCompletionHandler(future));

        return future;
    }

    protected SelectorRunner[] getTransportSelectorRunners() {
        return transport.getSelectorRunners();
    }
}
