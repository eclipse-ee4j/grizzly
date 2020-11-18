/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Buffer;

/**
 * Class has useful methods to simplify the work with {@link Buffer}s.
 *
 * @see MemoryManager
 * @see WrapperAware
 *
 * @author Alexey Stashok
 */
public class MemoryUtils {
    public static ByteBuffer allocateByteBuffer(MemoryManager memoryManager, int size) {
        if (memoryManager instanceof ByteBufferAware) {
            return ((ByteBufferAware) memoryManager).allocateByteBuffer(size);
        }

        return ByteBuffer.allocate(size);
    }

    public static ByteBuffer reallocateByteBuffer(MemoryManager memoryManager, ByteBuffer oldByteBuffer, int size) {
        if (memoryManager instanceof ByteBufferAware) {
            return ((ByteBufferAware) memoryManager).reallocateByteBuffer(oldByteBuffer, size);
        }

        return ByteBuffer.allocate(size);
    }

    public static void releaseByteBuffer(MemoryManager memoryManager, ByteBuffer byteBuffer) {
        if (memoryManager instanceof ByteBufferAware) {
            ((ByteBufferAware) memoryManager).releaseByteBuffer(byteBuffer);
        }
    }
}
