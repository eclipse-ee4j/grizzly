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

package org.glassfish.grizzly.utils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Alexey Stashok
 */
public class DelayedExecutor {
    public final static long UNSET_TIMEOUT = -1;
    
    private final ExecutorService threadPool;

    private final DelayedRunnable runnable = new DelayedRunnable();
    
    private final Queue<DelayQueue> queues =
             new ConcurrentLinkedQueue<DelayQueue>();

    private final Object sync = new Object();

    private volatile boolean isStarted;

    private final long checkIntervalMillis;

    public DelayedExecutor(final ExecutorService threadPool) {
        this(threadPool, 1000, TimeUnit.MILLISECONDS);
    }

    public DelayedExecutor(final ExecutorService threadPool,
            final long checkInterval, final TimeUnit timeunit) {
        if (checkInterval < 0) {
            throw new IllegalArgumentException("check interval can't be negative");
        }
        
        this.threadPool = threadPool;
        this.checkIntervalMillis = TimeUnit.MILLISECONDS.convert(checkInterval, timeunit);
    }

    public void start() {
        synchronized(sync) {
            if (!isStarted) {
                isStarted = true;
                threadPool.execute(runnable);
            }
        }
    }

    public void stop() {
        synchronized(sync) {
            if (isStarted) {
                isStarted = false;
                sync.notify();
            }
        }
    }

    public void destroy() {
        stop();
        synchronized(sync) {
            queues.clear();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public <E> DelayQueue<E> createDelayQueue(final Worker<E> worker,
            final Resolver<E> resolver) {
        
        final DelayQueue<E> queue = new DelayQueue<E>(worker, resolver);

        queues.add(queue);

        return queue;
    }

    private static boolean wasModified(final long l1, final long l2) {
        return l1 != l2;
    }

    private class DelayedRunnable implements Runnable {

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            while(isStarted) {
                final long currentTimeMillis = System.currentTimeMillis();
                
                for (final DelayQueue delayQueue : queues) {
                    if (delayQueue.queue.isEmpty()) continue;
                    
                    final Resolver resolver = delayQueue.resolver;

                    for (Iterator it = delayQueue.queue.keySet().iterator(); it.hasNext(); ) {
                        final Object element = it.next();
                        final long timeoutMillis = resolver.getTimeoutMillis(element);
                        
                        if (timeoutMillis == UNSET_TIMEOUT) {
                            it.remove();
                            if (wasModified(timeoutMillis,
                                    resolver.getTimeoutMillis(element))) {                                
                                delayQueue.queue.put(element, delayQueue);
                            }
                        } else if (currentTimeMillis - timeoutMillis >= 0) {
                            it.remove();
                            if (wasModified(timeoutMillis,
                                    resolver.getTimeoutMillis(element))) {
                                delayQueue.queue.put(element, delayQueue);
                            } else {
                                try {
                                    if (!delayQueue.worker.doWork(element)) {
                                        delayQueue.queue.put(element, delayQueue);
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }

                synchronized(sync) {
                    if (!isStarted) return;
                    
                    try {
                        sync.wait(checkIntervalMillis);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    public class DelayQueue<E> {
        final ConcurrentMap<E, DelayQueue> queue = new ConcurrentHashMap<>();

        final Worker<E> worker;
        final Resolver<E> resolver;

        public DelayQueue(final Worker<E> worker, final Resolver<E> resolver) {
            this.worker = worker;
            this.resolver = resolver;
        }

        public void add(final E elem, final long delay, final TimeUnit timeUnit) {
            if (delay >= 0) {
                final long delayWithSysTime =
                        System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, timeUnit);
                resolver.setTimeoutMillis(elem, ((delayWithSysTime < 0) ? Long.MAX_VALUE : delayWithSysTime));
                queue.put(elem, this);
            }
        }

        public void remove(final E elem) {
            resolver.removeTimeout(elem);
        }

        public void destroy() {
            queues.remove(this);
        }
    }

    public interface Worker<E> {
        /**
         * The method is executed by <tt>DelayExecutor</tt> once element's timeout expires.
         * 
         * @param element element to operate upon.
         * @return <tt>true</tt>, if the work is done and element has to be removed
         *          from the delay queue, or <tt>false</tt> if the element
         *          should be re-registered on the delay queue again
         */
        boolean doWork(E element);
    }

    public interface Resolver<E> {
        boolean removeTimeout(E element);
        
        long getTimeoutMillis(E element);
        
        void setTimeoutMillis(E element, long timeoutMillis);
    }
}
