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
 * CRC
 *
 * @author Igor Pavlov
 */
public class CRC {

    static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int r = i;
            for (int j = 0; j < 8; j++) {
                if ((r & 1) != 0) {
                    r = r >>> 1 ^ 0xEDB88320;
                } else {
                    r >>>= 1;
                }
            }
            TABLE[i] = r;
        }
    }
    int _value = -1;

    public void init() {
        _value = -1;
    }

    public void update(byte[] data, int offset, int size) {
        for (int i = 0; i < size; i++) {
            _value = TABLE[(_value ^ data[offset + i]) & 0xFF] ^ _value >>> 8;
        }
    }

    public void update(byte[] data) {
        int size = data.length;
        for (int i = 0; i < size; i++) {
            _value = TABLE[(_value ^ data[i]) & 0xFF] ^ _value >>> 8;
        }
    }

    public void updateByte(int b) {
        _value = TABLE[(_value ^ b) & 0xFF] ^ _value >>> 8;
    }

    public int getDigest() {
        return _value ^ -1;
    }
}
