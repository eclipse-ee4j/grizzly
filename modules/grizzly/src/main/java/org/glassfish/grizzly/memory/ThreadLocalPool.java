/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Services Ltd.
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

/**
 * A thread local pool used by a {@link MemoryManager} to create and modify Buffers
 * 
 * @param <E> Type of Buffer that will be created
 * @see java.nio.Buffer
 * @see org.glassfish.grizzly.Buffer
 */
public interface ThreadLocalPool<E> {

    /**
     * Resets the Buffer to empty values and empties the pool
     * 
     * @param pool the buffer to reset
     */
    void reset(E pool);

    /**
     * Creates a buffer with a given capacity and limit
     * 
     * @param size maximum number of elements
     * @return the new buffer
     * @see java.nio.ByteBuffer#allocate(int)
     */
    E allocate(int size);

    /**
     * Creates a new Buffer with a set size and assigns it the data that was held in the old one as long as the given size
     * is not smaller than the data held.
     * 
     * @param oldBuffer Old Buffer containing data
     * @param newSize The size the new Buffer should be.
     * @return the new Buffer or null if the buffer could not be resized
     */
    E reallocate(E oldBuffer, int newSize);

    /**
     * deallocates the data in the buffer
     * 
     * @param underlyingBuffer the buffer to release
     * @return true if operation successfully completed, false otherwise
     */
    boolean release(E underlyingBuffer);

    /**
     * Whether the last element in the buffer has been set
     * 
     * @param oldBuffer the buffer to check
     * @return true if the end of the buffer has been allocated, false otherwise
     */
    boolean isLastAllocated(E oldBuffer);

    /**
     * Reduces the buffer to the last data allocated
     * 
     * @param buffer
     * @return the old buffer data that was removed. This may be null.
     */
    E reduceLastAllocated(E buffer);

    /**
     * Checks if the size of the Buffer should be reset.
     * 
     * @param size the desired size of the buffer. If this is less than the current size ofthe buffer then this will return
     * false
     * @return true if the the buffer should be enlarged to hold the desired size
     */
    boolean wantReset(int size);

    /**
     * Gets the number of elements between the current position and the limit
     * 
     * @return number of elements
     */
    int remaining();

    /**
     * Whether there are elements between the current position and the end
     * 
     * @return true if there are unused elements, false otherwise
     */
    boolean hasRemaining();
}
