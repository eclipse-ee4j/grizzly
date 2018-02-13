/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.streams;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.memory.CompositeBuffer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author oleksiys
 */
public abstract class BufferedOutput implements Output {

    protected static final Integer ZERO = 0;
    protected static final GrizzlyFuture<Integer> ZERO_READY_FUTURE =
            ReadyFutureImpl.create(0);
    
    protected final int bufferSize;
    protected CompositeBuffer multiBufferWindow;
    private Buffer buffer;
    private int lastSlicedPosition;
    
    protected final AtomicBoolean isClosed = new AtomicBoolean();

    public BufferedOutput() {
        this(8192);
    }

    public BufferedOutput(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected abstract void onClosed() throws IOException;

    protected abstract GrizzlyFuture<Integer> flush0(final Buffer buffer,
            final CompletionHandler<Integer> completionHandler)
            throws IOException;

    protected abstract Buffer newBuffer(int size);

    protected abstract Buffer reallocateBuffer(Buffer oldBuffer, int size);

    @Override
    public void write(byte data) throws IOException {
        ensureBufferCapacity(1);
        buffer.put(data);
    }

    @Override
    public void write(Buffer bufferToWrite) throws IOException {
        if (multiBufferWindow == null) {
            multiBufferWindow = CompositeBuffer.newBuffer();
        }

        final boolean isInternalBufferEmpty = buffer == null ||
                (buffer.position() - lastSlicedPosition) == 0;
        
        if (!isInternalBufferEmpty) {
            final Buffer slice =
                    buffer.slice(lastSlicedPosition, buffer.position());
            
            multiBufferWindow.append(slice);

            lastSlicedPosition = buffer.position();
        }

        multiBufferWindow.append(bufferToWrite);
        ensureBufferCapacity(0);
    }

    @Override
    public boolean isBuffered() {
        return true;
    }

    @Override
    public Buffer getBuffer() {
        return buffer;
    }

    @Override
    public void ensureBufferCapacity(final int size) throws IOException {
        if (size > bufferSize) {
            throw new IllegalArgumentException("Size exceeds max size limit: " + bufferSize);
        }

        if (getBufferedSize() >= bufferSize) {
            overflow(null);
        }

        if (size == 0) return;
        
        if (buffer != null) {
            final int bufferRemaining = buffer.remaining();
            if (bufferRemaining < size) {
                overflow(null);
                ensureBufferCapacity(size);
            }
        } else {
            buffer = newBuffer(bufferSize);
        }
    }

    private GrizzlyFuture<Integer> overflow(
            final CompletionHandler<Integer> completionHandler)
            throws IOException {
        if (multiBufferWindow != null) {
            if (buffer != null && buffer.position() > lastSlicedPosition) {
                final Buffer slice =
                        buffer.slice(lastSlicedPosition, buffer.position());

                lastSlicedPosition = buffer.position();
                multiBufferWindow.append(slice);
            }

            final GrizzlyFuture<Integer> future = flush0(multiBufferWindow,
                    completionHandler);

            if (future.isDone()) {
                multiBufferWindow.removeAll();
                multiBufferWindow.clear();
                if (buffer != null) {
                    if (!buffer.isComposite()) {
                        buffer.clear();
                    } else {
                        buffer = null;
                    }
                    lastSlicedPosition = 0;
                }
            } else {
                multiBufferWindow = null;
                buffer = null;
                lastSlicedPosition = 0;
            }
            
            return future;
        } else if (buffer != null && buffer.position() > 0) {
            buffer.flip();

            final GrizzlyFuture<Integer> future = flush0(buffer,
                    completionHandler);
            if (future.isDone() && !buffer.isComposite()) {
                buffer.clear();
            } else {
                buffer = null;
            }

            return future;
        }
        
        return flush0(null, completionHandler);
    }

    @Override
    public GrizzlyFuture<Integer> flush(CompletionHandler<Integer> completionHandler)
            throws IOException {
        return overflow(completionHandler);
    }

    @Override
    public GrizzlyFuture<Integer> close(
            final CompletionHandler<Integer> completionHandler)
            throws IOException {

        if (!isClosed.getAndSet(true) && buffer != null && buffer.position() > 0) {
            final FutureImpl<Integer> future = SafeFutureImpl.create();

            try {
                overflow(new CompletionHandler<Integer>() {

                    @Override
                    public void cancelled() {
                        close(ZERO);
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        close(ZERO);
                    }

                    @Override
                    public void completed(Integer result) {
                        close(result);
                    }

                    @Override
                    public void updated(Integer result) {
                    }

                    public void close(Integer result) {
                        try {
                            onClosed();
                        } catch (IOException ignored) {
                        } finally {
                            if (completionHandler != null) {
                                completionHandler.completed(result);
                            }

                            future.result(result);
                        }
                    }
                });
            } catch (IOException ignored) {
            }

            return future;
        } else {
            if (completionHandler != null) {
                completionHandler.completed(ZERO);
            }

            return ZERO_READY_FUTURE;
        }
    }

    protected int getBufferedSize() {
        int size = 0;
        
        if (multiBufferWindow != null) {
            size = multiBufferWindow.remaining();
        }

        if (buffer != null) {
            size += buffer.position() - lastSlicedPosition;
        }

        return size;
    }
}
