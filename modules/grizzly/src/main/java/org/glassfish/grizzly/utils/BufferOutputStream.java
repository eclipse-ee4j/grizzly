/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * {@link OutputStream} implementation to write to a {@link Buffer}.
 */
public class BufferOutputStream extends OutputStream {
    private final static int BUFFER_SIZE = 8192;

    final MemoryManager mm;
    final boolean reallocate;

    private Buffer currentBuffer;
    private CompositeBuffer compositeBuffer;

    // ------------------------------------------------------------ Constructors

    /**
     * Creates the <tt>BufferOutputStream</tt> without initial Buffer. Created BufferOutputStream won't use "reallocate"
     * strategy, which means if internal {@link Buffer} window gets overloaded - it will be appended to the
     * {@link CompositeBuffer} and new window will be allocated.
     *
     * @param mm {@link MemoryManager}
     */
    public BufferOutputStream(final MemoryManager mm) {

        this(mm, null);

    }

    /**
     * Creates the <tt>BufferOutputStream</tt> using passed {@link Buffer} as initial. Created BufferOutputStream won't use
     * "reallocate" strategy, which means if internal {@link Buffer} window gets overloaded - it will be appended to the
     * {@link CompositeBuffer} and new window will be allocated.
     *
     * @param mm {@link MemoryManager}
     * @param buffer initial {@link Buffer}
     */
    public BufferOutputStream(final MemoryManager mm, final Buffer buffer) {

        this(mm, buffer, false);
    }

    /**
     * Creates the <tt>BufferOutputStream</tt> using passed {@link Buffer} as initial. Created BufferOutputStream can choose
     * whether use or not "reallocate" strategy. Using "reallocate" strategy means following: if internal {@link Buffer}
     * window gets overloaded - it will be reallocated using
     * {@link MemoryManager#reallocate(org.glassfish.grizzly.Buffer, int)}, which may lead to extra memory allocation and
     * bytes copying (bigger memory chunk might be allocated to keep original stream data plus extra data been written to
     * stream), the benefit of this is that stream data will be located in the single memory chunk. If we don't use
     * "reallocate" strategy - when internal {@link Buffer} gets overloaded it will be appended to the
     * {@link CompositeBuffer} and new window {@link Buffer} will be allocated then.
     *
     * @param mm {@link MemoryManager}
     * @param buffer initial {@link Buffer}
     */
    public BufferOutputStream(final MemoryManager mm, final Buffer buffer, final boolean reallocate) {

        this.currentBuffer = buffer;
        this.mm = mm;
        this.reallocate = reallocate;
    }

    // ----------------------------------------------- Public methods

    public void setInitialOutputBuffer(final Buffer initialBuffer) {
        if (currentBuffer != null || compositeBuffer != null) {
            throw new IllegalStateException("Can not set initial buffer on non-reset OutputStream");
        }

        currentBuffer = initialBuffer;
    }

    /**
     * Get the result {@link Buffer} (not flipped).
     * 
     * @return the result {@link Buffer} (not flipped).
     */
    public Buffer getBuffer() {
        if (reallocate || compositeBuffer == null) {
            return currentBuffer != null ? currentBuffer : Buffers.EMPTY_BUFFER;
        } else {
            if (currentBuffer != null && currentBuffer.position() > 0) {
                flushCurrent();
            }

            return compositeBuffer;
        }
    }

    /**
     * Returns <tt>true</tt> if "reallocate" strategy is used or <tt>false</tt> otherwise. Using "reallocate" strategy means
     * following: if internal {@link Buffer} window gets overloaded - it will be reallocated using
     * {@link MemoryManager#reallocate(org.glassfish.grizzly.Buffer, int)}, which may lead to extra memory allocation and
     * bytes copying (bigger memory chunk might be allocated to keep original stream data plus extra data been written to
     * stream), the benefit of this is that stream data will be located in the single memory chunk. If we don't use
     * "reallocate" strategy - when internal {@link Buffer} gets overloaded it will be appended to the
     * {@link CompositeBuffer} and new window {@link Buffer} will be allocated then.
     *
     * @return <tt>true</tt> if "reallocate" strategy is used or <tt>false</tt> otherwise.
     */
    public boolean isReallocate() {
        return reallocate;
    }

    // ----------------------------------------------- Methods from OutputStream

    @Override
    public void write(int b) throws IOException {
        ensureCapacity(1);
        currentBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureCapacity(len);
        currentBuffer.put(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        // no-op
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    public void reset() {
        currentBuffer = null;
        compositeBuffer = null;
    }

    // --------------------------------------------------------- Protected Methods

    protected Buffer allocateNewBuffer(final MemoryManager memoryManager, final int size) {
        return memoryManager.allocate(size);
    }

    // --------------------------------------------------------- Private Methods

    @SuppressWarnings({ "unchecked" })
    private void ensureCapacity(final int len) {
        if (currentBuffer == null) {
            currentBuffer = allocateNewBuffer(mm, Math.max(BUFFER_SIZE, len));
        } else if (currentBuffer.remaining() < len) {
            if (reallocate) {
                currentBuffer = mm.reallocate(currentBuffer, Math.max(currentBuffer.capacity() + len, currentBuffer.capacity() * 3 / 2 + 1));
            } else {
                flushCurrent();
                currentBuffer = allocateNewBuffer(mm, Math.max(BUFFER_SIZE, len));
            }
        }
    }

    private void flushCurrent() {
        currentBuffer.trim();
        if (compositeBuffer == null) {
            compositeBuffer = CompositeBuffer.newBuffer(mm);
        }

        compositeBuffer.append(currentBuffer);
        compositeBuffer.position(compositeBuffer.limit());
        currentBuffer = null;
    }
}
