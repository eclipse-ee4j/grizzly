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

package org.glassfish.grizzly.streams;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.utils.conditions.Condition;

/**
 *
 * @author Alexey Stashok
 */
public abstract class BufferedInput implements Input {

    protected final CompositeBuffer compositeBuffer;
    private volatile boolean isClosed;

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected boolean isCompletionHandlerRegistered;
    protected Exception registrationStackTrace; // used for debugging problems

    protected Condition condition;
    protected CompletionHandler<Integer> completionHandler;
    protected FutureImpl<Integer> future;

    public BufferedInput() {
        compositeBuffer = CompositeBuffer.newBuffer();
    }

    protected abstract void onOpenInputSource() throws IOException;

    protected abstract void onCloseInputSource() throws IOException;

    public boolean append(final Buffer buffer) {
        if (buffer == null) {
            return false;
        }

        lock.writeLock().lock();

        try {
            if (isClosed) {
                buffer.dispose();
            } else {
                final int addSize = buffer.remaining();
                if (addSize > 0) {
                    compositeBuffer.append(buffer);
                }
                notifyUpdate();
            }
        } finally {
            lock.writeLock().unlock();
        }

        return true;
    }

    public boolean prepend(final Buffer buffer) {
        if (buffer == null) {
            return false;
        }

        lock.writeLock().lock();

        try {
            if (isClosed) {
                buffer.dispose();
            } else {
                final int addSize = buffer.remaining();
                if (addSize > 0) {
                    compositeBuffer.prepend(buffer);
                }

                notifyUpdate();
            }
        } finally {
            lock.writeLock().unlock();
        }

        return true;
    }

    @Override
    public byte read() throws IOException {
        final byte result = compositeBuffer.get();
        compositeBuffer.shrink();
        return result;
    }

    @Override
    public void skip(int length) {
        if (length > size()) {
            throw new IllegalStateException("Can not skip more bytes than available");
        }

        compositeBuffer.position(compositeBuffer.position() + length);
        compositeBuffer.shrink();
    }

    @Override
    public final boolean isBuffered() {
        return true;
    }

    @Override
    public Buffer getBuffer() {
        return compositeBuffer;
    }

    @Override
    public Buffer takeBuffer() {
        final Buffer duplicate = compositeBuffer.duplicate();
        compositeBuffer.removeAll();
        return duplicate;
    }

    @Override
    public int size() {
        return compositeBuffer.remaining();
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (!isClosed) {
                isClosed = true;

                compositeBuffer.dispose();

                final CompletionHandler<Integer> localCompletionHandler = completionHandler;
                if (localCompletionHandler != null) {
                    completionHandler = null;
                    isCompletionHandlerRegistered = false;
                    notifyFailure(localCompletionHandler, new EOFException("Input is closed"));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public GrizzlyFuture<Integer> notifyCondition(Condition condition, CompletionHandler<Integer> completionHandler) {
        lock.writeLock().lock();

        try {
            if (!isCompletionHandlerRegistered) {
                if (condition.check()) {
                    notifyCompleted(completionHandler);
                    return ReadyFutureImpl.create(compositeBuffer.remaining());
                }

                registrationStackTrace = new Exception();
                isCompletionHandlerRegistered = true;
                this.completionHandler = completionHandler;
                final FutureImpl<Integer> localFuture = SafeFutureImpl.create();
                this.future = localFuture;
                this.condition = condition;

                try {
                    onOpenInputSource();
                } catch (IOException e) {
                    notifyFailure(completionHandler, e);
                    return ReadyFutureImpl.create(e);
                }

                return localFuture;
            } else {
                throw new IllegalStateException("Only one notificator could be registered. Previous registration came from: ", registrationStackTrace);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void notifyUpdate() {
        if (condition != null && condition.check()) {
            condition = null;
            final CompletionHandler<Integer> localCompletionHandler = completionHandler;
            completionHandler = null;

            final FutureImpl<Integer> localFuture = future;
            future = null;
            isCompletionHandlerRegistered = false;

            try {

                onCloseInputSource();
                notifyCompleted(localCompletionHandler);
                localFuture.result(compositeBuffer.remaining());
            } catch (IOException e) {
                notifyFailure(localCompletionHandler, e);
                localFuture.failure(e);
            }
        }
    }

    protected void notifyCompleted(final CompletionHandler<Integer> completionHandler) {
        if (completionHandler != null) {
            completionHandler.completed(compositeBuffer.remaining());
        }
    }

    protected void notifyFailure(final CompletionHandler<Integer> completionHandler, final Throwable failure) {
        if (completionHandler != null) {
            completionHandler.failed(failure);
        }
    }
}
