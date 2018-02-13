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
import java.net.SocketAddress;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.streams.AbstractStreamWriter;
import org.glassfish.grizzly.streams.BufferedOutput;

/**
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public final class DefaultStreamWriter extends AbstractStreamWriter {
    public DefaultStreamWriter(Connection connection) {
        super(connection, new Output(connection));
    }

    @Override
    public GrizzlyFuture<Integer> flush(
            final CompletionHandler<Integer> completionHandler)
            throws IOException {
        return super.flush(new ResetCounterCompletionHandler(
                (Output) output, completionHandler));
    }

    public final static class Output extends BufferedOutput {
        private final Connection connection;
        private int sentBytesCounter;

        public Output(Connection connection) {
            super(connection.getWriteBufferSize());
            this.connection = connection;
        }


        @Override
        protected GrizzlyFuture<Integer> flush0(Buffer buffer,
                final CompletionHandler<Integer> completionHandler)
                throws IOException {
            
            final FutureImpl<Integer> future = SafeFutureImpl.create();
            
            if (buffer == null) {
                buffer = Buffers.EMPTY_BUFFER;
            }

            connection.write(buffer,
                    new CompletionHandlerAdapter(this, future, completionHandler));
            return future;
        }

        @Override
        protected Buffer newBuffer(int size) {
            return connection.getMemoryManager().allocate(size);
        }

        @Override
        protected Buffer reallocateBuffer(Buffer oldBuffer, int size) {
            return connection.getMemoryManager().reallocate(oldBuffer, size);
        }

        @Override
        protected void onClosed() throws IOException {
            connection.closeSilently();
        }
    }

    private final static class CompletionHandlerAdapter
            implements CompletionHandler<WriteResult<Buffer, SocketAddress>> {

        private final Output output;
        private final FutureImpl<Integer> future;
        private final CompletionHandler<Integer> completionHandler;

        public CompletionHandlerAdapter(Output output,
                FutureImpl<Integer> future,
                CompletionHandler<Integer> completionHandler) {
            this.output = output;
            this.future = future;
            this.completionHandler = completionHandler;
        }

        @Override
        public void cancelled() {
            if (completionHandler != null) {
                completionHandler.cancelled();
            }

            if (future != null) {
                future.cancel(false);
            }
        }

        @Override
        public void failed(Throwable throwable) {
            if (completionHandler != null) {
                completionHandler.failed(throwable);
            }

            if (future != null) {
                future.failure(throwable);
            }
        }

        @Override
        public void completed(WriteResult result) {
            output.sentBytesCounter += result.getWrittenSize();
            int totalSentBytes = output.sentBytesCounter;

            if (completionHandler != null) {
                completionHandler.completed(totalSentBytes);
            }

            if (future != null) {
                future.result(totalSentBytes);
            }
        }

        @Override
        public void updated(WriteResult result) {
            if (completionHandler != null) {
                completionHandler.updated(output.sentBytesCounter
                        + (int) result.getWrittenSize());
            }
        }
    }

    private final static class ResetCounterCompletionHandler
            implements CompletionHandler<Integer> {

        private final Output output;
        private final CompletionHandler<Integer> parentCompletionHandler;

        public ResetCounterCompletionHandler(Output output,
                CompletionHandler<Integer> parentCompletionHandler) {
            this.output = output;
            this.parentCompletionHandler = parentCompletionHandler;
        }

        @Override
        public void cancelled() {
            if (parentCompletionHandler != null) {
                parentCompletionHandler.cancelled();
            }
        }

        @Override
        public void failed(Throwable throwable) {
            if (parentCompletionHandler != null) {
                parentCompletionHandler.failed(throwable);
            }
        }

        @Override
        public void completed(Integer result) {
            output.sentBytesCounter = 0;
            if (parentCompletionHandler != null) {
                parentCompletionHandler.completed(result);
            }
        }

        @Override
        public void updated(Integer result) {
            if (parentCompletionHandler != null) {
                parentCompletionHandler.updated(result);
            }
        }
    }
}
