/*
 * Copyright (c) 2016, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

import static org.junit.Assert.assertEquals;

import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;

public class ChunkTest {

    private final static String CONTENT = "    one fish,\ttwo fish, \rred fish,  \nblue fish";
    private final static char[] CONTENT_CHARS = CONTENT.toCharArray();
    private final static byte[] CONTENT_BYTES = CONTENT.getBytes(Charsets.UTF8_CHARSET);
    private final static String TRIM1 = "one fish,\ttwo fish, \rred fish,  \nblue fish";
    private final static String TRIM2 = "two fish, \rred fish,  \nblue fish";
    private final static String TRIM3 = "red fish,  \nblue fish";
    private final static String TRIM4 = "blue fish";

    // ---------------------------------------------------------------------------------------------------- Test Methods

    @Test
    public void testTrimLeft() throws Exception {
        DataChunk dc = DataChunk.newInstance();
        dc.setChars(CONTENT_CHARS, 0, CONTENT_CHARS.length);
        trimAndAssertCorrect(dc);

        dc.setBytes(CONTENT_BYTES, 0, CONTENT_BYTES.length);
        trimAndAssertCorrect(dc);

        dc.setBuffer(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, CONTENT_BYTES));
        trimAndAssertCorrect(dc);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static void trimAndAssertCorrect(final DataChunk dc) {
        assertEquals(CONTENT, dc.toString(Charsets.UTF8_CHARSET));

        dc.trimLeft();
        assertEquals(TRIM1, dc.toString(Charsets.UTF8_CHARSET));

        dc.setStart(dc.getStart() + dc.indexOf(',', 0) + 1);
        dc.trimLeft();
        assertEquals(TRIM2, dc.toString(Charsets.UTF8_CHARSET));

        dc.setStart(dc.getStart() + dc.indexOf(',', 0) + 1);
        dc.trimLeft();
        assertEquals(TRIM3, dc.toString(Charsets.UTF8_CHARSET));

        dc.setStart(dc.getStart() + dc.indexOf(',', 0) + 1);
        dc.trimLeft();
        assertEquals(TRIM4, dc.toString(Charsets.UTF8_CHARSET));
    }
}
