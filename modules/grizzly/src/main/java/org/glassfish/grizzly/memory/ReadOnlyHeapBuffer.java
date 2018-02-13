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

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import org.glassfish.grizzly.Buffer;

/**
 * Read-only {@link HeapBuffer implementation}.
 *
 * @since 2.0
 */
class ReadOnlyHeapBuffer extends HeapBuffer {


    // ------------------------------------------------------------ Constructors


    ReadOnlyHeapBuffer(byte[] heap, int offset, int cap) {
        super(heap, offset, cap);
    }


    // ------------------------------------------------- Methods from HeapBuffer


    @Override
    public HeapBuffer asReadOnlyBuffer() {
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public HeapBuffer prepend(Buffer header) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(int index, byte b) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(Buffer src) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(Buffer src, int position, int length) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public Buffer put(ByteBuffer src) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public Buffer put(ByteBuffer src, int position, int length) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(byte b) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(byte[] src) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer put(byte[] src, int offset, int length) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putChar(char value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putChar(int index, char value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putShort(short value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putShort(int index, short value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putInt(int value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putInt(int index, int value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putLong(long value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putLong(int index, long value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putFloat(float value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putFloat(int index, float value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putDouble(double value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public HeapBuffer putDouble(int index, double value) {
        throw new ReadOnlyBufferException();
    }

    @Override
    protected HeapBuffer createHeapBuffer(final int offset, final int capacity) {
        return new ReadOnlyHeapBuffer(heap, offset, capacity);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return super.toByteBuffer().asReadOnlyBuffer();
    }

    @Override
    public ByteBuffer toByteBuffer(int position, int limit) {
        return super.toByteBuffer(position, limit).asReadOnlyBuffer();
    }
}
