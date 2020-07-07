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
import org.glassfish.grizzly.compression.lzma.LZMADecoder;

/**
 * RangeDecoder
 *
 * @author Igor Pavlov
 */
public class RangeDecoder {

    static final int kTopMask = ~((1 << 24) - 1);
    static final int kNumBitModelTotalBits = 11;
    static final int kBitModelTotal = 1 << kNumBitModelTotalBits;
    static final int kNumMoveBits = 5;
    int Range;
    int Code;
    Buffer inputBuffer;

    int newBound;

    int decodeBitState;

    int decodeDirectBitsState;
    int decodeDirectBitsResult;
    int decodeDirectBitsI;

    public final void initFromState(final LZMADecoder.LZMAInputState decoderState) {
        this.inputBuffer = decoderState.getSrc();
    }

    public final void releaseBuffer() {
        inputBuffer = null;
    }

    public final void init() throws IOException {
        Code = 0;
        Range = -1;
        decodeBitState = 0;
        decodeDirectBitsState = 0;
        for (int i = 0; i < 5; i++) {
            Code = Code << 8 | inputBuffer.get() & 0xFF;
        }
    }

    public final boolean decodeDirectBits(LZMADecoder.LZMAInputState decodeState, int numTotalBits) throws IOException {
        do {
            switch (decodeDirectBitsState) {
            case 0: {
                decodeDirectBitsResult = 0;
                decodeDirectBitsI = numTotalBits;
                decodeDirectBitsState = 1;
            }
            case 1: {
                if (decodeDirectBitsI == 0) {
                    decodeDirectBitsState = 4;
                    continue;
                }

                Range >>>= 1;
                final int t = Code - Range >>> 31;
                Code -= Range & t - 1;
                decodeDirectBitsResult = decodeDirectBitsResult << 1 | 1 - t;
                final boolean condition = (Range & kTopMask) == 0;
                decodeDirectBitsState = condition ? 2 : 3;
                continue;
            }
            case 2: {
                if (!inputBuffer.hasRemaining()) {
                    return false;
                }
                Code = Code << 8 | inputBuffer.get() & 0xFF;
                Range <<= 8;
            }
            case 3: {
                decodeDirectBitsI--;
                decodeDirectBitsState = 1;
                continue;
            }
            case 4: {
                decodeState.lastMethodResult = decodeDirectBitsResult;
                decodeDirectBitsState = 0;
                return true;
            }
            }
        } while (true);
    }

    public boolean decodeBit(LZMADecoder.LZMAInputState decodeState, short[] probs, int index) throws IOException {

        do {
            switch (decodeBitState) {
            case 0: {
                int prob = probs[index];
                newBound = (Range >>> kNumBitModelTotalBits) * prob;
                final boolean condition = (Code ^ 0x80000000) < (newBound ^ 0x80000000);
                decodeBitState = condition ? 1 : 4;
                continue;
            }
            case 1: {
                int prob = probs[index];
                Range = newBound;
                probs[index] = (short) (prob + (kBitModelTotal - prob >>> kNumMoveBits));
                final boolean condition = (Range & kTopMask) == 0;
                decodeBitState = condition ? 2 : 3;
                continue;
            }
            case 2: {
                if (!inputBuffer.hasRemaining()) {
                    return false;
                }
                Code = Code << 8 | inputBuffer.get() & 0xFF;
                Range <<= 8;
            }
            case 3: {
                decodeState.lastMethodResult = 0;
                decodeBitState = 0;
                return true;
            }
            case 4: {
                int prob = probs[index];
                Range -= newBound;
                Code -= newBound;
                probs[index] = (short) (prob - (prob >>> kNumMoveBits));
                final boolean condition = (Range & kTopMask) == 0;
                decodeBitState = condition ? 5 : 6;
                continue;
            }
            case 5: {
                if (!inputBuffer.hasRemaining()) {
                    return false;
                }
                Code = Code << 8 | inputBuffer.get() & 0xFF;
                Range <<= 8;
            }
            case 6: {
                decodeState.lastMethodResult = 1;
                decodeBitState = 0;
                return true;
            }
            }
        } while (true);
    }

    public static void initBitModels(short[] probs) {
        for (int i = 0; i < probs.length; i++) {
            probs[i] = kBitModelTotal >>> 1;
        }
    }
}
