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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;

import org.glassfish.grizzly.utils.Charsets;

import junit.framework.TestCase;

/**
 * {@link Charsets} test.
 *
 * @author Alexey Stashok
 */
public class CharsetsTest extends TestCase {
    private final Charset[] charsets = new Charset[] { Charsets.UTF8_CHARSET, Charset.forName("UTF-16"), Charset.forName("UTF-32"), Charsets.ASCII_CHARSET,
            Charset.forName("GB2312") };

    public void testDecodersCache() {
        final CharsetDecoder decoder0 = Charsets.getCharsetDecoder(charsets[0]);
        final CharsetDecoder decoder1 = Charsets.getCharsetDecoder(charsets[1]);
        final CharsetDecoder decoder2 = Charsets.getCharsetDecoder(charsets[2]);
        final CharsetDecoder decoder3 = Charsets.getCharsetDecoder(charsets[3]);

        assertTrue("Decoder is not the same", decoder0 == Charsets.getCharsetDecoder(charsets[0]));
        assertTrue("Decoder is not the same", decoder1 == Charsets.getCharsetDecoder(charsets[1]));
        assertTrue("Decoder is not the same", decoder2 == Charsets.getCharsetDecoder(charsets[2]));
        assertTrue("Decoder is not the same", decoder3 == Charsets.getCharsetDecoder(charsets[3]));

        final CharsetDecoder decoder4 = Charsets.getCharsetDecoder(charsets[4]);

        assertTrue("Decoder should be different", decoder0 != Charsets.getCharsetDecoder(charsets[0]));

        assertTrue("Decoder is not the same", decoder4 == Charsets.getCharsetDecoder(charsets[4]));
    }

    public void testEncodersCache() {
        final CharsetEncoder encoder0 = Charsets.getCharsetEncoder(charsets[0]);
        final CharsetEncoder encoder1 = Charsets.getCharsetEncoder(charsets[1]);
        final CharsetEncoder encoder2 = Charsets.getCharsetEncoder(charsets[2]);
        final CharsetEncoder encoder3 = Charsets.getCharsetEncoder(charsets[3]);

        assertTrue("Encoder is not the same", encoder0 == Charsets.getCharsetEncoder(charsets[0]));
        assertTrue("Encoder is not the same", encoder1 == Charsets.getCharsetEncoder(charsets[1]));
        assertTrue("Encoder is not the same", encoder2 == Charsets.getCharsetEncoder(charsets[2]));
        assertTrue("Encoder is not the same", encoder3 == Charsets.getCharsetEncoder(charsets[3]));

        final CharsetEncoder encoder4 = Charsets.getCharsetEncoder(charsets[4]);

        assertTrue("Encoder should be different", encoder0 != Charsets.getCharsetEncoder(charsets[0]));

        assertTrue("Encoder is not the same", encoder4 == Charsets.getCharsetEncoder(charsets[4]));
    }

    public void testPreloadedCharsets() {
        Charsets.preloadAllCharsets();
        try {
            Charsets.lookupCharset("NON-EXISTED-CHARSET");
        } catch (UnsupportedCharsetException e) {
            StackTraceElement[] elements = e.getStackTrace();
            assertEquals("Exception is not thrown from Charsets class", elements[0].getClassName(), Charsets.class.getName());
        }

        Charsets.drainAllCharsets();

        try {
            Charsets.lookupCharset("NON-EXISTED-CHARSET");
        } catch (UnsupportedCharsetException e) {
            StackTraceElement[] elements = e.getStackTrace();
            assertFalse("Exception is unexpectedly thrown from Charsets class", elements[0].getClassName().equals(Charsets.class.getName()));
        }

    }
}
