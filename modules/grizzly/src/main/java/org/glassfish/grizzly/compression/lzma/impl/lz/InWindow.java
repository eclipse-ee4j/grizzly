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

/**
 * InWindow
 *
 * @author Igor Pavlov
 */
public class InWindow {

    public byte[] _bufferBase; // pointer to buffer with data
    Buffer _buffer;
    int _posLimit; // offset (from _buffer) of first byte when new block reading must be done
    boolean _streamEndWasReached; // if (true) then _streamPos shows real end of stream
    int _pointerToLastSafePosition;
    public int _bufferOffset;
    public int _blockSize; // Size of Allocated memory block
    public int _pos; // offset (from _buffer) of curent byte
    int _keepSizeBefore; // how many BYTEs must be kept in buffer before _pos
    int _keepSizeAfter; // how many BYTEs must be kept buffer after _pos
    public int _streamPos; // offset (from _buffer) of first not read byte from Stream

    public void moveBlock() {
        int offset = _bufferOffset + _pos - _keepSizeBefore;
        // we need one additional byte, since movePos moves on 1 byte.
        if (offset > 0) {
            offset--;
        }

        int numBytes = _bufferOffset + _streamPos - offset;

        // check negative offset ????
        System.arraycopy(_bufferBase, offset, _bufferBase, 0, numBytes);
        _bufferOffset -= offset;
    }

    public void readBlock() throws IOException {
        if (_streamEndWasReached) {
            return;
        }
        while (true) {
            int size = 0 - _bufferOffset + _blockSize - _streamPos;
            if (size == 0) {
                return;
            }
            int pos = _buffer.position();
            size = Math.min(size, _buffer.remaining());
            _buffer.get(_bufferBase, _bufferOffset + _streamPos, size);
            int numReadBytes = _buffer.position() - pos;
            if (numReadBytes == 0) {
                _posLimit = _streamPos;
                int pointerToPostion = _bufferOffset + _posLimit;
                if (pointerToPostion > _pointerToLastSafePosition) {
                    _posLimit = _pointerToLastSafePosition - _bufferOffset;
                }

                _streamEndWasReached = true;
                return;
            }
            _streamPos += numReadBytes;
            if (_streamPos >= _pos + _keepSizeAfter) {
                _posLimit = _streamPos - _keepSizeAfter;
            }
        }
    }

    void free() {
        _bufferBase = null;
    }

    public void create(int keepSizeBefore, int keepSizeAfter, int keepSizeReserv) {
        _keepSizeBefore = keepSizeBefore;
        _keepSizeAfter = keepSizeAfter;
        int blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv;
        if (_bufferBase == null || _blockSize != blockSize) {
            free();
            _blockSize = blockSize;
            _bufferBase = new byte[_blockSize];
        }
        _pointerToLastSafePosition = _blockSize - keepSizeAfter;
    }

    public void setBuffer(Buffer buffer) {
        _buffer = buffer;
    }

    public void releaseBuffer() {
        _buffer = null;
    }

    public void init() throws IOException {
        _bufferOffset = 0;
        _pos = 0;
        _streamPos = 0;
        _streamEndWasReached = false;
        readBlock();
    }

    public void movePos() throws IOException {
        _pos++;
        if (_pos > _posLimit) {
            int pointerToPostion = _bufferOffset + _pos;
            if (pointerToPostion > _pointerToLastSafePosition) {
                moveBlock();
            }
            readBlock();
        }
    }

    public byte getIndexByte(int index) {
        return _bufferBase[_bufferOffset + _pos + index];
    }

    // index + limit have not to exceed _keepSizeAfter;
    public int getMatchLen(int index, int distance, int limit) {
        if (_streamEndWasReached) {
            if (_pos + index + limit > _streamPos) {
                limit = _streamPos - (_pos + index);
            }
        }
        distance++;
        // Byte *pby = _buffer + (size_t)_pos + index;
        int pby = _bufferOffset + _pos + index;

        int i;
        for (i = 0; i < limit && _bufferBase[pby + i] == _bufferBase[pby + i - distance]; i++) {
            ;
        }
        return i;
    }

    public int getNumAvailableBytes() {
        return _streamPos - _pos;
    }

    public void reduceOffsets(int subValue) {
        _bufferOffset += subValue;
        _posLimit -= subValue;
        _pos -= subValue;
        _streamPos -= subValue;
    }
}
