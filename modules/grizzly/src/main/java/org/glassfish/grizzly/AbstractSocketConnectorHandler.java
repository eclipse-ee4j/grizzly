/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Services Ltd.
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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;

/**
 * Abstract class simplifies the implementation of
 * {@link SocketConnectorHandler}
 * interface by pre-implementing some of its methods.
 * 
 * @author Alexey Stashok
 */
public abstract class AbstractSocketConnectorHandler
        implements SocketConnectorHandler {

    protected final Transport transport;
    private Processor processor;
    private ProcessorSelector processorSelector;

    protected final List<ConnectionProbe> probes =
            new LinkedList<ConnectionProbe>();

    public AbstractSocketConnectorHandler(Transport transport) {
        this.transport = transport;
        this.processor = transport.getProcessor();
        this.processorSelector = transport.getProcessorSelector();
    }

    @Override
    public GrizzlyFuture<Connection> connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    @Override
    public GrizzlyFuture<Connection> connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, (SocketAddress) null);
    }

    @Override
    public void connect(SocketAddress remoteAddress,
            CompletionHandler<Connection> completionHandler) {
        connect(remoteAddress, null, completionHandler);
    }

    @Override
    public GrizzlyFuture<Connection> connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        return connectAsync(remoteAddress, localAddress, null, true);
    }

    @Override
    public void connect(SocketAddress remoteAddress,
            SocketAddress localAddress,
            CompletionHandler<Connection> completionHandler) {
        connectAsync(remoteAddress, localAddress, completionHandler, false);
    }

    protected abstract FutureImpl<Connection> connectAsync(
            final SocketAddress remoteAddress,
            final SocketAddress localAddress,
            final CompletionHandler<Connection> completionHandler,
            final boolean needFuture);

    /**
     * Get the default {@link Processor} to process {@link IOEvent}, occurring
     * on connection phase.
     *
     * @return the default {@link Processor} to process {@link IOEvent},
     * occurring on connection phase.
     */
    public Processor getProcessor() {
        return processor;
    }

    /**
     * Set the default {@link Processor} to process {@link IOEvent}, occurring
     * on connection phase.
     *
     * @param processor the default {@link Processor} to process
     * {@link IOEvent}, occurring on connection phase.
     */
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    /**
     * Gets the default {@link ProcessorSelector}, which will be used to get
     * {@link Processor} to process I/O events, occurring on connection phase.
     *
     * @return the default {@link ProcessorSelector}, which will be used to get
     * {@link Processor} to process I/O events, occurring on connection phase.
     */
    public ProcessorSelector getProcessorSelector() {
        return processorSelector;
    }

    /**
     * Sets the default {@link ProcessorSelector}, which will be used to get
     * {@link Processor} to process I/O events, occurring on connection phase.
     *
     * @param processorSelector the default {@link ProcessorSelector},
     * which will be used to get {@link Processor} to process I/O events,
     * occurring on connection phase.
     */
    public void setProcessorSelector(ProcessorSelector processorSelector) {
        this.processorSelector = processorSelector;
    }

    /**
     * Add the {@link ConnectionProbe}, which will be notified about
     * <tt>Connection</tt> life-cycle events.
     *
     * @param probe the {@link ConnectionProbe}.
     */
    public void addMonitoringProbe(ConnectionProbe probe) {
        probes.add(probe);
    }

    /**
     * Remove the {@link ConnectionProbe}.
     *
     * @param probe the {@link ConnectionProbe}.
     * @return true if probe was in the list and is now removed
     */
    public boolean removeMonitoringProbe(ConnectionProbe probe) {
        return probes.remove(probe);
    }

    /**
     * Get the {@link ConnectionProbe}, which are registered on the <tt>Connection</tt>.
     * Please note, it's not appropriate to modify the returned array's content.
     * Please use {@link #addMonitoringProbe(org.glassfish.grizzly.ConnectionProbe)} and
     * {@link #removeMonitoringProbe(org.glassfish.grizzly.ConnectionProbe)} instead.
     *
     * @return the {@link ConnectionProbe}, which are registered on the <tt>Connection</tt>.
     */
    public ConnectionProbe[] getMonitoringProbes() {
        return probes.toArray(new ConnectionProbe[probes.size()]);
    }

    /**
     * Pre-configures {@link Connection} object before actual connecting phase
     * will be started.
     * 
     * @param connection {@link Connection} to pre-configure.
     */
    protected void preConfigure(Connection connection) {
    }

    protected FutureImpl<Connection> makeCancellableFuture(final Connection connection) {
        return new SafeFutureImpl<Connection>() {

            @Override
            protected void onComplete() {
                try {
                    if (!isCancelled()) {
                        get();
                        return;
                    }
                } catch (Throwable ignored) {
                }

                try {
                    connection.closeSilently();
                } catch (Exception ignored) {
                }
            }
        };
    }
    
    /**
     * Builder
     *
     * @param <E> itself
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<E extends Builder> {

        protected Processor processor;
        protected ProcessorSelector processorSelector;
        protected ConnectionProbe connectionProbe;

        public E processor(final Processor processor) {
            this.processor = processor;
            return (E) this;
        }

        public E processorSelector(final ProcessorSelector processorSelector) {
            this.processorSelector = processorSelector;
            return (E) this;
        }

        public E probe(ConnectionProbe connectionProbe) {
            this.connectionProbe = connectionProbe;
            return (E) this;
        }

        public AbstractSocketConnectorHandler build() {
            AbstractSocketConnectorHandler handler = create();
            if (processor != null) {
                handler.setProcessor(processor);
            }
            if (processorSelector != null) {
                handler.setProcessorSelector(processorSelector);
            }
            if (connectionProbe != null) {
                handler.addMonitoringProbe(connectionProbe);
            }
            return handler;
        }

        protected abstract AbstractSocketConnectorHandler create();
    }
}
