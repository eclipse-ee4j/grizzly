/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.hpack;

import org.glassfish.grizzly.Buffer;

import java.io.IOException;

//
// Custom implementation of ISO/IEC 8859-1:1998
//
// The rationale behind this is not to deal with CharsetEncoder/CharsetDecoder,
// basically because it would require wrapping every single CharSequence into a
// CharBuffer and then copying it back.
//
// But why not to give a CharBuffer instead of Appendable? Because I can choose
// an Appendable (e.g. StringBuilder) that adjusts its length when needed and
// therefore not to deal with pre-sized CharBuffers or copying.
//
// The encoding is simple and well known: 1 byte <-> 1 char
//
final class ISO_8859_1 {

    private ISO_8859_1() { }

    public static final class Reader {

        public void read(Buffer source, Appendable destination) {
            for (int i = 0, len = source.remaining(); i < len; i++) {
                char c = (char) (source.get() & 0xff);
                try {
                    destination.append(c);
                } catch (IOException e) {
                    throw new RuntimeException
                            ("Error appending to the destination", e);
                }
            }
        }

        public Reader reset() {
            return this;
        }
    }

    public static final class Writer {

        private CharSequence source;
        private int pos;
        private int end;

        public Writer configure(CharSequence source, int start, int end) {
            this.source = source;
            this.pos = start;
            this.end = end;
            return this;
        }

        public boolean write(Buffer destination) {
            for (; pos < end; pos++) {
                char c = source.charAt(pos);
                if (c > '\u00FF') {
                    throw new IllegalArgumentException(
                            "Illegal ISO-8859-1 char: " + (int) c);
                }
                if (destination.hasRemaining()) {
                    destination.put((byte) c);
                } else {
                    return false;
                }
            }
            return true;
        }

        public Writer reset() {
            source = null;
            pos = -1;
            end = -1;
            return this;
        }
    }
}
