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

package org.glassfish.grizzly.http.io;

import java.util.Arrays;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.HeapBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * {@link HeapBuffer} implementation, which might be reset to reference another byte[] at any moment.
 *
 * @author Alexey Stashok
 */
final class TemporaryHeapBuffer extends HeapBuffer {

    boolean isDisposed;
    boolean hasClonedArray;

    /**
     * Reset the byte[] this Buffer wraps.
     */
    void reset(final byte[] heap, final int offset, final int len) {
        this.heap = heap;
        this.offset = offset;
        this.cap = len;
        this.lim = len;
        this.pos = 0;
        byteBuffer = null;
        isDisposed = false;
        hasClonedArray = false;
    }

    Buffer cloneContent(final MemoryManager memoryManager) {
        final Buffer buffer;

        final int length = remaining();

        if (!hasClonedArray) {
            buffer = memoryManager.allocate(length);
            buffer.put(heap, offset + pos, length);
            buffer.flip();
        } else {
            buffer = Buffers.wrap(memoryManager, heap, offset + pos, length);
        }

        buffer.allowBufferDispose(true);
        dispose();

        return buffer;
    }

    @Override
    protected void onShareHeap() {
        if (!hasClonedArray) {
            heap = Arrays.copyOfRange(heap, offset, offset + cap);
            offset = 0;
            hasClonedArray = true;
        }

        super.onShareHeap();
    }

    @Override
    public void dispose() {
        isDisposed = true;

        super.dispose();
    }

    boolean isDisposed() {
        return isDisposed;
    }

    public void recycle() {
        reset(null, 0, 0);
    }
}
