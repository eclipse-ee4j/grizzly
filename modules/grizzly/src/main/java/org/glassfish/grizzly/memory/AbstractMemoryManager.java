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

package org.glassfish.grizzly.memory;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;
import org.glassfish.grizzly.threadpool.DefaultWorkerThread;


/**
 * A {@link MemoryManager} abstraction to provide utilities that may be useful
 * across different {@link MemoryManager} implementations.
 *
 * @since 2.0
 */
public abstract class AbstractMemoryManager<E extends Buffer>
        implements MemoryManager<E>, ThreadLocalPoolProvider {


    /**
     * The maximum size of the memory pool that is to be maintained by
     * either the MemoryManager itself or any {@link ThreadLocalPool}s.
     */
    public static final int DEFAULT_MAX_BUFFER_SIZE = 1024 * 64;


    /**
     * Basic monitoring support.  Concrete implementations of this class need
     * only to implement the {@link #createJmxManagementObject()}  method
     * to plug into the Grizzly 2.0 JMX framework.
     */
    protected final DefaultMonitoringConfig<MemoryProbe> monitoringConfig =
            new DefaultMonitoringConfig<MemoryProbe>(MemoryProbe.class) {

        @Override
        public Object createManagementObject() {
            return createJmxManagementObject();
        }

    };

    protected final int maxBufferSize;


    // ------------------------------------------------------------ Constructors


    /**
     * Creates a new <code>AbstractMemoryManager</code> using a max buffer size
     * of {@value #DEFAULT_MAX_BUFFER_SIZE}.
     */
    public AbstractMemoryManager() {

        this(DEFAULT_MAX_BUFFER_SIZE);

    }

    /**
     * Creates a new <code>AbstractMemoryManager</code> using the specified
     * buffer size.
     *
     * @param maxBufferSize max size of the maintained buffer.
     */
    public AbstractMemoryManager(final int maxBufferSize) {

        this.maxBufferSize = maxBufferSize;

    }


    // ---------------------------------------------------------- Public Methods


    /**
     * Get the size of local thread memory pool.
     *
     * @return the size of local thread memory pool.
     */
    public int getReadyThreadBufferSize() {
       ThreadLocalPool threadLocalPool = getThreadLocalPool();
        if (threadLocalPool != null) {
            return threadLocalPool.remaining();
        }

        return 0;
    }


    /**
     * @return the max size of the buffer maintained by this
     * <code>AbstractMemoryManager</code>.
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }


    // ------------------------------------------------------- Protected Methods


    /**
     * Allocate a {@link Buffer} using the provided {@link ThreadLocalPool}.
     *
     * @param threadLocalCache the {@link ThreadLocalPool} to allocate from.
     * @param size the amount to allocate.
     *
     * @return an memory buffer, or <code>null</code> if the requested size
     *  exceeds the remaining free memory of the {@link ThreadLocalPool}.
     */
    protected Object allocateFromPool(final ThreadLocalPool threadLocalCache,
                                      final int size) {
        if (threadLocalCache.remaining() >= size) {
            ProbeNotifier.notifyBufferAllocatedFromPool(monitoringConfig, size);

            return threadLocalCache.allocate(size);
        }

        return null;
    }


    /**
     * @return the JMX {@link Object} used to register/deregister with the
     *  JMX runtime.
     */
    protected abstract Object createJmxManagementObject();


    /**
     * Get thread associated buffer pool.
     *
     * @return thread associated buffer pool.  This method may return
     *  <code>null</code> if the current thread doesn't have a buffer pool
     *  associated with it.
     */
    protected static ThreadLocalPool getThreadLocalPool() {
        final Thread t = Thread.currentThread();
        if (t instanceof DefaultWorkerThread) {
            return ((DefaultWorkerThread) t).getMemoryPool();
        } else {
            return null;
        }
    }


    // ---------------------------------------------------------- Nested Classes

    /**
     * This is a marker interface indicating a particular {@link Buffer}
     * implementation can be trimmed.
     */
    protected interface TrimAware extends Cacheable { }

}
