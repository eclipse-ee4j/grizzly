/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

import java.util.concurrent.Future;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Copyable;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.impl.UnsafeFutureImpl;

/**
 * Set of {@link Future} utilities.
 *
 * @author Alexey Stashok
 */
public class Futures {

    /**
     * Returns thread-safe {@link FutureImpl} implementation. (Based on the JDK {@link java.util.concurrent.FutureTask}).
     *
     * @return thread-safe {@link FutureImpl} implementation.
     */
    public static <R> FutureImpl<R> createSafeFuture() {
        return SafeFutureImpl.create();
    }

    /**
     * Returns non thread-safe {@link FutureImpl} implementation.
     *
     * @return non thread-safe {@link FutureImpl} implementation.
     */
    public static <R> FutureImpl<R> createUnsafeFuture() {
        return UnsafeFutureImpl.create();
    }

    /**
     * Create a {@link Future}, which has a preset result.
     *
     * @param result the result
     * @return a {@link Future}, which has a preset result.
     */
    public static <R> GrizzlyFuture<R> createReadyFuture(final R result) {
        return ReadyFutureImpl.create(result);
    }

    /**
     * Create a {@link Future}, which has a preset failure.
     *
     * @param error the failure
     * @return a {@link Future}, which has a preset failure.
     */
    public static <R> GrizzlyFuture<R> createReadyFuture(final Throwable error) {
        return ReadyFutureImpl.create(error);
    }

    /**
     * Complete passed {@link FutureImpl} and {@link CompletionHandler} using the passed result object.
     *
     * @param future {@link FutureImpl} to be notified
     * @param completionHandler {@link CompletionHandler} to be notified
     * @param result the result
     */
    public static <R> void notifyResult(final FutureImpl<R> future, final CompletionHandler<R> completionHandler, final R result) {
        if (completionHandler != null) {
            completionHandler.completed(result);
        }

        if (future != null) {
            future.result(result);
        }
    }

    /**
     * Complete passed {@link FutureImpl} and {@link CompletionHandler} using the passed error
     *
     * @param future {@link FutureImpl} to be notified
     * @param completionHandler {@link CompletionHandler} to be notified
     * @param error the error.
     */
    public static <R> void notifyFailure(final FutureImpl<R> future, final CompletionHandler completionHandler, final Throwable error) {
        if (completionHandler != null) {
            completionHandler.failed(error);
        }

        if (future != null) {
            future.failure(error);
        }
    }

    /**
     * Complete passed {@link FutureImpl} and {@link CompletionHandler} via the cancellation notification.
     *
     * @param future {@link FutureImpl} to be notified
     * @param completionHandler {@link CompletionHandler} to be notified
     */
    public static <R> void notifyCancel(final FutureImpl<R> future, final CompletionHandler completionHandler) {
        if (completionHandler != null) {
            completionHandler.cancelled();
        }

        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Creates {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl}. All the notifications
     * coming to the returned {@link CompletionHandler} will be passed to the passed {@link FutureImpl}.
     *
     * @return {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl}. All the notifications
     * coming to the returned {@link CompletionHandler} will be passed to the passed {@link FutureImpl}.
     */
    public static <R> CompletionHandler<R> toCompletionHandler(final FutureImpl<R> future) {
        return new FutureToCompletionHandler<>(future);
    }

    /**
     * Creates {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl} and
     * {@link CompletionHandler} objects. All the notifications coming to the returned {@link CompletionHandler} will be
     * passed to the {@link FutureImpl} and {@link CompletionHandler} passed as parameters.
     *
     * @return {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl} and
     * {@link CompletionHandler} objects. All the notifications coming to the returned {@link CompletionHandler} will be
     * passed to the {@link FutureImpl} and {@link CompletionHandler} passed as parameters.
     */
    public static <R> CompletionHandler<R> toCompletionHandler(final FutureImpl<R> future, final CompletionHandler<R> completionHandler) {
        return new CompletionHandlerAdapter<>(future, completionHandler);
    }

    /**
     * Creates {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl}. All the notifications
     * coming to the returned {@link CompletionHandler} will be <tt>adapted</tt> using {@link GenericAdapter} and passed to
     * the {@link FutureImpl}.
     *
     * @return {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl}. All the notifications
     * coming to the returned {@link CompletionHandler} will be <tt>adapted</tt> using {@link GenericAdapter} and passed to
     * the {@link FutureImpl}.
     */
    public static <A, B> CompletionHandler<B> toAdaptedCompletionHandler(final FutureImpl<A> future, final GenericAdapter<B, A> adapter) {
        return toAdaptedCompletionHandler(future, null, adapter);
    }

    /**
     * Creates {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl} and
     * {@link CompletionHandler}. All the notifications coming to the returned {@link CompletionHandler} will be
     * <tt>adapted</tt> using {@link GenericAdapter} and passed to the {@link FutureImpl} and {@link CompletionHandler}.
     *
     * @return {@link CompletionHandler}, which may serve as a bridge for passed {@link FutureImpl} and
     * {@link CompletionHandler}. All the notifications coming to the returned {@link CompletionHandler} will be
     * <tt>adapted</tt> using {@link GenericAdapter} and passed to the {@link FutureImpl} and {@link CompletionHandler}.
     */
    public static <A, B> CompletionHandler<B> toAdaptedCompletionHandler(final FutureImpl<A> future, final CompletionHandler<A> completionHandler,
            final GenericAdapter<B, A> adapter) {
        return new CompletionHandlerAdapter<>(future, completionHandler, adapter);
    }

    private static final class FutureToCompletionHandler<E> extends EmptyCompletionHandler<E> {

        private final FutureImpl<E> future;

        public FutureToCompletionHandler(FutureImpl<E> future) {
            this.future = future;
        }

        @Override
        public void cancelled() {
            future.cancel(false);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void completed(E result) {
            if (result instanceof Copyable) {
                result = (E) ((Copyable) result).copy();
            }
            future.result(result);
        }

        @Override
        public void failed(final Throwable throwable) {
            future.failure(throwable);
        }
    }
}
