/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.Buffers;

/**
 * Thread-local Direct {@link ByteBuffer} storage.
 *
 * @author Alexey Stashok
 */
public final class DirectByteBufferRecord {

    private static final ThreadCache.CachedTypeIndex<DirectByteBufferRecord> CACHE_IDX =
            ThreadCache.obtainIndex("direct-buffer-cache", DirectByteBufferRecord.class, 1);

    public static DirectByteBufferRecord get() {
        final DirectByteBufferRecord record =
                ThreadCache.getFromCache(CACHE_IDX);
        if (record != null) {
            return record;
        }
        final DirectByteBufferRecord recordLocal = new DirectByteBufferRecord();
        ThreadCache.putToCache(CACHE_IDX, recordLocal);
        return recordLocal;
    }
    
    
    private ByteBuffer directBuffer;
    private int sliceOffset;
    private ByteBuffer directBufferSlice;
    private SoftReference<ByteBuffer> softRef;
    private ByteBuffer array[];
    private int arraySize;

    DirectByteBufferRecord() {
        array = new ByteBuffer[8];
    }

    public ByteBuffer getDirectBuffer() {
        return directBuffer;
    }

    public ByteBuffer getDirectBufferSlice() {
        return directBufferSlice;
    }

    public ByteBuffer allocate(final int size) {
        ByteBuffer byteBuffer;
        if ((byteBuffer = switchToStrong()) != null && byteBuffer.remaining() >= size) {
            return byteBuffer;
        } else {
            byteBuffer = ByteBuffer.allocateDirect(size);
            reset(byteBuffer);
            return byteBuffer;
        }
    }
        
    public ByteBuffer sliceBuffer() {
        int oldLim = directBuffer.limit();
        Buffers.setPositionLimit(directBuffer, sliceOffset, directBuffer.capacity());
        directBufferSlice = directBuffer.slice();
        Buffers.setPositionLimit(directBuffer, 0, oldLim);        
        return directBufferSlice;
    }

    public void finishBufferSlice() {
        if (directBufferSlice != null) {
            directBufferSlice.flip();
            final int sliceSz = directBufferSlice.remaining();
            sliceOffset += sliceSz;

            if (sliceSz > 0) {
                putToArray(directBufferSlice);
            }

            directBufferSlice = null;
        }
    }

    public ByteBuffer[] getArray() {
        return array;
    }

    public int getArraySize() {
        return arraySize;
    }

    public void putToArray(ByteBuffer byteBuffer) {
        ensureArraySize();
        array[arraySize++] = byteBuffer;
    }

    public void release() {
        if (directBuffer != null) {
            directBuffer.clear();
            switchToSoft();
        }
        
        Arrays.fill(array, 0, arraySize, null);
        arraySize = 0;
        directBufferSlice = null;
        sliceOffset = 0;
    }

    private ByteBuffer switchToStrong() {
        if (directBuffer == null && softRef != null) {
            directBuffer = directBufferSlice = softRef.get();
        }
        return directBuffer;
    }

    private void switchToSoft() {
        if (directBuffer != null && softRef == null) {
            softRef = new SoftReference<ByteBuffer>(directBuffer);
        }
        directBuffer = null;
    }

    private void reset(ByteBuffer byteBuffer) {
        directBuffer = directBufferSlice = byteBuffer;
        softRef = null;
    }

    private void ensureArraySize() {
        if (arraySize == array.length) {
            array = Arrays.copyOf(array, (arraySize * 3) / 2 + 1);
        }
    }
}
