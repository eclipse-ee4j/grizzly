/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.URLDecoder;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;

import junit.framework.TestCase;

/**
 * Parse URL decoder
 *
 * @author Alexey Stashok
 */
public class URIDecoderTest extends TestCase {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public void testURLNoChangeBB() throws Exception {
        testDecoder("http://localhost:8080/helloworld");
    }

    public void testURLSpaceBB() throws Exception {
        testDecoder("http://localhost:8080/hello world");
    }

    public void testURLUTFBB() throws Exception {
        String s = "http://localhost:8080/\u043F\u0440\u0438\u0432\u0435\u0442 \u043C\u0438\u0440";

        testDecoder(s);
    }

    private void testDecoder(String inputURI) throws Exception {
        testBufferDecoder(inputURI);
        testStringDecoder(inputURI);
        testCharsDecoder(inputURI);
    }

    @SuppressWarnings({ "unchecked" })
    private void testBufferDecoder(String inputURI) throws Exception {

        MemoryManager mm = MemoryManager.DEFAULT_MEMORY_MANAGER;
        String encodedURI = URLEncoder.encode(inputURI, UTF8_CHARSET.name());

        Buffer b = Buffers.wrap(mm, encodedURI);

        DataChunk bufferChunk = DataChunk.newInstance();
        bufferChunk.setBuffer(b, b.position(), b.limit());

        URLDecoder.decode(bufferChunk);

        String decodedURI = bufferChunk.toString(UTF8_CHARSET);

        assertEquals(inputURI, decodedURI);
    }

    @SuppressWarnings({ "unchecked" })
    private void testStringDecoder(String inputURI) throws Exception {

        String encodedURI = URLEncoder.encode(inputURI, UTF8_CHARSET.name());

        String decodedURI = URLDecoder.decode(encodedURI, true, UTF8_CHARSET.name());

        assertEquals(inputURI, decodedURI);
    }

    @SuppressWarnings({ "unchecked" })
    private void testCharsDecoder(String inputURI) throws Exception {

        String encodedURI = URLEncoder.encode(inputURI, UTF8_CHARSET.name());

        DataChunk dataChunk = DataChunk.newInstance();
        final char[] encodedCA = encodedURI.toCharArray();
        dataChunk.setChars(encodedCA, 0, encodedCA.length);

        URLDecoder.decode(dataChunk, dataChunk, true, UTF8_CHARSET.name());

        assertEquals(inputURI, dataChunk.toString());
    }

}
