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

package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.impl.FutureImpl;

/**
 *
 * @author Alexey Stashok
 */
public class CompletionHandlerAdapter<A, B> implements CompletionHandler<B> {

    private final static GenericAdapter DIRECT_ADAPTER = new GenericAdapter() {
        @Override
        public Object adapt(final Object result) {
            return result;
        }
    };

    private final GenericAdapter<B, A> adapter;
    private final FutureImpl<A> future;
    private final CompletionHandler<A> completionHandler;

    public CompletionHandlerAdapter(FutureImpl<A> future) {
        this(future, null);
    }

    public CompletionHandlerAdapter(FutureImpl<A> future, CompletionHandler<A> completionHandler) {
        this(future, completionHandler, null);
    }

    public CompletionHandlerAdapter(FutureImpl<A> future, CompletionHandler<A> completionHandler, GenericAdapter<B, A> adapter) {
        this.future = future;
        this.completionHandler = completionHandler;
        if (adapter != null) {
            this.adapter = adapter;
        } else {
            this.adapter = getDirectAdapter();
        }
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
    public void completed(B result) {
        final A adaptedResult = adapt(result);

        if (completionHandler != null) {
            completionHandler.completed(adaptedResult);
        }

        if (future != null) {
            future.result(adaptedResult);
        }
    }

    @Override
    public void updated(B result) {
        final A adaptedResult = adapt(result);

        if (completionHandler != null) {
            completionHandler.updated(adaptedResult);
        }
    }

    protected A adapt(B result) {
        return adapter.adapt(result);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> GenericAdapter<K, V> getDirectAdapter() {
        return DIRECT_ADAPTER;
    }
}
