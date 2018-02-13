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

package org.glassfish.grizzly.asyncqueue;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.localization.LogMessages;
import org.glassfish.grizzly.utils.DebugPoint;

/**
 * {@link AsyncQueue} element unit
 * 
 * @param <R> the result type
 * 
 * @author Alexey Stashok
 */
public abstract class AsyncQueueRecord<R> implements Cacheable {
    private final static Logger LOGGER = Grizzly.logger(AsyncQueue.class);
    
    protected Connection connection;
    protected Object message;
    protected CompletionHandler completionHandler;

    protected boolean isRecycled = false;
    protected DebugPoint recycleTrack;
    
    protected AsyncQueueRecord() {
    }
    
    public AsyncQueueRecord(final Connection connection,
            final Object message, final CompletionHandler completionHandler) {
        set(connection, message, completionHandler);
    }

    protected final void set(final Connection connection,
            final Object message, final CompletionHandler completionHandler) {

        checkRecycled();
        this.connection = connection;
        this.message = message;
        this.completionHandler = completionHandler;
    }

    public Connection getConnection() {
        return connection;
    }
  
    @SuppressWarnings("unchecked")
    public final <T> T getMessage() {
        checkRecycled();
        return (T) message;
    }

    public final void setMessage(final Object message) {
        checkRecycled();
        this.message = message;
    }

    /**
     * Returns the current record result object.
     * 
     * @return the current record result object
     */
    public abstract R getCurrentResult();

    public void notifyFailure(final Throwable e) {
        if (completionHandler != null) {
            completionHandler.failed(e);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE,
                        LogMessages.FINE_GRIZZLY_ASYNCQUEUE_ERROR_NOCALLBACK_ERROR(e));
            }
        }
    }


    @SuppressWarnings("unchecked")
    public final void notifyIncomplete() {
        if (completionHandler != null) {
            completionHandler.updated(getCurrentResult());
        }
    }
    
    protected final void checkRecycled() {
        if (Grizzly.isTrackingThreadCache() && isRecycled) {
            final DebugPoint track = recycleTrack;
            if (track != null) {
                throw new IllegalStateException("AsyncReadQueueRecord has been recycled at: " + track);
            } else {
                throw new IllegalStateException("AsyncReadQueueRecord has been recycled");
            }
        }
    }
}
