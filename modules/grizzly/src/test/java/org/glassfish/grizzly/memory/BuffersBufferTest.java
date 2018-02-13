/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class BuffersBufferTest extends AbstractMemoryManagerTest {

    public BuffersBufferTest(int mmType) {
        super(mmType);
    }


    // ------------------------------------------------------------ Test Methods

    @Test
    public void testCharEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putChar('a');
        buffer.flip();
        assertEquals("big endian", 'a', buffer.getChar());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putChar('a');
        buffer.flip();
        assertEquals("little endian", 'a', buffer.getChar());
    }

    @Test
    public void testShortEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.flip();
        assertEquals("big endian", ((short) 1), buffer.getShort());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 1);
        buffer.flip();
        assertEquals("little endian", ((short) 1), buffer.getShort());
    }

    @Test
    public void testIntEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putInt(1);
        buffer.flip();
        assertEquals("big endian", 1, buffer.getInt());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1);
        buffer.flip();
        assertEquals("little endian", 1, buffer.getInt());
    }

    @Test
    public void testLongEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putLong(1L);
        buffer.flip();
        assertEquals("big endian", 1, buffer.getLong());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(1L);
        buffer.flip();
        assertEquals("little endian", 1, buffer.getLong());
    }

    @Test
    public void testFloatEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putFloat(1.0f);
        buffer.flip();
        assertEquals("big endian", 1.f, 1.0f, buffer.getFloat());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(1.0f);
        buffer.flip();
        assertEquals("little endian", 1.0f, 1.0f, buffer.getFloat());
    }

    @Test
    public void testDoubleEndianess() {
        BuffersBuffer buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.putDouble(1.0d);
        buffer.flip();
        assertEquals("big endian", 1.0d, 1.0d, buffer.getDouble());
        buffer = createOneSevenBuffer(mm);
        assertTrue(buffer.order() == ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(buffer.order() == ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(1.0d);
        buffer.flip();
        assertEquals("little endian", 1.0d, 1.0d, buffer.getDouble());
    }

    @Test
    public void testMarkAndReset() {
        final BuffersBuffer buffer = createOneSevenBuffer(mm);

        buffer.putShort((short)0);
        buffer.putShort((short) 1);
        buffer.mark();
        buffer.putShort((short)2);
        buffer.putShort((short) 3);
        assertTrue(buffer.remaining() == 0);
        final int lastPosition = buffer.position();

        buffer.reset();
        assertTrue(lastPosition != buffer.position());
        assertEquals(2, buffer.getShort());

        buffer.reset();
        assertEquals(2, buffer.getShort());
        assertEquals(3, buffer.getShort());

        buffer.flip();
        assertEquals(0, buffer.getShort());
        buffer.mark();
        assertEquals(1, buffer.getShort());
        assertEquals(2, buffer.getShort());

        buffer.reset();
        assertEquals(1, buffer.getShort());
        assertEquals(2, buffer.getShort());
        assertEquals(3, buffer.getShort());

        assertEquals(lastPosition, buffer.position());
        buffer.mark();
        buffer.position(2); // mark should be reset because of mark > position
        assertEquals(1, buffer.getShort());
        try {
            buffer.reset(); // exception should be thrown
            fail();
        } catch (InvalidMarkException ignore) {
        }

        assertEquals(2, buffer.getShort());
        buffer.mark();
        assertEquals(3, buffer.getShort());
        buffer.reset();
        assertEquals(3, buffer.getShort());

        buffer.flip(); // mark should be reset
        assertEquals(0, buffer.getShort());
        try {
            buffer.reset();
            fail(); // exception should be thrown because mark was already reset
        } catch (InvalidMarkException ignore) {
        }
        assertEquals(1, buffer.getShort());
    }

    @Test
    public void testBulkByteBufferGetWithEmptyBuffers() throws Exception {
        BuffersBuffer b = BuffersBuffer.create(mm);
        b.append(Buffers.wrap(mm, "Hello "));
        b.append(BuffersBuffer.create(mm));
        b.append(Buffers.wrap(mm, "world!"));
        
        ByteBuffer buffer = ByteBuffer.allocate(12);
        b.get(buffer);
        buffer.flip();
        assertEquals("Hello world!", Charsets.getCharsetDecoder(Charsets.UTF8_CHARSET).decode(buffer).toString());
    }

    @Test
    public void testBulkArrayGetWithEmptyBuffers() throws Exception {
        BuffersBuffer b = BuffersBuffer.create(mm);
        b.append(Buffers.wrap(mm, "Hello "));
        b.append(BuffersBuffer.create(mm));
        b.append(Buffers.wrap(mm, "world!"));

        byte[] bytes = new byte[12];
        b.get(bytes);
        assertEquals("Hello world!", new String(bytes));
    }


    // ------------------------------------------------------- Protected Methods



    // --------------------------------------------------------- Private Methods


    private static BuffersBuffer createOneSevenBuffer(final MemoryManager mm) {
        final BuffersBuffer b = BuffersBuffer.create(mm);
        b.append(mm.allocate(7).limit(1));
        b.append(mm.allocate(7));
        return b;
    }
}
