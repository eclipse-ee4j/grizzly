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

package org.glassfish.grizzly.threadpool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Need to evaluate queue size limit perf implications on this fixedpool variant.
 * The atomic counter can in theory approach synchronized (lack of) scalability
 * in heavy load situations.
 *
 * @author gustav trede
 * @author Tigran Mkrtchyan
 */
final class QueueLimitedThreadPool extends FixedThreadPool {

    private final Semaphore queuePermits;

    /**
     * @param config the {@link ThreadPoolConfig} to configure this pool.
     */
    QueueLimitedThreadPool(ThreadPoolConfig config) {
        super(config);
        if (config.getQueueLimit() < 0) {
            throw new IllegalArgumentException("maxQueuedTasks < 0");
        }

        queuePermits = new Semaphore(config.getQueueLimit());
    }

    @Override
    public final void execute(Runnable command) {
        if (command == null) { // must nullcheck to ensure queuesize is valid
            throw new IllegalArgumentException("Runnable task is null");
        }

        if (!running) {
            throw new RejectedExecutionException("ThreadPool is not running");
        }

        if (!queuePermits.tryAcquire()) {
            onTaskQueueOverflow();
        }

        if (!workQueue.offer(command)) {
            queuePermits.release();
            onTaskQueueOverflow();
        }

        onTaskQueued(command);
    }

    @Override
    protected final void beforeExecute(final Worker worker, final Thread t,
            final Runnable r) {
        super.beforeExecute(worker, t, r);
        queuePermits.release();
    }
}

