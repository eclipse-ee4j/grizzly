/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.fail;

import java.nio.InvalidMarkException;
import java.util.Arrays;
import java.util.Collection;

import org.glassfish.grizzly.Buffer;
import org.junit.runners.Parameterized;

public class AbstractMemoryManagerTest {

    protected final MemoryManager mm;

    @Parameterized.Parameters
    public static Collection<Object[]> getOptimizedForMultiplexing() {
        return Arrays.asList(new Object[][] { { 0 }, { 1 }, { 2 }, });
    }

    public AbstractMemoryManagerTest(final int mmType) {
        switch (mmType) {
        case 0:
            mm = createHeapMemoryManager();
            break;
        case 1:
            mm = createByteBufferManager();
            break;
        case 2:
            mm = createPooledMemoryManager();
            break;
        default:
            throw new IllegalStateException("Unknown memory manager type");
        }
    }

    protected PooledMemoryManager createPooledMemoryManager() {
        return new PooledMemoryManager();
    }

    protected ByteBufferManager createByteBufferManager() {
        return new ByteBufferManager();
    }

    protected HeapMemoryManager createHeapMemoryManager() {
        return new HeapMemoryManager();
    }

    protected static void assertMarkExceptionThrown(final Buffer bufferToTest) {
        try {
            bufferToTest.reset(); // mark never carried over to the split buffer.
            fail();
        } catch (InvalidMarkException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

}
