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

package org.glassfish.grizzly.nio.tmpselectors;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.glassfish.grizzly.*;
import org.glassfish.grizzly.nio.NIOConnection;

/**
 *
 * @author oleksiys
 */
public abstract class TemporarySelectorReader
        extends AbstractReader<SocketAddress> {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    protected final TemporarySelectorsEnabledTransport transport;

    public TemporarySelectorReader(
            TemporarySelectorsEnabledTransport transport) {
        this.transport = transport;
    }

    @Override
    public void read(
            Connection<SocketAddress> connection, Buffer message,
            CompletionHandler<ReadResult<Buffer, SocketAddress>> completionHandler,
            Interceptor<ReadResult> interceptor) {
        read(connection, message, completionHandler,
                interceptor,
                connection.getReadTimeout(TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    /**
     * Method reads data to the <tt>message</tt>.
     *
     * @param connection the {@link Connection} to read from
     * @param message the message, where data will be read
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when read will be completed
     * @param interceptor intercept to invoke on operation
     * @param timeout operation timeout value value
     * @param timeunit the timeout unit
     * @return {@link Future}, using which it's possible to check the result
     * @throws java.io.IOException
     */
    public void read(
            final Connection<SocketAddress> connection, final Buffer message,
            final CompletionHandler<ReadResult<Buffer, SocketAddress>> completionHandler,
            final Interceptor<ReadResult> interceptor,
            final long timeout, final TimeUnit timeunit) {

        if (connection == null || !(connection instanceof NIOConnection)) {
            failure(new IllegalStateException(
                    "Connection should be NIOConnection and cannot be null"),
                    completionHandler);
            return;
        }

        final NIOConnection nioConnection = (NIOConnection) connection;
        
        final ReadResult<Buffer, SocketAddress> currentResult =
                ReadResult.create(connection, message, null, 0);

        try {
            final int readBytes = read0(nioConnection, interceptor,
                    currentResult, message, timeout, timeunit);

            if (readBytes > 0) {

                if (completionHandler != null) {
                    completionHandler.completed(currentResult);
                }

                if (interceptor != null) {
                    interceptor.intercept(COMPLETE_EVENT, connection, currentResult);
                }
            } else {
                failure(new TimeoutException(), completionHandler);
            }
        } catch (IOException e) {
            failure(e, completionHandler);
        }
    }
    
    private int read0(NIOConnection connection,
            Interceptor<ReadResult> interceptor,
            ReadResult<Buffer, SocketAddress> currentResult, Buffer buffer,
            long timeout, TimeUnit timeunit) throws IOException {

        boolean isCompleted = false;
        while (!isCompleted) {
            isCompleted = true;
            final int readBytes = read0(connection, currentResult,
                    buffer, timeout, timeunit);

            if (readBytes <= 0) {
                return -1;
            } else {
                if (interceptor != null) {
                    isCompleted = (interceptor.intercept(Reader.READ_EVENT,
                            null, currentResult) & Interceptor.COMPLETED) != 0;
                }
            }
        }

        return currentResult.getReadSize();
    }

    protected final int read0(final NIOConnection connection,
            final ReadResult<Buffer, SocketAddress> currentResult,
            final Buffer buffer, final long timeout, final TimeUnit timeunit)
            throws IOException {

        int bytesRead;

        Selector readSelector = null;
        SelectionKey key = null;
        final SelectableChannel channel = connection.getChannel();
        final long readTimeout = TimeUnit.MILLISECONDS.convert(
                ((timeout < 0) ? 0 : timeout), timeunit);

        try {
            bytesRead = readNow0(connection, buffer, currentResult);

            if (bytesRead == 0) {
                readSelector = transport.getTemporarySelectorIO().
                        getSelectorPool().poll();

                if (readSelector == null) {
                    return bytesRead;
                }

                key = channel.register(readSelector, SelectionKey.OP_READ);
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                int code = readSelector.select(readTimeout);
                key.interestOps(
                        key.interestOps() & (~SelectionKey.OP_READ));

                if (code == 0) {
                    return bytesRead; // Return on the main Selector and try again.
                }

                bytesRead = readNow0(connection, buffer, currentResult);
            }

            if (bytesRead == -1) {
                throw new EOFException();
            }
        } finally {
            transport.getTemporarySelectorIO().recycleTemporaryArtifacts(
                    readSelector, key);
        }

        return bytesRead;
    }

    protected abstract int readNow0(NIOConnection connection,
            Buffer buffer, ReadResult<Buffer, SocketAddress> currentResult)
            throws IOException;

    protected Buffer acquireBuffer(Connection connection) {
        Transport connectionTransport = connection.getTransport();
        return connectionTransport.getMemoryManager().
                allocate(DEFAULT_BUFFER_SIZE);
    }

    public TemporarySelectorsEnabledTransport getTransport() {
        return transport;
    }
    
    private static void failure(
            final Throwable failure,
            final CompletionHandler<ReadResult<Buffer, SocketAddress>> completionHandler) {
        if (completionHandler != null) {
            completionHandler.failed(failure);
        }
    }    
}
