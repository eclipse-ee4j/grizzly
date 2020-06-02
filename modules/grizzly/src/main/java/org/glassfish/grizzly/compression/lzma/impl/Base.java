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

package org.glassfish.grizzly.compression.lzma.impl;

/**
 * Base
 *
 * @author Igor Pavlov
 */
public class Base {

    public static final int kNumRepDistances = 4;
    public static final int kNumStates = 12;

    public static int stateInit() {
        return 0;
    }

    public static int stateUpdateChar(int index) {
        if (index < 4) {
            return 0;
        }
        if (index < 10) {
            return index - 3;
        }
        return index - 6;
    }

    public static int stateUpdateMatch(int index) {
        return index < 7 ? 7 : 10;
    }

    public static int stateUpdateRep(int index) {
        return index < 7 ? 8 : 11;
    }

    public static int stateUpdateShortRep(int index) {
        return index < 7 ? 9 : 11;
    }

    public static boolean stateIsCharState(int index) {
        return index < 7;
    }

    public static final int kNumPosSlotBits = 6;
    public static final int kDicLogSizeMin = 0;
    // public static final int kDicLogSizeMax = 28;
    // public static final int kDistTableSizeMax = kDicLogSizeMax * 2;
    public static final int kNumLenToPosStatesBits = 2; // it's for speed optimization
    public static final int kNumLenToPosStates = 1 << kNumLenToPosStatesBits;
    public static final int kMatchMinLen = 2;

    public static int getLenToPosState(int len) {
        len -= kMatchMinLen;
        if (len < kNumLenToPosStates) {
            return len;
        }
        return kNumLenToPosStates - 1;
    }

    public static final int kNumAlignBits = 4;
    public static final int kAlignTableSize = 1 << kNumAlignBits;
    public static final int kAlignMask = kAlignTableSize - 1;
    public static final int kStartPosModelIndex = 4;
    public static final int kEndPosModelIndex = 14;
    public static final int kNumPosModels = kEndPosModelIndex - kStartPosModelIndex;
    public static final int kNumFullDistances = 1 << kEndPosModelIndex / 2;
    public static final int kNumLitPosStatesBitsEncodingMax = 4;
    public static final int kNumLitContextBitsMax = 8;
    public static final int kNumPosStatesBitsMax = 4;
    public static final int kNumPosStatesMax = 1 << kNumPosStatesBitsMax;
    public static final int kNumPosStatesBitsEncodingMax = 4;
    public static final int kNumPosStatesEncodingMax = 1 << kNumPosStatesBitsEncodingMax;
    public static final int kNumLowLenBits = 3;
    public static final int kNumMidLenBits = 3;
    public static final int kNumHighLenBits = 8;
    public static final int kNumLowLenSymbols = 1 << kNumLowLenBits;
    public static final int kNumMidLenSymbols = 1 << kNumMidLenBits;
    public static final int kNumLenSymbols = kNumLowLenSymbols + kNumMidLenSymbols + (1 << kNumHighLenBits);
    public static final int kMatchMaxLen = kMatchMinLen + kNumLenSymbols - 1;
}
