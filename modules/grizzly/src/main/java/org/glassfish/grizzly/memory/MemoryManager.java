/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.grizzly.monitoring.MonitoringAware;

/**
 * <tt>MemoryManager</tt>, responsible for allocating and releasing memory,
 * required during application runtime.
 * <tt>MemoryManager</tt> implementations work with Grizzly {@link Buffer}s.
 *
 * @see Buffer
 *
 * @author Alexey Stashok
 */
public interface MemoryManager<E extends Buffer>
        extends MonitoringAware<MemoryProbe> {

    /**
     * <p>
     * The default {@link MemoryManager} implementation used by all created builder
     * instances.
     * </p>
     *
     * <p>
     * The default may be changed by one of two methods:
     * <ul>
     *     <li>
     *          Setting the system property {@value MemoryManagerInitializer#DMM_PROP_NAME}
     *          with the fully qualified name of the class that implements the
     *          MemoryManager interface.  Note that this class must be public and
     *          have a public no-arg constructor.
     *     </li>
     *     <li>
     *         Setting the system property {@value DefaultMemoryManagerFactory#DMMF_PROP_NAME}
     *         with the fully qualified name of the class that implements the
     *         {@link org.glassfish.grizzly.memory.DefaultMemoryManagerFactory} interface.
     *         Note that this class must be public and have a public no-arg
     *         constructor.
     *     </li>
     * </ul>
     *
     * </p>
     */
    MemoryManager DEFAULT_MEMORY_MANAGER =
            MemoryManagerInitializer.initManager();

    /**
     * Allocated {@link Buffer} of the required size.
     *
     * @param size {@link Buffer} size to be allocated.
     * @return allocated {@link Buffer}.
     */
    E allocate(int size);

    /**
     * Allocated {@link Buffer} at least of the provided size.
     * This could be useful for usecases like Socket.read(...), where
     * we're not sure how many bytes are available, but want to read as
     * much as possible.
     *
     * @param size the min {@link Buffer} size to be allocated.
     * @return allocated {@link Buffer}.
     */
    E allocateAtLeast(int size);

    /**
     * Reallocate {@link Buffer} to a required size.
     * Implementation may choose the way, how reallocation could be done, either
     * by allocating new {@link Buffer} of required size and copying old
     * {@link Buffer} content there, or perform more complex logic related to
     * memory pooling etc.
     *
     * @param oldBuffer old {@link Buffer} to be reallocated.
     * @param newSize new {@link Buffer} required size.
     * @return reallocated {@link Buffer}.
     */
    E reallocate(E oldBuffer, int newSize);

    /**
     * Release {@link Buffer}.
     * Implementation may ignore releasing and let JVM Garbage collector to take
     * care about the {@link Buffer}, or return {@link Buffer} to pool, in case
     * of more complex <tt>MemoryManager</tt> implementation.
     *
     * @param buffer {@link Buffer} to be released.
     */
    void release(E buffer);
    
    /**
     * Return <tt>true</tt> if next {@link #allocate(int)} or {@link #allocateAtLeast(int)} call,
     * made in the current thread for the given memory size, going to return a {@link Buffer} based
     * on direct {@link java.nio.ByteBuffer}, or <tt>false</tt> otherwise.
     * 
     * @param size
     * @return 
     */
    boolean willAllocateDirect(int size);
}
