/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.nio.NIOTransport;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.util.Random;

/**
 * @since 2.2.19
 */
public abstract class AbstractBindingHandler implements SocketBinder {
    protected static final Random RANDOM = new Random();
    protected final NIOTransport transport;
    protected Processor processor;
    protected ProcessorSelector processorSelector;

    // ------------------------------------------------------------ Constructors


    public AbstractBindingHandler(final NIOTransport transport) {
        this.transport = transport;
        this.processor = transport.getProcessor();
        this.processorSelector = transport.getProcessorSelector();
    }


    // ---------------------------------------------------------- Public Methods


    /**
     * Get the default {@link Processor} to process {@link IOEvent}, occurring
     * on connection phase.
     *
     * @return the default {@link Processor} to process {@link IOEvent},
     *         occurring on connection phase.
     */
    public Processor getProcessor() {
        return processor;
    }

    /**
     * Set the default {@link Processor} to process {@link IOEvent}, occurring
     * on connection phase.
     *
     * @param processor the default {@link Processor} to process
     *                  {@link IOEvent}, occurring on connection phase.
     */
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    /**
     * Gets the default {@link ProcessorSelector}, which will be used to get
     * {@link Processor} to process I/O events, occurring on connection phase.
     *
     * @return the default {@link ProcessorSelector}, which will be used to get
     *         {@link Processor} to process I/O events, occurring on connection phase.
     */
    public ProcessorSelector getProcessorSelector() {
        return processorSelector;
    }

    /**
     * Sets the default {@link ProcessorSelector}, which will be used to get
     * {@link Processor} to process I/O events, occurring on connection phase.
     *
     * @param processorSelector the default {@link ProcessorSelector},
     *                          which will be used to get {@link Processor} to process I/O events,
     *                          occurring on connection phase.
     */
    public void setProcessorSelector(final ProcessorSelector processorSelector) {
        this.processorSelector = processorSelector;
    }


    // ----------------------------------------------- Methods from SocketBinder

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection<?> bind(final int port) throws IOException {
        return bind(new InetSocketAddress(port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection<?> bind(final String host, final int port) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection<?> bind(final String host, final int port, final int backlog)
            throws IOException {
        return bind(new InetSocketAddress(host, port), backlog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection<?> bind(final String host, final PortRange portRange,
            final int backlog) throws IOException {
        return bind(host, portRange, true, backlog);
    }

    @Override
    public Connection<?> bind(final String host, final PortRange portRange,
            boolean randomStartPort, final int backlog) throws IOException {
        // Get the initial range parameters
        final int lower = portRange.getLower();
        final int range = portRange.getUpper() - lower + 1;

        // Select a start point in the range
        final int initialOffset;
        if (randomStartPort) {
            initialOffset = RANDOM.nextInt(range);
        } else {
            initialOffset = 0;
        }

        // Loop the offset through all ports in the range and attempt
        // to bind to each
        int offset = initialOffset;
        do {
            final int port = lower + offset;
            try {
                return bind(host, port, backlog);
            } catch (IOException caught) {
                // Swallow exceptions until the end
            }
            offset = (offset + 1) % range;
        } while (offset != initialOffset);

        // If a port can't be bound, throw the exception
        throw new BindException(String.format("Couldn't bind to any port in the range `%s`.", portRange.toString()));
    }

    /**
     * This operation is not supported by implementations of {@link AbstractBindingHandler}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public final void unbindAll() {
        throw new UnsupportedOperationException();
    }


    // ------------------------------------------------------- Protected Methods


    @SuppressWarnings("unchecked")
    protected <T> T getSystemInheritedChannel(final Class<?> channelType)
    throws IOException {
        final Channel inheritedChannel = System.inheritedChannel();

        if (inheritedChannel == null) {
            throw new IOException("Inherited channel is not set");
        }
        if (!(channelType.isInstance(inheritedChannel))) {
            throw new IOException("Inherited channel is not "
                    + channelType.getName()
                    + ", but "
                    + inheritedChannel.getClass().getName());
        }
        return (T) inheritedChannel;
    }


    // ----------------------------------------------------------- Inner Classes

    /**
     * Builder
     *
     * @param <E>
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<E extends Builder> {

        protected Processor processor;
        protected ProcessorSelector processorSelector;

        public E processor(final Processor processor) {
            this.processor = processor;
            return (E) this;
        }

        public E processorSelector(final ProcessorSelector processorSelector) {
            this.processorSelector = processorSelector;
            return (E) this;
        }

        public AbstractBindingHandler build() {
            AbstractBindingHandler bindingHandler = create();
            if (processor != null) {
                bindingHandler.setProcessor(processor);
            }
            if (processorSelector != null) {
                bindingHandler.setProcessorSelector(processorSelector);
            }
            return bindingHandler;
        }

        protected abstract AbstractBindingHandler create();

    }
}
