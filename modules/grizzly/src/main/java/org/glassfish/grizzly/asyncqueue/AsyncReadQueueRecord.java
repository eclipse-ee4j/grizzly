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

package org.glassfish.grizzly.asyncqueue;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Interceptor;
import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.utils.DebugPoint;

/**
 * {@link AsyncQueue} read element unit
 *
 * @author Alexey Stashok
 */
public final class AsyncReadQueueRecord extends AsyncQueueRecord<ReadResult> {
    private static final ThreadCache.CachedTypeIndex<AsyncReadQueueRecord> CACHE_IDX = ThreadCache.obtainIndex(AsyncReadQueueRecord.class, 2);

    public static AsyncReadQueueRecord create(final Connection connection, final Buffer message, final CompletionHandler completionHandler,
            final Interceptor<ReadResult> interceptor) {

        final AsyncReadQueueRecord asyncReadQueueRecord = ThreadCache.takeFromCache(CACHE_IDX);

        if (asyncReadQueueRecord != null) {
            asyncReadQueueRecord.isRecycled = false;

            asyncReadQueueRecord.set(connection, message, completionHandler, interceptor);
            return asyncReadQueueRecord;
        }

        return new AsyncReadQueueRecord(connection, message, completionHandler, interceptor);
    }

    protected Interceptor interceptor;
    private final RecordReadResult readResult = new RecordReadResult();

    private AsyncReadQueueRecord(final Connection connection, final Buffer message, final CompletionHandler completionHandler,
            final Interceptor<ReadResult> interceptor) {

        set(connection, message, completionHandler, interceptor);
    }

    public Interceptor getInterceptor() {
        checkRecycled();
        return interceptor;
    }

    @SuppressWarnings("unchecked")
    public void notifyComplete() {
        if (completionHandler != null) {
            completionHandler.completed(readResult);
        }
    }

    public boolean isFinished() {
        return readResult.getReadSize() > 0 || !((Buffer) message).hasRemaining();
    }

    @Override
    public ReadResult getCurrentResult() {
        return readResult;
    }

    @SuppressWarnings("unchecked")
    protected void set(final Connection connection, final Object message, final CompletionHandler completionHandler, final Interceptor interceptor) {
        set(connection, message, completionHandler);
        this.interceptor = interceptor;

        readResult.set(connection, message, null, 0);
    }

    protected void reset() {
        set(null, null, null);
        readResult.recycle(); // reset the ReadResult
        interceptor = null;
    }

    @Override
    public void recycle() {
        checkRecycled();

        reset();
        isRecycled = true;
        if (Grizzly.isTrackingThreadCache()) {
            recycleTrack = new DebugPoint(new Exception(), Thread.currentThread().getName());
        }

        ThreadCache.putToCache(CACHE_IDX, this);
    }
}
