/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.http.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Efficient conversion of character to bytes.
 *
 * Now uses NIO directly
 */

public class C2BConverter {

    private static final Logger logger = Grizzly.logger(C2BConverter.class);
    protected ByteChunk bb;
    protected final String enc;
    protected final CharsetEncoder encoder;

    /**
     * Create a converter, with bytes going to a byte buffer
     */
    public C2BConverter(ByteChunk output, String encoding) throws IOException {
        this.bb = output;
        this.enc = encoding;
        encoder = Charsets.lookupCharset(enc).newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    /**
     * Create a converter
     */
    public C2BConverter(String encoding) throws IOException {
        this(new ByteChunk(1024), encoding);
    }

    public static C2BConverter getInstance(ByteChunk output, String encoding) throws IOException {
        return new C2BConverter(output, encoding);
    }

    public ByteChunk getByteChunk() {
        return bb;
    }

    public String getEncoding() {
        return enc;
    }

    public void setByteChunk(ByteChunk bb) {
        this.bb = bb;
    }

    /**
     * Reset the internal state, empty the buffers. The encoding remain in effect, the internal buffers remain allocated.
     */
    public void recycle() {
        bb.recycle();
    }

    /**
     * Generate the bytes using the specified encoding
     */
    public void convert(char c[], int off, int len) throws IOException {
        CharBuffer cb = CharBuffer.wrap(c, off, len);
        byte[] barr = bb.getBuffer();
        int boff = bb.getEnd();
        ByteBuffer tmp = ByteBuffer.wrap(barr, boff, barr.length - boff);
        CoderResult cr = encoder.encode(cb, tmp, true);
        bb.setEnd(tmp.position());
        while (cr == CoderResult.OVERFLOW) {
            if (!bb.canGrow()) {
                bb.flushBuffer();
            }
            boff = bb.getEnd();
            barr = bb.getBuffer();
            tmp = ByteBuffer.wrap(barr, boff, barr.length - boff);
            cr = encoder.encode(cb, tmp, true);
            bb.setEnd(tmp.position());
        }
        if (cr != CoderResult.UNDERFLOW) {
            throw new IOException("Encoding error");
        }
    }

    /**
     * Generate the bytes using the specified encoding
     */
    public void convert(String s) throws IOException {
        convert(s, 0, s.length());
    }

    /**
     * Generate the bytes using the specified encoding
     */
    public void convert(String s, int off, int len) throws IOException {
        convert(s.toCharArray(), off, len);
    }

    /**
     * Generate the bytes using the specified encoding
     */
    public void convert(char c) throws IOException {
        char[] tmp = new char[1];
        tmp[0] = c;
        convert(tmp, 0, 1);
    }

    /**
     * Convert a message bytes chars to bytes
     */
    public void convert(MessageBytes mb) throws IOException {
        int type = mb.getType();
        if (type == MessageBytes.T_BYTES) {
            return;
        }
        ByteChunk orig = bb;
        setByteChunk(mb.getByteChunk());
        bb.recycle();
        bb.allocate(32, -1);

        if (type == MessageBytes.T_STR) {
            convert(mb.getString());
            // System.out.println("XXX Converting " + mb.getString() );
        } else if (type == MessageBytes.T_CHARS) {
            CharChunk charC = mb.getCharChunk();
            convert(charC.getBuffer(), charC.getStart(), charC.getLength());
            // System.out.println("XXX Converting " + mb.getCharChunk() );
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "XXX unknowon type {0}", type);
            }
        }
        // System.out.println("C2B: XXX " + bb.getBuffer() + bb.getLength());
        setByteChunk(orig);
    }

    /**
     * Flush any internal buffers into the ByteOutput or the internal byte[]
     */
    public void flushBuffer() throws IOException {
        bb.flushBuffer();
    }

}
