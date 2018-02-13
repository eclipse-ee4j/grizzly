/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio.transport;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.*;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.nio.RegisterChannelResult;
import org.glassfish.grizzly.utils.Futures;

/**
 * Server {@link org.glassfish.grizzly.Connection} implementation
 * for the {@link UDPNIOTransport}
 *
 * @author Alexey Stashok
 */
public class UDPNIOServerConnection extends UDPNIOConnection {
    private static final Logger LOGGER = Grizzly.logger(UDPNIOServerConnection.class);

    public UDPNIOServerConnection(UDPNIOTransport transport, DatagramChannel channel) {
        super(transport, channel);
    }

    @Override
    public Processor getProcessor() {
        if (processor == null) {
            return transport.getProcessor();
        }

        return processor;
    }

    @Override
    public ProcessorSelector getProcessorSelector() {
        if (processorSelector == null) {
            return transport.getProcessorSelector();
        }

        return processorSelector;
    }

    public void register() throws IOException {

        final FutureImpl<RegisterChannelResult> future =
                Futures.createSafeFuture();

        transport.getNIOChannelDistributor().registerServiceChannelAsync(
                channel,
                SelectionKey.OP_READ, this,
                Futures.toCompletionHandler(future,
                ((UDPNIOTransport) transport).registerChannelCompletionHandler
                ));

        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IOException("Error registering server channel key", e);
        }
        
        notifyReady();
    }
    
    @Override
    protected void closeGracefully0(
            final CompletionHandler<Closeable> completionHandler,
            final CloseReason closeReason) {
        terminate0(completionHandler, closeReason);
    }


    @Override
    protected void terminate0(final CompletionHandler<Closeable> completionHandler,
            final CloseReason closeReason) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("UDPNIOServerConnection might be only closed by calling unbind().");
        }

        if (completionHandler != null) {
            completionHandler.completed(this);
        }
    }
    
    public void unbind(
            final CompletionHandler<Closeable> completionHandler) {
        super.terminate0(completionHandler, CloseReason.LOCALLY_CLOSED_REASON);
    }

    @Override
    protected void preClose() {
        transport.unbind(this);
        super.preClose();
    }


}
