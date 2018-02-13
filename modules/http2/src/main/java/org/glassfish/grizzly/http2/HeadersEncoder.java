/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http2.hpack.Encoder;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

import java.util.Map;

/**
 *
 * @author oleksiys
 */
public class HeadersEncoder {

    private static final String DEFAULT_BUFFER_SIZE_PROP_NAME =
            "org.glassfish.grizzly.http2.HeadersEncoder.DEFAULT_BUFFER_SIZE";
    private static final String DEFAULT_BUFFER_SIZE_STRING = "8192";

    private static final int DEFAULT_BUFFER_SIZE =
            Integer.parseInt(System.getProperty(DEFAULT_BUFFER_SIZE_PROP_NAME, DEFAULT_BUFFER_SIZE_STRING));

    private final Encoder hpackEncoder;
    private final MemoryManager memoryManager;

    private CompositeBuffer buffer;

    public HeadersEncoder(final MemoryManager memoryManager,
                          final int maxHeaderTableSize) {
        this.memoryManager = memoryManager;
        hpackEncoder = new Encoder(maxHeaderTableSize);
    }
    
    public void encodeHeader(final String name, final String value, final Map<String,String> capture) {
        if (capture != null) {
            capture.put(name, value);
        }
        init();
        hpackEncoder.header(name, value);
        while (!hpackEncoder.encode(buffer)) {
            buffer.append(memoryManager.allocate(DEFAULT_BUFFER_SIZE));
        }
    }
    
    public Buffer flushHeaders() {
        final Buffer bufferLocal = buffer;
        bufferLocal.trim();
        buffer = null;

        return bufferLocal;
    }

    private void init() {
        if (buffer == null) {
            buffer = CompositeBuffer.newBuffer(memoryManager);
            buffer.allowInternalBuffersDispose(true);
            buffer.allowBufferDispose(true);
            buffer.append(memoryManager.allocate(DEFAULT_BUFFER_SIZE));
        }
    }
}
