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

/**
 * BitTreeEncoder
 *
 * @author Igor Pavlov
 */
public class BitTreeEncoder {

    final short[] Models;
    final int NumBitLevels;

    public BitTreeEncoder(int numBitLevels) {
        NumBitLevels = numBitLevels;
        Models = new short[1 << numBitLevels];
    }

    public void init() {
        RangeDecoder.initBitModels(Models);
    }

    public void encode(RangeEncoder rangeEncoder, int symbol) throws IOException {
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0;) {
            bitIndex--;
            int bit = symbol >>> bitIndex & 1;
            rangeEncoder.encode(Models, m, bit);
            m = m << 1 | bit;
        }
    }

    public void reverseEncode(RangeEncoder rangeEncoder, int symbol) throws IOException {
        int m = 1;
        for (int i = 0; i < NumBitLevels; i++) {
            int bit = symbol & 1;
            rangeEncoder.encode(Models, m, bit);
            m = m << 1 | bit;
            symbol >>= 1;
        }
    }

    public int getPrice(int symbol) {
        int price = 0;
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0;) {
            bitIndex--;
            int bit = symbol >>> bitIndex & 1;
            price += RangeEncoder.getPrice(Models[m], bit);
            m = (m << 1) + bit;
        }
        return price;
    }

    public int reverseGetPrice(int symbol) {
        int price = 0;
        int m = 1;
        for (int i = NumBitLevels; i != 0; i--) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += RangeEncoder.getPrice(Models[m], bit);
            m = m << 1 | bit;
        }
        return price;
    }

    public static int reverseGetPrice(short[] Models, int startIndex, int NumBitLevels, int symbol) {
        int price = 0;
        int m = 1;
        for (int i = NumBitLevels; i != 0; i--) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += RangeEncoder.getPrice(Models[startIndex + m], bit);
            m = m << 1 | bit;
        }
        return price;
    }

    public static void reverseEncode(short[] Models, int startIndex, RangeEncoder rangeEncoder, int NumBitLevels, int symbol) throws IOException {
        int m = 1;
        for (int i = 0; i < NumBitLevels; i++) {
            int bit = symbol & 1;
            rangeEncoder.encode(Models, startIndex + m, bit);
            m = m << 1 | bit;
            symbol >>= 1;
        }
    }
}
