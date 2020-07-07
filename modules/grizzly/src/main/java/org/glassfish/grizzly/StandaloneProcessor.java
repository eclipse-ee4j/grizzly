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

package org.glassfish.grizzly;

import java.net.Socket;

import org.glassfish.grizzly.asyncqueue.AsyncQueueEnabledTransport;
import org.glassfish.grizzly.asyncqueue.AsyncQueueReader;
import org.glassfish.grizzly.asyncqueue.AsyncQueueWriter;
import org.glassfish.grizzly.asyncqueue.MessageCloner;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.nio.transport.DefaultStreamReader;
import org.glassfish.grizzly.nio.transport.DefaultStreamWriter;
import org.glassfish.grizzly.streams.StreamReader;
import org.glassfish.grizzly.streams.StreamWriter;

/**
 * {@link Processor}, which is not interested in processing I/O events. {@link Connection} lifecycle should be managed
 * explicitly, using read/write/accept/connect methods.
 *
 * This {@link Processor} could be set on {@link Connection} to avoid it from being processed by {@link FilterChain} or
 * other {@link Processor}. In this case {@link Connection} could be used like regular Java {@link Socket}.
 *
 * @author Alexey Stashok
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class StandaloneProcessor implements Processor {
    public static final StandaloneProcessor INSTANCE = new StandaloneProcessor();

    /**
     * This method should never be called, because {@link StandaloneProcessor#isInterested(IOEvent)} returns false for any
     * {@link IOEvent}.
     */
    @Override
    public ProcessorResult process(final Context context) {
        final IOEvent iOEvent = context.getIoEvent();
        if (iOEvent == IOEvent.READ) {
            final Connection connection = context.getConnection();
            final AsyncQueueReader reader = ((AsyncQueueEnabledTransport) connection.getTransport()).getAsyncQueueIO().getReader();

            return reader.processAsync(context).toProcessorResult();
        } else if (iOEvent == IOEvent.WRITE) {
            final Connection connection = context.getConnection();
            final AsyncQueueWriter writer = ((AsyncQueueEnabledTransport) connection.getTransport()).getAsyncQueueIO().getWriter();

            return writer.processAsync(context).toProcessorResult();
        }

        throw new IllegalStateException("Unexpected IOEvent=" + iOEvent);
    }

    /**
     * {@link StandaloneProcessor} is not interested in any {@link IOEvent}.
     */
    @Override
    public boolean isInterested(IOEvent ioEvent) {
        return ioEvent == IOEvent.READ || ioEvent == IOEvent.WRITE;
    }

    /**
     * Method does nothing.
     */
    @Override
    public void setInterested(IOEvent ioEvent, boolean isInterested) {
    }

    @Override
    public Context obtainContext(final Connection connection) {
        final Context context = Context.create(connection);
        context.setProcessor(this);
        return context;
    }

    /**
     * Get the {@link Connection} {@link StreamReader}, to read data from the {@link Connection}.
     *
     * @param connection {@link Connection} to get the {@link StreamReader} for
     * @return the {@link Connection} {@link StreamReader}, to read data from the {@link Connection}.
     */
    public StreamReader getStreamReader(Connection connection) {
        return new DefaultStreamReader(connection);
    }

    /**
     * Get the {@link Connection} {@link StreamWriter}, to write data to the {@link Connection}.
     *
     * @param connection connection to get the {@link StreamWriter} for
     * @return the {@link Connection} {@link StreamWriter}, to write data to the {@link Connection}.
     */
    public StreamWriter getStreamWriter(Connection connection) {
        return new DefaultStreamWriter(connection);
    }

    @Override
    public void read(Connection connection, CompletionHandler completionHandler) {

        final Transport transport = connection.getTransport();
        transport.getReader(connection).read(connection, null, completionHandler);
    }

    @Override
    public void write(final Connection connection, final Object dstAddress, final Object message, final CompletionHandler completionHandler) {
        write(connection, dstAddress, message, completionHandler, (MessageCloner) null);
    }

    @Override
    public void write(Connection connection, Object dstAddress, Object message, CompletionHandler completionHandler, MessageCloner messageCloner) {

        final Transport transport = connection.getTransport();

        transport.getWriter(connection).write(connection, dstAddress, (Buffer) message, completionHandler, messageCloner);
    }

    @Override
    @Deprecated
    public void write(Connection connection, Object dstAddress, Object message, CompletionHandler completionHandler,
            org.glassfish.grizzly.asyncqueue.PushBackHandler pushBackHandler) {

        final Transport transport = connection.getTransport();

        transport.getWriter(connection).write(connection, dstAddress, (Buffer) message, completionHandler, pushBackHandler);
    }
}
