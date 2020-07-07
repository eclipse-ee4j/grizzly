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

package org.glassfish.grizzly;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.BufferInputStream;
import org.glassfish.grizzly.utils.BufferOutputStream;
import org.junit.Test;

/**
 * Testing {@link BufferInputStream} and {@link BufferOutputStream}.
 *
 * @author Alexey Stashok
 */
public class InputOutputStreamTest {
    @Test
    public void testInputStream() throws IOException {
        final MemoryManager mm = MemoryManager.DEFAULT_MEMORY_MANAGER;

        Buffer b = mm.allocate(10);
        b.put((byte) 0x1);
        b.put((byte) 0xFF);

        byte[] bytes = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8 };

        b.put(bytes);

        b.flip();

        BufferInputStream bis = new BufferInputStream(b);

        assertEquals(0x1, bis.read());
        assertEquals(0xFF, bis.read());

        byte[] readBytes = new byte[bytes.length];
        bis.read(readBytes);

        assertArrayEquals(bytes, readBytes);
    }

    @Test
    public void testOutputStreamReallocate() throws IOException {
        final MemoryManager mm = MemoryManager.DEFAULT_MEMORY_MANAGER;

        final byte[] initialBytes = "initial info".getBytes("ASCII");
        final Buffer initialBuffer = mm.allocate(initialBytes.length * 2);
        initialBuffer.put(initialBytes);

        BufferOutputStream bos = new BufferOutputStream(mm, initialBuffer, true);

        for (int i = 0; i < 9; i++) {
            final byte[] b = new byte[32768];
            Arrays.fill(b, (byte) i);
            bos.write(b);
        }

        bos.close();

        final Buffer resultBuffer = bos.getBuffer().flip();

        byte[] initialCheckBytes = new byte[initialBytes.length];
        resultBuffer.get(initialCheckBytes);
        assertArrayEquals(initialBytes, initialCheckBytes);

        for (int i = 0; i < 9; i++) {
            final byte[] pattern = new byte[32768];
            Arrays.fill(pattern, (byte) i);

            final byte[] b = new byte[32768];
            resultBuffer.get(b);
            assertArrayEquals(pattern, b);
        }
    }

    @Test
    public void testOutputStreamWithoutReallocate() throws IOException {
        final MemoryManager mm = MemoryManager.DEFAULT_MEMORY_MANAGER;

        final byte[] initialBytes = "initial info".getBytes("ASCII");
        final Buffer initialBuffer = mm.allocate(initialBytes.length * 2);
        initialBuffer.put(initialBytes);

        BufferOutputStream bos = new BufferOutputStream(mm, initialBuffer, false);

        for (int i = 0; i < 9; i++) {
            final byte[] b = new byte[32768];
            Arrays.fill(b, (byte) i);
            bos.write(b);
        }

        bos.close();

        final Buffer resultBuffer = bos.getBuffer().flip();

        assertTrue(resultBuffer.isComposite());

        byte[] initialCheckBytes = new byte[initialBytes.length];
        resultBuffer.get(initialCheckBytes);
        assertArrayEquals(initialBytes, initialCheckBytes);

        for (int i = 0; i < 9; i++) {
            final byte[] pattern = new byte[32768];
            Arrays.fill(pattern, (byte) i);

            final byte[] b = new byte[32768];
            resultBuffer.get(b);
            assertArrayEquals(pattern, b);
        }
    }
}
