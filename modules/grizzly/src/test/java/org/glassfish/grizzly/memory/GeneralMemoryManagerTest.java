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

package org.glassfish.grizzly.memory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

import org.glassfish.grizzly.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class GeneralMemoryManagerTest extends AbstractMemoryManagerTest {


    // ------------------------------------------------------------ Constructors


    public GeneralMemoryManagerTest(int mmType) {
        super(mmType);
    }


    // ------------------------------------------------------------ Test Methods

    @Test
    public void testBufferEquals() {
        final HeapMemoryManager hmm = new HeapMemoryManager();
        final ByteBufferManager bbm = new ByteBufferManager();
        final PooledMemoryManager pmm = new PooledMemoryManager();

        Buffer[] buffers = new Buffer[4];
        buffers[0] = Buffers.wrap(hmm, "Value#1");
        buffers[1] = Buffers.wrap(bbm, "Value#1");
        buffers[2] = Buffers.wrap(pmm, "Value#1");

        Buffer b11 = Buffers.wrap(hmm, "Val");
        Buffer b12 = Buffers.wrap(bbm, "ue");
        Buffer b13 = Buffers.wrap(pmm, "#1");

        Buffer tmp = Buffers.appendBuffers(bbm, b11, b12);
        buffers[3] = Buffers.appendBuffers(bbm, tmp, b13);

        for (int i = 0; i < buffers.length; i++) {
            for (int j = 0; j < buffers.length; j++) {
                assertEquals(buffers[i], buffers[j]);
            }
        }
    }

    @Test
    public void testBufferPut() {
        final Buffer b = mm.allocate(127);
        if (!(b instanceof HeapBuffer)) {
            return;
        }

        int i = 0;
        while (b.hasRemaining()) {
            b.put((byte) i++);
        }

        b.flip();

        b.put(b, 10, 127 - 10).flip();


        assertEquals(127 - 10, b.remaining());

        i = 10;
        while (b.hasRemaining()) {
            assertEquals(i++, b.get());
        }
    }

    @Test
    public void testBufferSlice() {
        Buffer b = mm.allocate(10);
        b.putInt(1);
        ByteBuffer bb = b.slice().toByteBuffer().slice();
        bb.rewind();
        bb.putInt(2);
        b.rewind();
        assertEquals(1, b.getInt());
    }

    @Test
    public void testBufferSplitWithMark() {
        Buffer b = mm.allocate(100);
        b.position(10);
        b.mark();
        Buffer newBuffer = b.split(15);
        assertMarkExceptionThrown(newBuffer);

        b.position(12);
        b.reset();
        assertEquals(10, b.position());

        newBuffer = b.split(5);
        assertMarkExceptionThrown(b);
        assertMarkExceptionThrown(newBuffer);
    }

}
