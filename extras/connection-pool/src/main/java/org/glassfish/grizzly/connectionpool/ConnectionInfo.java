/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.connectionpool;

import org.glassfish.grizzly.Connection;

/**
 * Pooled {@link Connection} information, that might be used for monitoring reasons.
 *
 * @param <E>
 * @author Alexey Stashok
 */
public final class ConnectionInfo<E> {
    final Connection connection;
    final Link<ConnectionInfo<E>> readyStateLink;
    final SingleEndpointPool<E> endpointPool;

    long ttlTimeout; // the place holder for TTL time stamp

    private final long pooledTimeStamp;

    ConnectionInfo(final Connection connection, final SingleEndpointPool<E> endpointPool) {
        this.connection = connection;
        this.endpointPool = endpointPool;
        this.readyStateLink = new Link<>(this);
        pooledTimeStamp = System.currentTimeMillis();
    }

    /**
     * @return <tt>true</tt> if the {@link Connection} is in ready state, waiting for a user to pull it out from the pool.
     * Returns <tt>false</tt> if the {@link Connection} is currently busy.
     */
    public boolean isReady() {
        synchronized (endpointPool.poolSync) {
            return readyStateLink.isAttached();
        }
    }

    /**
     * @return the timestamp (in milliseconds) when this {@link Connection} was returned to the pool and its state switched
     * to ready, or <tt>-1</tt> if the {@link Connection} is currently in busy state.
     */
    public long getReadyTimeStamp() {
        synchronized (endpointPool.poolSync) {
            return readyStateLink.getAttachmentTimeStamp();
        }
    }

    /**
     * @return the timestamp (in milliseconds) when this {@link Connection} was added to the pool: either created directly
     * by pool or attached.
     */
    public long getPooledTimeStamp() {
        return pooledTimeStamp;
    }

    @Override
    public String toString() {
        return "ConnectionInfo{" + "connection=" + connection + ", readyStateLink=" + readyStateLink + ", endpointPool=" + endpointPool + ", pooledTimeStamp="
                + pooledTimeStamp + "} " + super.toString();
    }
}
