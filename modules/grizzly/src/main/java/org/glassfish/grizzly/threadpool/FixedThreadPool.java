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

package org.glassfish.grizzly.threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;

/**
 * Minimalistic fixed thread pool to allow for nice scalability if a good Queue impl is used.
 *
 * @author gustav trede
 */
public class FixedThreadPool extends AbstractThreadPool {

    protected final BlockingQueue<Runnable> workQueue;

    public FixedThreadPool(ThreadPoolConfig config) {
        super(config);

        this.workQueue = config.getQueue() != null ? (BlockingQueue<Runnable>) config.getQueue()
                : (BlockingQueue<Runnable>) config.setQueue(new LinkedTransferQueue<>()).getQueue();

        int poolSize = config.getMaxPoolSize();

        synchronized (stateLock) {
            while (poolSize-- > 0) {
                doStartWorker();
            }
        }

        ProbeNotifier.notifyThreadPoolStarted(this);
        super.onMaxNumberOfThreadsReached();
    }

    /**
     * Must hold statelock while calling this method.
     */
    private void doStartWorker() {
        startWorker(new BasicWorker());
    }

    @Override
    public void execute(Runnable command) {
        if (running) {
            if (workQueue.offer(command)) {
                // doublecheck the pool is still running
                if (!running && workQueue.remove(command)) {
                    throw new RejectedExecutionException("ThreadPool is not running");
                }

                onTaskQueued(command);
                return;
            }
            onTaskQueueOverflow();
            return;
        }
        throw new RejectedExecutionException("ThreadPool is not running");
    }

    private final class BasicWorker extends Worker {
        @Override
        protected Runnable getTask() throws InterruptedException {
            return workQueue.take();
        }
    }
}
