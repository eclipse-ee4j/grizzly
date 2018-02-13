/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test {@link OutputBuffer#ByteArrayCloner}.
 * 
 * @author Alexey Stashok
 */
public class ByteArrayClonerTest {
    private static final int EXTRA_BUFFER_SIZE = 1024;
    private static final int TEMP_BUFFER_SIZE = 2048;
    
    private final MemoryManager mm = MemoryManager.DEFAULT_MEMORY_MANAGER;
    
    @Test
    public void testSimpleBuffer() {
        
        TemporaryHeapBuffer buffer = new TemporaryHeapBuffer();
        
        // Test simple buffer with offset = 0
        byte[] array = new byte[TEMP_BUFFER_SIZE];
        buffer.reset(array, 0, array.length);
        
        fill(buffer);
        OutputBuffer.ByteArrayCloner cloner =
                new OutputBuffer.ByteArrayCloner(buffer);
        
        Buffer newBuffer = cloner.clone0(mm, buffer);
        clean(array);
        checkContent(newBuffer, 'A');
        
        
        // Test simple buffer with offset != 0
        
        array = new byte[TEMP_BUFFER_SIZE];
        int offset = 1111;
        buffer.reset(array, offset, array.length - offset);
        
        fill(buffer);
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, buffer);
        clean(array);
        assertEquals(array.length - offset, newBuffer.remaining());
        checkContent(newBuffer, 'A');
        
        // Test simple buffer with offset != 0 and position > 0
        
        array = new byte[TEMP_BUFFER_SIZE];
        offset = 1111;
        int position = 15;
        
        buffer.reset(array, offset, array.length - offset);
        
        fill(buffer);
        buffer.position(buffer.position() + position);
        
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, buffer);
        clean(array);
        assertEquals(array.length - offset - position, newBuffer.remaining());
        checkContent(newBuffer, 'A' + position);
    }
    
    @Test
    public void testSingleElementCompositeBuffer() {
        CompositeBuffer cb = CompositeBuffer.newBuffer();
        TemporaryHeapBuffer buffer = new TemporaryHeapBuffer();
        
        // Test composite buffer with offset = 0
        byte[] array = new byte[TEMP_BUFFER_SIZE];
        buffer.reset(array, 0, array.length);
        cb.append(buffer);
        
        fill(cb);
        OutputBuffer.ByteArrayCloner cloner =
                new OutputBuffer.ByteArrayCloner(buffer);
        
        Buffer newBuffer = cloner.clone0(mm, cb);
        clean(array);
        checkContent(newBuffer, 'A');
        
        
        // Test composite buffer with offset != 0
        
        cb = CompositeBuffer.newBuffer();

        array = new byte[TEMP_BUFFER_SIZE];
        int offset = 1111;
        buffer.reset(array, offset, array.length - offset);
        cb.append(buffer);
        
        fill(cb);
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, cb);
        clean(array);
        assertEquals(array.length - offset, newBuffer.remaining());
        checkContent(newBuffer, 'A');
        
        // Test composite buffer with offset != 0 and position > 0
        
        cb = CompositeBuffer.newBuffer();

        array = new byte[TEMP_BUFFER_SIZE];
        offset = 1111;
        int position = 15;
        
        buffer.reset(array, offset, array.length - offset);
        cb.append(buffer);
        
        fill(cb);
        cb.position(cb.position() + position);
        
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, cb);
        clean(array);
        assertEquals(array.length - offset - position, newBuffer.remaining());
        checkContent(newBuffer, 'A' + position);
    }
    
    /**
     * T - stands for TemporaryHeapBuffer
     * B - stands for any other Buffer
     */
    @Test
    public void testBTBCompositeBuffer() {
        CompositeBuffer cb = CompositeBuffer.newBuffer();
        TemporaryHeapBuffer buffer = new TemporaryHeapBuffer();
        
        // Test position = 0
        byte[] array = new byte[TEMP_BUFFER_SIZE];
        buffer.reset(array, 0, array.length);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));
        cb.append(buffer);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));
        
        fill(cb);
        OutputBuffer.ByteArrayCloner cloner =
                new OutputBuffer.ByteArrayCloner(buffer);
        
        Buffer newBuffer = cloner.clone0(mm, cb);
        clean(array);
        checkContent(newBuffer, 'A');
        
        // Test position in the middle of the first B
        cb = CompositeBuffer.newBuffer();
        array = new byte[TEMP_BUFFER_SIZE];
        int position = 15;

        buffer.reset(array, 0, array.length);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));
        cb.append(buffer);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));

        fill(cb);
        cb.position(cb.position() + position);
        
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, cb);
        clean(array);
        
        assertEquals(TEMP_BUFFER_SIZE + EXTRA_BUFFER_SIZE * 2 - position, newBuffer.remaining());
        checkContent(newBuffer, 'A' + position);
        
        // Test position in the middle of T
        cb = CompositeBuffer.newBuffer();
        array = new byte[TEMP_BUFFER_SIZE];
        position = EXTRA_BUFFER_SIZE + 15;

        buffer.reset(array, 0, array.length);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));
        cb.append(buffer);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));

        fill(cb);
        cb.position(cb.position() + position);
        
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, cb);
        clean(array);
        
        assertEquals(TEMP_BUFFER_SIZE + EXTRA_BUFFER_SIZE * 2 - position, newBuffer.remaining());
        checkContent(newBuffer, 'A' + position);
        
        // Test position in the middle of the second B
        cb = CompositeBuffer.newBuffer();
        array = new byte[TEMP_BUFFER_SIZE];
        position = EXTRA_BUFFER_SIZE + TEMP_BUFFER_SIZE + 15;

        buffer.reset(array, 0, array.length);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));
        cb.append(buffer);
        cb.append(MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(EXTRA_BUFFER_SIZE));

        fill(cb);
        cb.position(cb.position() + position);
        
        cloner = new OutputBuffer.ByteArrayCloner(buffer);
        
        newBuffer = cloner.clone0(mm, cb);
        clean(array);
        
        assertEquals(TEMP_BUFFER_SIZE + EXTRA_BUFFER_SIZE * 2 - position, newBuffer.remaining());
        checkContent(newBuffer, 'A' + position);               
    }
    
    @Test
    public void testTSplit() {
        int splitPos = TEMP_BUFFER_SIZE / 4;
        
        // Test offset = 0
        TemporaryHeapBuffer t1 = new TemporaryHeapBuffer();
        
        byte[] array = fill(new byte[TEMP_BUFFER_SIZE]);
        t1.reset(array, 0, array.length);
        
        Buffer t2 = t1.split(splitPos);
        clean(array);
        
        CompositeBuffer cb = CompositeBuffer.newBuffer();
        cb.append(t1);
        cb.append(t2);

        checkContent(cb, 'A');               
        
        // Test with offset
        
        int offset = 345;
        t1 = new TemporaryHeapBuffer();
        
        array = fill(new byte[TEMP_BUFFER_SIZE]);
        t1.reset(array, offset, array.length - offset);
        
        t2 = t1.split(splitPos);
        clean(array);
        
        cb = CompositeBuffer.newBuffer();
        cb.append(t1);
        cb.append(t2);

        assertEquals(TEMP_BUFFER_SIZE - offset, cb.remaining());
        checkContent(cb, 'A' + offset);               
        
    }
    
    private void checkContent(final Buffer b,
            final int startSym) {
        
        final int pos = b.position();
        int a = (startSym - 'A') % ('Z' - 'A');
        while (b.hasRemaining()) {
            assertEquals((char) ('A' + a), (char) b.get());
            a = (++a) % ('Z' - 'A');
        }
        
        b.position(pos);
        
    }
    
    private Buffer fill(final Buffer b) {
        final int pos = b.position();
        int a = 0;
        while (b.hasRemaining()) {
            b.put((byte) ('A' + a));
            a = (++a) % ('Z' - 'A');
        }
        
        b.position(pos);
        
        return b;
    }
    
    private byte[] fill(final byte[] array) {
        int a = 0;
        for (int i = 0; i < array.length; i++) {
            array[i] = ((byte) ('A' + a));
            a = (++a) % ('Z' - 'A');
        }
        
        return array;
    }
    
    
    private void clean(final byte[] array) {
        Arrays.fill(array, 0, array.length, (byte) 0);
    }

    private void clean(final TemporaryHeapBuffer b) {
        final int pos = b.position();
        int a = 0;
        while (b.hasRemaining()) {
            b.put((byte) 0);
        }
        
        b.position(pos);
    }
}
