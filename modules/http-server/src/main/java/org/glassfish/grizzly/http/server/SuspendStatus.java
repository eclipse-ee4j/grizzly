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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.ThreadCache;

/**
 * The request/response suspend status bound to a specific thread.
 *
 * @author Alexey Stashok
 */
public final class SuspendStatus {
    private static final ThreadCache.CachedTypeIndex<SuspendStatus> CACHE_IDX = ThreadCache.obtainIndex(SuspendStatus.class, 4);

    public static SuspendStatus create() {
        SuspendStatus status = ThreadCache.takeFromCache(CACHE_IDX);
        if (status == null) {
            status = new SuspendStatus();
        }

        assert status.initThread == Thread.currentThread();

        status.state = State.NOT_SUSPENDED;

        return status;
    }

    private enum State {
        NOT_SUSPENDED, SUSPENDED, INVALIDATED
    }

    private State state;

    private final Thread initThread;

    private SuspendStatus() {
        initThread = Thread.currentThread();
    }

    public void suspend() {
        assert Thread.currentThread() == initThread;

        if (state != State.NOT_SUSPENDED) {
            throw new IllegalStateException("Can not suspend. Expected suspend state='" + State.NOT_SUSPENDED + "' but was '" + state + "'");
        }

        state = State.SUSPENDED;
    }

    boolean getAndInvalidate() {
        assert Thread.currentThread() == initThread;

        final boolean wasSuspended = state == State.SUSPENDED;
        state = State.INVALIDATED;

        ThreadCache.putToCache(initThread, CACHE_IDX, this);

        return wasSuspended;
    }

    public void reset() {
        assert Thread.currentThread() == initThread;

        state = State.NOT_SUSPENDED;
    }
}
