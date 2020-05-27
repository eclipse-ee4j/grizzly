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

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.Writer;
import org.glassfish.grizzly.utils.DebugPoint;

/**
 * {@link AsyncQueue} write element unit
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public class AsyncWriteQueueRecord extends AsyncQueueRecord<RecordWriteResult> {
    public final static int UNCOUNTABLE_RECORD_SPACE_VALUE = 1;

    private static final ThreadCache.CachedTypeIndex<AsyncWriteQueueRecord> CACHE_IDX = ThreadCache.obtainIndex(AsyncWriteQueueRecord.class,
            Writer.Reentrant.getMaxReentrants());

    public static AsyncWriteQueueRecord create(final Connection connection, final WritableMessage message, final CompletionHandler completionHandler,
            final Object dstAddress, final PushBackHandler pushbackHandler, final boolean isUncountable) {

        final AsyncWriteQueueRecord asyncWriteQueueRecord = ThreadCache.takeFromCache(CACHE_IDX);

        if (asyncWriteQueueRecord != null) {
            asyncWriteQueueRecord.isRecycled = false;
            asyncWriteQueueRecord.set(connection, message, completionHandler, dstAddress, pushbackHandler, isUncountable);

            return asyncWriteQueueRecord;
        }

        return new AsyncWriteQueueRecord(connection, message, completionHandler, dstAddress, pushbackHandler, isUncountable);
    }

    private long initialMessageSize;
    private boolean isUncountable;
    private Object dstAddress;
    private PushBackHandler pushBackHandler;

    private final RecordWriteResult writeResult = new RecordWriteResult();

    protected AsyncWriteQueueRecord(final Connection connection, final WritableMessage message, final CompletionHandler completionHandler,
            final Object dstAddress, final PushBackHandler pushBackHandler, final boolean isUncountable) {

        set(connection, message, completionHandler, dstAddress, pushBackHandler, isUncountable);
    }

    @SuppressWarnings("unchecked")
    protected void set(final Connection connection, final WritableMessage message, final CompletionHandler completionHandler, final Object dstAddress,
            final PushBackHandler pushBackHandler, final boolean isUncountable) {
        super.set(connection, message, completionHandler);

        this.dstAddress = dstAddress;
        this.isUncountable = isUncountable;
        this.initialMessageSize = message != null ? message.remaining() : 0;
        this.pushBackHandler = pushBackHandler;

        writeResult.set(connection, message, dstAddress, 0);
    }

    public final Object getDstAddress() {
        checkRecycled();
        return dstAddress;
    }

    public final WritableMessage getWritableMessage() {
        return (WritableMessage) message;
    }

    /**
     * @return <tt>true</tt> if record reserves in async write queue space, that is not related to message size
     * {@link #remaining()}, but is constant {@link AsyncWriteQueueRecord#UNCOUNTABLE_RECORD_SPACE_VALUE}.
     */
    public boolean isUncountable() {
        return isUncountable;
    }

    public void setUncountable(final boolean isUncountable) {
        this.isUncountable = isUncountable;
    }

    public long getBytesToReserve() {
        return isUncountable ? UNCOUNTABLE_RECORD_SPACE_VALUE : initialMessageSize;
    }

    public long getInitialMessageSize() {
        return initialMessageSize;
    }

    public long remaining() {
        return getWritableMessage().remaining();
    }

    @Override
    public RecordWriteResult getCurrentResult() {
        return writeResult;
    }

    @Deprecated
    public PushBackHandler getPushBackHandler() {
        return pushBackHandler;
    }

    public boolean canBeAggregated() {
        return !getWritableMessage().isExternal();
    }

    @SuppressWarnings("unchecked")
    public void notifyCompleteAndRecycle() {

        final CompletionHandler<WriteResult> completionHandlerLocal = completionHandler;

        final WritableMessage messageLocal = getWritableMessage();

        if (completionHandlerLocal != null) {
            completionHandlerLocal.completed(writeResult);
        }

        recycle();

        // try to dispose originalBuffer (if allowed)
        messageLocal.release();

    }

    public boolean isFinished() {
        return !getWritableMessage().hasRemaining();
    }

    protected final void reset() {
        set(null, null, null, null, null, false);
        writeResult.recycle(); // reset the write result
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
