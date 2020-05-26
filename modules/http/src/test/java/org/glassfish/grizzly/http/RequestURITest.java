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

package org.glassfish.grizzly.http;

import java.io.CharConversionException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.Constants;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.RequestURIRef;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.Charsets;

import junit.framework.TestCase;

/**
 * Test the {@link RequestURIRef} decoding.
 * 
 * @author Alexey Stashok
 */
public class RequestURITest extends TestCase {
    private final String rus = "\u043F\u0440\u0438\u0432\u0435\u0442\u043C\u0438\u0440";
    private final String rusEncoded;
    private final String url;
    private Buffer buffer;

    public RequestURITest() throws Exception {
        rusEncoded = URLEncoder.encode(rus, "UTF-8");
        url = "http://localhost:4848/management/domain/resources/jdbc-resource/" + rusEncoded + "/jdbc%2F__TimerPool.xml";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        buffer = Buffers.wrap(null, url);
    }

    public void testBufferChunk() throws Exception {
        RequestURIRef rur = new RequestURIRef();
        DataChunk originalURIDataChunk = rur.getOriginalRequestURIBC();
        assertTrue(originalURIDataChunk.isNull());

        rur.init(buffer, 0, buffer.capacity());

        try {
            rur.getDecodedRequestURIBC(false);
            fail("Exception must be thrown");
        } catch (CharConversionException e) {
        }

        // Try wrong charset
        DataChunk decodedDC = rur.getDecodedRequestURIBC(true, Constants.DEFAULT_HTTP_CHARSET);
        assertEquals(DataChunk.Type.Chars, decodedDC.getType());
        // there shouldn't be our decoded word
        assertEquals(-1, decodedDC.toString().indexOf(rus));

        // Try correct charset
        decodedDC = rur.getDecodedRequestURIBC(true, Charsets.UTF8_CHARSET);
        assertEquals(DataChunk.Type.Chars, decodedDC.getType());
        // there should be our decoded word
        assertTrue(decodedDC.toString().contains(rus));

        // One more time the same
        decodedDC = rur.getDecodedRequestURIBC(true, Charsets.UTF8_CHARSET);
        assertEquals(DataChunk.Type.Chars, decodedDC.getType());
        // there should be our decoded word
        assertTrue(decodedDC.toString().contains(rus));

        // there shouldn't be our decoded word
        assertTrue(!rur.getURI().contains(rus));

        // Original should be the same
        assertEquals(url, rur.getOriginalRequestURIBC().toString());
    }

    public void testDefaultEncoding() throws Exception {
        String pattern = new String(new byte[] { '/', (byte) 0x82, (byte) 0xc4, (byte) 0x82, (byte) 0xb7, (byte) 0x82, (byte) 0xc6, '.', 'j', 's', 'p' },
                "Shift_JIS");

        final Buffer b = Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, "/%82%c4%82%b7%82%c6.jsp");

        RequestURIRef rur = new RequestURIRef();

        rur.init(b, 0, b.capacity());

        try {
            rur.getDecodedRequestURIBC(false);
            fail("Exception must be thrown");
        } catch (CharConversionException e) {
        }

        rur.setDefaultURIEncoding(Charset.forName("Shift_JIS"));

        assertEquals(pattern, rur.getDecodedURI());
    }

    public void testURIChangeTrigger() {
        RequestURIRef rur = new RequestURIRef();
        rur.init(buffer, 0, buffer.capacity());

        final DataChunk originalRequestURIBC = rur.getOriginalRequestURIBC();
        final DataChunk actualRequestURIBC = rur.getRequestURIBC();

        assertTrue(originalRequestURIBC.getBufferChunk().getBuffer() == actualRequestURIBC.getBufferChunk().getBuffer());

        actualRequestURIBC.notifyDirectUpdate();

        assertEquals(DataChunk.Type.Bytes, actualRequestURIBC.getType());

        final ByteChunk actualByteChunk = actualRequestURIBC.getByteChunk();
        actualByteChunk.delete(0, 7);

        assertEquals(url, originalRequestURIBC.toString());
        assertEquals(url.substring(7), actualRequestURIBC.toString());
    }
}
