/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

/**
 *
 * @author oleksiys
 */
public final class ReusableFuture<V> implements FutureImpl<V> {
    private volatile FutureImpl<V> innerFuture;

    public ReusableFuture() {
        reset();
    }

    @Override
    public void addCompletionHandler(final CompletionHandler<V> completionHandler) {
        innerFuture.addCompletionHandler(completionHandler);
    }

    protected void reset() {
        innerFuture = Futures.createSafeFuture();
    }

    @Override
    public void result(V result) {
        innerFuture.result(result);
    }

    @Override
    public void failure(Throwable failure) {
        innerFuture.failure(failure);
    }

    @Override
    public void recycle(boolean recycleResult) {
        innerFuture.recycle(recycleResult);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return innerFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return innerFuture.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return innerFuture.get(timeout, unit);
    }

    @Override
    public void recycle() {
        innerFuture = null;
    }

    @Override
    public V getResult() {
        return innerFuture.getResult();
    }

    @Override
    public void markForRecycle(boolean recycleResult) {
        innerFuture.markForRecycle(recycleResult);
    }
}
