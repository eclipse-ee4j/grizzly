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

package org.glassfish.grizzly.memory;

import org.glassfish.grizzly.Buffer;

/**
 *
 * @author Alexey Stashok
 */
public abstract class CompositeBuffer implements Buffer {
    /**
     * The order in which internal {@link Buffer}s will be disposed.
     */
    public enum DisposeOrder {
        LAST_TO_FIRST, FIRST_TO_LAST
    }

    protected DisposeOrder disposeOrder = DisposeOrder.LAST_TO_FIRST;

    /**
     * Construct <tt>CompositeBuffer</tt>.
     * 
     * @return new <tt>CompositeBuffer</tt>
     */
    public static CompositeBuffer newBuffer() {
        return BuffersBuffer.create();
    }

    public static CompositeBuffer newBuffer(final MemoryManager memoryManager) {
        return BuffersBuffer.create(memoryManager);
    }

    public static CompositeBuffer newBuffer(final MemoryManager memoryManager, final Buffer... buffers) {
        return BuffersBuffer.create(memoryManager, buffers);
    }

    public static CompositeBuffer newBuffer(final MemoryManager memoryManager, final Buffer[] buffers, final boolean isReadOnly) {
        return BuffersBuffer.create(memoryManager, buffers, isReadOnly);
    }

    /**
     * Returns the order in which internal {@link Buffer}s will be disposed.
     * 
     * @return {@link DisposeOrder}
     */
    public DisposeOrder disposeOrder() {
        return disposeOrder;
    }

    /**
     * Sets the order in which internal {@link Buffer}s will be disposed.
     * 
     * @param disposeOrder
     * @return this buffer
     */
    public CompositeBuffer disposeOrder(final DisposeOrder disposeOrder) {
        this.disposeOrder = disposeOrder;
        return this;
    }

    public abstract CompositeBuffer append(Buffer buffer);

    @Override
    public abstract CompositeBuffer prepend(Buffer buffer);

    @Override
    public abstract Object[] underlying();

    /**
     * Removes underlying {@link Buffer}s, without disposing
     */
    public abstract void removeAll();

    public abstract void allowInternalBuffersDispose(boolean allow);

    public abstract boolean allowInternalBuffersDispose();

    /**
     * Iterates over {@link Buffer} bytes from {@link #position()} to {@link #limit()} and lets {@link BulkOperation}
     * examine/change the buffer content;
     *
     * @param operation {@link BulkOperation}
     * @return <tt>Buffer</tt> position the processing was stopped on, or <tt>-1</tt>, if processing reached the limit.
     */
    public abstract int bulk(BulkOperation operation);

    /**
     * Iterates over {@link Buffer} bytes from position to limit and lets {@link BulkOperation} examine/change the buffer
     * content;
     *
     * @param operation {@link BulkOperation}
     * @return <tt>Buffer</tt> position the processing was stopped on, or <tt>-1</tt>, if processing reached the limit.
     */
    public abstract int bulk(BulkOperation operation, int position, int limit);

    /**
     * Replace one internal {@link Buffer} with another one.
     * 
     * @param oldBuffer the {@link Buffer} to replace.
     * @param newBuffer the new {@link Buffer}.
     * @return <tt>true</tt>, if the oldBuffer was found and replaced, or <tt>false</tt> otherwise.
     */
    public abstract boolean replace(Buffer oldBuffer, Buffer newBuffer);

    /**
     * Bulk Buffer operation, responsible for byte-by-byte Buffer processing.
     */
    public interface BulkOperation {
        /**
         * Method is responsible to examine/change one single Buffer's byte.
         * 
         * @param setter {@link Setter}
         * @return <tt>true</tt>, if we want bulk processing stop, or <tt>false</tt> to continue processing.
         */
        boolean processByte(byte value, Setter setter);
    }

    /**
     * Setter.
     */
    public interface Setter {
        /**
         * Set the current byte value.
         * 
         * @param value value
         */
        void set(byte value);
    }
}
