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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * {@link ExecutorService} implementation, which function the similar way as former Grizzly 1.x Pipeline based thread
 * pools.
 *
 * The <tt>SyncThreadPool</tt> is synchronized similar way as Grizzly 1.x Pipeline, which makes thread pool more
 * accurate when deciding to create or not additional worker threads.
 *
 *
 * @author Alexey Stashok
 */
public class SyncThreadPool extends AbstractThreadPool {

    private final Queue<Runnable> workQueue;
    protected int maxQueuedTasks = -1;
    private int currentPoolSize;
    private int activeThreadsCount;

    /**
     *
     */
    public SyncThreadPool(ThreadPoolConfig config) {
        super(config);
        if (config.getKeepAliveTime(TimeUnit.MILLISECONDS) < 0) {
            throw new IllegalArgumentException("keepAliveTime < 0");
        }

        workQueue = config.getQueue() != null ? config.getQueue() : config.setQueue(new LinkedList<Runnable>()).getQueue();

        this.maxQueuedTasks = config.getQueueLimit();
        final int corePoolSize = config.getCorePoolSize();
        while (currentPoolSize < corePoolSize) {
            startWorker(new SyncThreadWorker(true));
        }
        ProbeNotifier.notifyThreadPoolStarted(this);
    }

    @Override
    public void execute(Runnable task) {

        if (task == null) {
            throw new IllegalArgumentException("Runnable task is null");
        }

        synchronized (stateLock) {
            if (!running) {
                throw new RejectedExecutionException("ThreadPool is not running");
            }

            final int workQueueSize = workQueue.size() + 1;

            if ((maxQueuedTasks < 0 || workQueueSize <= maxQueuedTasks) && workQueue.offer(task)) {
                onTaskQueued(task);
            } else {
                onTaskQueueOverflow();
                assert false; // should not reach this point
            }

            final int idleThreadsNumber = currentPoolSize - activeThreadsCount;

            if (idleThreadsNumber >= workQueueSize) {
                stateLock.notify();
                return;
            }

            if (currentPoolSize < config.getMaxPoolSize()) {
                final boolean isCore = currentPoolSize < config.getCorePoolSize();
                startWorker(new SyncThreadWorker(isCore));

                if (currentPoolSize == config.getMaxPoolSize()) {
                    onMaxNumberOfThreadsReached();
                }
            }
        }
    }

    @Override
    protected void startWorker(Worker worker) {
        synchronized (stateLock) {
            super.startWorker(worker);
            activeThreadsCount++;
            currentPoolSize++;
        }
    }

    @Override
    protected void onWorkerExit(Worker worker) {
        super.onWorkerExit(worker);

        synchronized (stateLock) {
            currentPoolSize--;
            activeThreadsCount--;
        }
    }

    @Override
    protected void poisonAll() {
        int size = currentPoolSize;
        final Queue<Runnable> q = getQueue();
        while (size-- > 0) {
            q.offer(poison);
        }
    }

    @Override
    public String toString() {
        synchronized (stateLock) {
            return super.toString() + ", max-queue-size=" + maxQueuedTasks;
        }
    }

    protected class SyncThreadWorker extends Worker {

        private final boolean core;

        public SyncThreadWorker(boolean core) {
            this.core = core;
        }

        @Override
        protected Runnable getTask() throws InterruptedException {
            synchronized (stateLock) {
                activeThreadsCount--;
                try {

                    if (!running || !core && currentPoolSize > config.getMaxPoolSize()) {
                        // if maxpoolsize becomes lower during runtime we kill of the
                        return null;
                    }

                    Runnable r = workQueue.poll();

                    if (r != null) {
                        return r;
                    }

                    long keepAliveMillis = config.getKeepAliveTime(TimeUnit.MILLISECONDS);
                    final boolean hasKeepAlive = !core && keepAliveMillis >= 0;

                    long endTime = -1;
                    if (hasKeepAlive) {
                        endTime = System.currentTimeMillis() + keepAliveMillis;
                    }

                    do {
                        if (!hasKeepAlive) {
                            stateLock.wait();
                        } else {
                            stateLock.wait(keepAliveMillis);
                        }

                        r = workQueue.poll();

                        if (r != null) {
                            return r;
                        }

                        // Less than 20 millis remainder will consider as keepalive timeout
                        if (!running) {
                            return null;
                        } else if (hasKeepAlive) {
                            keepAliveMillis = endTime - System.currentTimeMillis();

                            if (keepAliveMillis < 20) {
                                return null;
                            }
                        }
                    } while (true);

                } finally {
                    activeThreadsCount++;
                }
            }
        }
    }
}
