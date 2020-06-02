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

package org.glassfish.grizzly.compression.lzma.impl.rangecoder;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * RangeEncoder
 *
 * @author Igor Pavlov
 */
public class RangeEncoder {

    static final int kTopMask = ~((1 << 24) - 1);
    static final int kNumBitModelTotalBits = 11;
    static final int kBitModelTotal = 1 << kNumBitModelTotalBits;
    static final int kNumMoveBits = 5;
    Buffer dst;
    MemoryManager mm;
    long Low;
    int Range;
    int _cacheSize;
    int _cache;
    long _position;

    public void setBuffer(Buffer dst, MemoryManager mm) {
        this.dst = dst;
        this.mm = mm;
    }

    public Buffer releaseBuffer() {
        mm = null;
        try {
            return dst;
        } finally {
            dst = null;
        }
    }

    public void init() {
        _position = 0;
        Low = 0;
        Range = -1;
        _cacheSize = 1;
        _cache = 0;
    }

    public void flushData() throws IOException {
        for (int i = 0; i < 5; i++) {
            shiftLow();
        }
    }

    public void shiftLow() throws IOException {
        int LowHi = (int) (Low >>> 32);
        if (LowHi != 0 || Low < 0xFF000000L) {
            _position += _cacheSize;
            int temp = _cache;
            do {
                if (!dst.hasRemaining()) {
                    dst = resizeBuffer(mm, dst, 1);
                }
                dst.put((byte) (temp + LowHi));
                temp = 0xFF;
            } while (--_cacheSize != 0);
            _cache = (int) Low >>> 24;
        }
        _cacheSize++;
        Low = (Low & 0xFFFFFF) << 8;
    }

    public void encodeDirectBits(int v, int numTotalBits) throws IOException {
        for (int i = numTotalBits - 1; i >= 0; i--) {
            Range >>>= 1;
            if ((v >>> i & 1) == 1) {
                Low += Range;
            }
            if ((Range & RangeEncoder.kTopMask) == 0) {
                Range <<= 8;
                shiftLow();
            }
        }
    }

    public long getProcessedSizeAdd() {
        return _cacheSize + _position + 4;
    }

    static final int kNumMoveReducingBits = 2;
    public static final int kNumBitPriceShiftBits = 6;

    public static void initBitModels(short[] probs) {
        for (int i = 0; i < probs.length; i++) {
            probs[i] = kBitModelTotal >>> 1;
        }
    }

    public void encode(short[] probs, int index, int symbol) throws IOException {
        int prob = probs[index];
        int newBound = (Range >>> kNumBitModelTotalBits) * prob;
        if (symbol == 0) {
            Range = newBound;
            probs[index] = (short) (prob + (kBitModelTotal - prob >>> kNumMoveBits));
        } else {
            Low += newBound & 0xFFFFFFFFL;
            Range -= newBound;
            probs[index] = (short) (prob - (prob >>> kNumMoveBits));
        }
        if ((Range & kTopMask) == 0) {
            Range <<= 8;
            shiftLow();
        }
    }

    private static final int[] ProbPrices = new int[kBitModelTotal >>> kNumMoveReducingBits];

    static {
        int kNumBits = kNumBitModelTotalBits - kNumMoveReducingBits;
        for (int i = kNumBits - 1; i >= 0; i--) {
            int start = 1 << kNumBits - i - 1;
            int end = 1 << kNumBits - i;
            for (int j = start; j < end; j++) {
                ProbPrices[j] = (i << kNumBitPriceShiftBits) + (end - j << kNumBitPriceShiftBits >>> kNumBits - i - 1);
            }
        }
    }

    static public int getPrice(int Prob, int symbol) {
        return ProbPrices[((Prob - symbol ^ -symbol) & kBitModelTotal - 1) >>> kNumMoveReducingBits];
    }

    static public int getPrice0(int Prob) {
        return ProbPrices[Prob >>> kNumMoveReducingBits];
    }

    static public int getPrice1(int Prob) {
        return ProbPrices[kBitModelTotal - Prob >>> kNumMoveReducingBits];
    }

    @SuppressWarnings({ "unchecked" })
    private static Buffer resizeBuffer(final MemoryManager memoryManager, final Buffer headerBuffer, final int grow) {

        return memoryManager.reallocate(headerBuffer, Math.max(headerBuffer.capacity() + grow, headerBuffer.capacity() * 3 / 2 + 1));
    }
}
