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

package org.glassfish.grizzly.compression.lzma.impl.lz;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.compression.lzma.LZMADecoder;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * OutWindow
 *
 * @author Igor Pavlov
 */
public class OutWindow {

    LZMADecoder.LZMAInputState _decoderState;
    byte[] _buffer;
    int _pos;
    int _windowSize = 0;
    int _streamPos;

    public void create(int windowSize) {
        if (_buffer == null || _windowSize != windowSize) {
            _buffer = new byte[windowSize];
        }
        _windowSize = windowSize;
        _pos = 0;
        _streamPos = 0;
    }

    public void initFromState(LZMADecoder.LZMAInputState decoderState) throws IOException {
        _decoderState = decoderState;
    }

    public void releaseBuffer() throws IOException {
        // Flush();
        _decoderState = null;
    }

    public void init(boolean solid) {
        if (!solid) {
            _streamPos = 0;
            _pos = 0;
        }
    }

    public void flush() throws IOException {
        int size = _pos - _streamPos;
        if (size == 0) {
            return;
        }

        Buffer dst = _decoderState.getDst();

        if (dst == null || dst.remaining() < size) {
            dst = resizeBuffer(_decoderState.getMemoryManager(), dst, size);
            _decoderState.setDst(dst);
        }
        dst.put(_buffer, _streamPos, size);
        dst.trim();
        dst.position(dst.limit());

        if (_pos >= _windowSize) {
            _pos = 0;
        }
        _streamPos = _pos;
    }

    public void copyBlock(int distance, int len) throws IOException {
        int pos = _pos - distance - 1;
        if (pos < 0) {
            pos += _windowSize;
        }
        for (; len > 0; len--) {
            if (pos >= _windowSize) {
                pos = 0;
            }
            _buffer[_pos++] = _buffer[pos++];
            if (_pos >= _windowSize) {
                flush();
            }
        }
    }

    public void putByte(byte b) throws IOException {
        _buffer[_pos++] = b;
        if (_pos >= _windowSize) {
            flush();
        }
    }

    public byte getByte(int distance) {
        int pos = _pos - distance - 1;
        if (pos < 0) {
            pos += _windowSize;
        }
        return _buffer[pos];
    }

    @SuppressWarnings({ "unchecked" })
    private static Buffer resizeBuffer(final MemoryManager memoryManager, final Buffer buffer, final int grow) {
        if (buffer == null) {
            return memoryManager.allocate(Math.max(grow, 4096));
        }

        return memoryManager.reallocate(buffer, Math.max(buffer.capacity() + grow, buffer.capacity() * 3 / 2 + 1));
    }

}
