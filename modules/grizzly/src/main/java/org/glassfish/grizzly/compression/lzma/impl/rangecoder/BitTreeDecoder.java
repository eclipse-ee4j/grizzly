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

import org.glassfish.grizzly.compression.lzma.LZMADecoder;

/**
 * BitTreeDecoder
 *
 * @author Igor Pavlov
 */
public class BitTreeDecoder {

    final short[] Models;
    final int NumBitLevels;

    // decode state
    int decodeMethodState;
    int m;
    int bitIndex;

    // reverseDecode state
    int reverseDecodeMethodState;
    int symbol;

    public BitTreeDecoder(int numBitLevels) {
        NumBitLevels = numBitLevels;
        Models = new short[1 << numBitLevels];
    }

    public void init() {
        decodeMethodState = 0;
        reverseDecodeMethodState = 0;

        RangeDecoder.initBitModels(Models);
    }

    public boolean decode(LZMADecoder.LZMAInputState decodeState, RangeDecoder rangeDecoder) throws java.io.IOException {

        do {
            switch (decodeMethodState) {
            case 0: {
                m = 1;
                bitIndex = NumBitLevels;
                decodeMethodState = 1;
            }
            case 1: {
                if (bitIndex == 0) {
                    decodeMethodState = 3;
                    continue;
                }

                decodeMethodState = 2;
            }
            case 2: {
                if (!rangeDecoder.decodeBit(decodeState, Models, m)) {
                    return false;
                }

                m = (m << 1) + decodeState.lastMethodResult;
                bitIndex--;

                decodeMethodState = 1;
                continue;
            }
            case 3: {
                decodeState.lastMethodResult = m - (1 << NumBitLevels);
                decodeMethodState = 0;
                return true;
            }

            }
        } while (true);
    }

    public boolean reverseDecode(LZMADecoder.LZMAInputState decodeState, RangeDecoder rangeDecoder) throws java.io.IOException {

        do {
            switch (reverseDecodeMethodState) {
            case 0: {
                m = 1;
                symbol = 0;
                bitIndex = 0;
                reverseDecodeMethodState = 1;
            }
            case 1: {
                if (bitIndex >= NumBitLevels) {
                    reverseDecodeMethodState = 3;
                    continue;
                }

                reverseDecodeMethodState = 2;
            }
            case 2: {
                if (!rangeDecoder.decodeBit(decodeState, Models, m)) {
                    return false;
                }

                final int bit = decodeState.lastMethodResult;
                m <<= 1;
                m += bit;
                symbol |= bit << bitIndex;

                bitIndex++;
                reverseDecodeMethodState = 1;
                continue;
            }
            case 3: {
                decodeState.lastMethodResult = symbol;
                reverseDecodeMethodState = 0;
                return true;
            }

            }
        } while (true);
    }

    public static boolean reverseDecode(LZMADecoder.LZMAInputState decodeState, short[] Models, int startIndex, RangeDecoder rangeDecoder, int NumBitLevels)
            throws java.io.IOException {

        do {
            switch (decodeState.staticReverseDecodeMethodState) {
            case 0: {
                decodeState.staticM = 1;
                decodeState.staticSymbol = 0;
                decodeState.staticBitIndex = 0;
                decodeState.staticReverseDecodeMethodState = 1;
            }
            case 1: {
                if (decodeState.staticBitIndex >= NumBitLevels) {
                    decodeState.staticReverseDecodeMethodState = 3;
                    continue;
                }

                decodeState.staticReverseDecodeMethodState = 2;
            }
            case 2: {
                if (!rangeDecoder.decodeBit(decodeState, Models, startIndex + decodeState.staticM)) {
                    return false;
                }

                final int bit = decodeState.lastMethodResult;
                decodeState.staticM <<= 1;
                decodeState.staticM += bit;
                decodeState.staticSymbol |= bit << decodeState.staticBitIndex;

                decodeState.staticBitIndex++;
                decodeState.staticReverseDecodeMethodState = 1;
                continue;
            }
            case 3: {
                decodeState.lastMethodResult = decodeState.staticSymbol;
                decodeState.staticReverseDecodeMethodState = 0;
                return true;
            }

            }
        } while (true);
    }
}
